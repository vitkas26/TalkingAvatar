package kg.nurtelecom.o.talkingavatar.data.di

import kg.nurtelecom.o.talkingavatar.speech.AndroidSttEngine
import kg.nurtelecom.o.talkingavatar.speech.AndroidTtsEngine
import kg.nurtelecom.o.talkingavatar.speech.DebugRoutingSttEngine
import kg.nurtelecom.o.talkingavatar.speech.DebugRoutingTtsEngine
import kg.nurtelecom.o.talkingavatar.speech.EngineSettings
import kg.nurtelecom.o.talkingavatar.speech.LanguageAwareSttEngine
import kg.nurtelecom.o.talkingavatar.speech.LanguageAwareTtsEngine
import kg.nurtelecom.o.talkingavatar.speech.SttEngine
import kg.nurtelecom.o.talkingavatar.speech.TtsEngine
import kg.nurtelecom.o.talkingavatar.speech.akylai.AkylAiApiService
import kg.nurtelecom.o.talkingavatar.speech.akylai.AkylAiConfig
import kg.nurtelecom.o.talkingavatar.speech.akylai.AkylAiSttEngine
import kg.nurtelecom.o.talkingavatar.speech.akylai.AkylAiTtsEngine
import kg.nurtelecom.o.talkingavatar.speech.googlecloud.GoogleCloudApiService
import kg.nurtelecom.o.talkingavatar.speech.googlecloud.GoogleCloudConfig
import kg.nurtelecom.o.talkingavatar.speech.googlecloud.GoogleCloudSttEngine
import kg.nurtelecom.o.talkingavatar.speech.googlecloud.GoogleCloudTtsEngine
import kg.nurtelecom.o.talkingavatar.speech.piper.PiperApiService
import kg.nurtelecom.o.talkingavatar.speech.piper.PiperConfig
import kg.nurtelecom.o.talkingavatar.speech.piper.PiperTtsEngine
import kg.nurtelecom.o.talkingavatar.speech.whisper.WhisperApiService
import kg.nurtelecom.o.talkingavatar.speech.whisper.WhisperConfig
import kg.nurtelecom.o.talkingavatar.speech.whisper.WhisperSttEngine
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Квалификаторы: несколько реализаций SttEngine/TtsEngine одновременно зарегистрированы в Koin
// (system/whisper/akylai/languageAware), get() по голому интерфейсу без квалификатора инферится
// как SttEngine и резолвится в единственный unqualified single<SttEngine> — то есть в самого себя
// (DebugRoutingSttEngine) -> StackOverflowError. Каждая реализация регистрируется под своим именем.
private val system = named("system")
private val whisper = named("whisper")
private val akylai = named("akylai")
private val piper = named("piper")
private val languageAware = named("languageAware")
private val languageAwarePiper = named("languageAwarePiper")
private val googleCloud = named("googleCloud")

// Basic Auth для тестового VPS-стенда за nginx: если basicAuthUser пуст, запрос пропускается
// без изменений (сборка без VPS/без авторизации не ломается). Один интерсептор на все три клиента,
// чтобы Authorization не дублировать в каждом OkHttpClient.Builder() отдельно.
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
private fun proxyTokenInterceptor(engineSettings: EngineSettings) = Interceptor { chain ->
    val original = chain.request()
    val token = engineSettings.googleCloudProxyToken
    val request = if (token.isNotEmpty()) {
        original.newBuilder().header("X-Proxy-Token", token).build()
    } else {
        original
    }
    chain.proceed(request)
}

