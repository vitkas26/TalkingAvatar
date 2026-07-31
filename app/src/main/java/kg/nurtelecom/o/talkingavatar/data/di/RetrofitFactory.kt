package kg.nurtelecom.o.talkingavatar.data.di

import kg.nurtelecom.o.talkingavatar.data.speech.EngineSettings
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Basic Auth для тестового VPS-стенда за nginx: если basicAuthUser пуст, запрос пропускается
// без изменений (сборка без VPS/без авторизации не ломается). Общий интерсептор для всех
// Retrofit-клиентов STT/TTS движков, чтобы Authorization не дублировать в каждом builder'е.
private fun basicAuthInterceptor(engineSettings: EngineSettings) = Interceptor { chain ->
    val original = chain.request()
    val user = engineSettings.basicAuthUser
    val request = if (user.isNotEmpty()) {
        original.newBuilder()
            .header("Authorization", Credentials.basic(user, engineSettings.basicAuthPassword))
            .build()
    } else {
        original
    }
    chain.proceed(request)
}

// Доп. авторизация для google-cloud-proxy поверх Basic Auth. Пусто — заголовок не шлём
// (сборка без токена не ломается, как и с Basic Auth выше).
fun proxyTokenInterceptor(engineSettings: EngineSettings) = Interceptor { chain ->
    val original = chain.request()
    val token = engineSettings.googleCloudProxyToken
    val request = if (token.isNotEmpty()) {
        original.newBuilder().header("X-Proxy-Token", token).build()
    } else {
        original
    }
    chain.proceed(request)
}

// Реальный host/port/scheme берётся из EngineSettings (currentBaseUrl) на каждый запрос —
// baseUrl() в Retrofit.Builder ниже только структурная заглушка (Retrofit требует валидный
// URL при сборке). Так адрес STT/TTS-сервиса меняется с экрана настроек без пересборки APK.
private fun hostOverrideInterceptor(currentBaseUrl: () -> String) = Interceptor { chain ->
    val original = chain.request()
    val override = currentBaseUrl().toHttpUrlOrNull()
    val request = if (override != null) {
        original.newBuilder()
            .url(
                original.url.newBuilder()
                    .scheme(override.scheme)
                    .host(override.host)
                    .port(override.port)
                    .build(),
            )
            .build()
    } else {
        original
    }
    chain.proceed(request)
}

// Единая фабрика Retrofit-клиента для STT/TTS движков (Whisper/AkylAI/Piper/GoogleCloud):
// host-override + Basic Auth + опциональные доп.интерсепторы (напр. proxyTokenInterceptor
// для google-cloud-proxy). Раньше этот блок был скопирован 4 раза в AudioModule.
fun <T> retrofitService(
    placeholderBaseUrl: String,
    apiClass: Class<T>,
    engineSettings: EngineSettings,
    extraInterceptors: List<Interceptor> = emptyList(),
    currentBaseUrl: () -> String,
): T {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(hostOverrideInterceptor(currentBaseUrl))
        .addInterceptor(basicAuthInterceptor(engineSettings))
        .apply { extraInterceptors.forEach { addInterceptor(it) } }
        .build()

    return Retrofit.Builder()
        .baseUrl(placeholderBaseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(apiClass)
}