val audioModule = module {
    single { EngineSettings() }

    // --- Системный Android STT/TTS (без изменений, доступны как один из вариантов выбора) ---
    single<SttEngine>(system) { AndroidSttEngine(androidContext()) }
    single<TtsEngine>(system) { AndroidTtsEngine(androidContext()) }

    // --- Whisper (локальный Ktor-сервис, см. WhisperApiService/WhisperConfig) ---
    // Тот же паттерн, что и для AkylAI ниже: baseUrl(WhisperConfig.BASE_URL) — только
    // структурная заглушка для Retrofit, реальный host/port берётся из
    // EngineSettings.whisperBaseUrl на каждый запрос через интерсептор.
    single {
        val engineSettings = get<EngineSettings>()
        Retrofit.Builder()
            .baseUrl(WhisperConfig.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val override = engineSettings.whisperBaseUrl.toHttpUrlOrNull()
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
                    .addInterceptor(basicAuthInterceptor(engineSettings))
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WhisperApiService::class.java)
    }
    single<SttEngine>(whisper) { WhisperSttEngine(androidContext(), get()) }

    // --- AkylAI (локальный HTTP-сервис вокруг AkylAI-STT/AkylAI-TTS-mini, см. AkylAiConfig) ---
    // baseUrl(AkylAiConfig.BASE_URL) ниже — только структурная заглушка для Retrofit (ему нужен
    // валидный URL при сборке). Реальный host/port/scheme берётся из EngineSettings.akylAiBaseUrl
    // на каждый запрос через интерсептор — так адрес меняется с экрана настроек без пересборки APK.
    single {
        val engineSettings = get<EngineSettings>()
        Retrofit.Builder()
            .baseUrl(AkylAiConfig.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val override = engineSettings.akylAiBaseUrl.toHttpUrlOrNull()
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
                    .addInterceptor(basicAuthInterceptor(engineSettings))
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AkylAiApiService::class.java)
    }
    single<SttEngine>(akylai) { AkylAiSttEngine(androidContext(), get()) }
    single<TtsEngine>(akylai) { AkylAiTtsEngine(androidContext(), get()) }

    // --- Piper (локальный TTS-сервис, см. PiperApiService/PiperConfig) — тот же паттерн ---
    single {
        val engineSettings = get<EngineSettings>()
        Retrofit.Builder()
            .baseUrl(PiperConfig.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val override = engineSettings.piperBaseUrl.toHttpUrlOrNull()
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
                    .addInterceptor(basicAuthInterceptor(engineSettings))
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PiperApiService::class.java)
    }
    single<TtsEngine>(piper) { PiperTtsEngine(androidContext(), get()) }

    // --- Google Cloud (через google-cloud-proxy на том же VPS, см. GoogleCloudApiService/Config) ---
    // Тот же паттерн host-override + Basic Auth, что и у остальных, плюс X-Proxy-Token поверх.
    single {
        val engineSettings = get<EngineSettings>()
        Retrofit.Builder()
            .baseUrl(GoogleCloudConfig.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val override = engineSettings.googleCloudBaseUrl.toHttpUrlOrNull()
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
                    .addInterceptor(basicAuthInterceptor(engineSettings))
                    .addInterceptor(proxyTokenInterceptor(engineSettings))
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleCloudApiService::class.java)
    }
    single<SttEngine>(googleCloud) { GoogleCloudSttEngine(androidContext(), get(), get()) }
    single<TtsEngine>(googleCloud) { GoogleCloudTtsEngine(androidContext(), get()) }

    // --- Языковой роутинг (ky-* -> AkylAI, остальное -> Whisper/системный TTS) ---
    single<SttEngine>(languageAware) {
        LanguageAwareSttEngine(whisperEngine = get(whisper), akylAiEngine = get(akylai))
    }
    single<TtsEngine>(languageAware) {
        LanguageAwareTtsEngine(fallbackEngine = get(system), akylAiTtsEngine = get(akylai))
    }
    // Комбо "Piper + AkylAI": ky-* -> AkylAI, остальное -> Piper вместо системного TTS.
    single<TtsEngine>(languageAwarePiper) {
        LanguageAwareTtsEngine(fallbackEngine = get(piper), akylAiTtsEngine = get(akylai))
    }

    // --- Финальный движок, который реально инжектится в MainViewModel: поверх языкового
    // роутинга ещё и ручной оверрайд с экрана настроек (debug для пилота) ---
    single<SttEngine> {
        DebugRoutingSttEngine(
            settings = get(),
            systemEngine = get(system),
            whisperEngine = get(whisper),
            akylAiEngine = get(akylai),
            languageAwareEngine = get(languageAware),
            googleCloudEngine = get(googleCloud),
        )
    }
    single<TtsEngine> {
        DebugRoutingTtsEngine(
            settings = get(),
            systemEngine = get(system),
            akylAiEngine = get(akylai),
            piperEngine = get(piper),
            languageAwareEngine = get(languageAware),
            piperAkylAiEngine = get(languageAwarePiper),
            googleCloudEngine = get(googleCloud),
        )
    }
}
