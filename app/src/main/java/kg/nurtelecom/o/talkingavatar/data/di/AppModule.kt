package kg.nurtelecom.o.talkingavatar.data.di

import kg.nurtelecom.o.talkingavatar.data.api.AnthropicService
import kg.nurtelecom.o.talkingavatar.data.repository.ChatRepositoryImpl
import kg.nurtelecom.o.talkingavatar.domain.repository.ChatRepository
import kg.nurtelecom.o.talkingavatar.domain.usecase.AskQuestionUseCase
import kg.nurtelecom.o.talkingavatar.ui.mainScreen.MainViewModel
import kg.nurtelecom.o.talkingavatar.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    single<AnthropicService> { provideAnthropicService() }
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
    singleOf(::AskQuestionUseCase)
    singleOf(::MainViewModel)
}

private fun provideAnthropicService(): AnthropicService {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request: Request = chain.request().newBuilder()
                .header("x-api-key", BuildConfig.CLAUDE_API_KEY)
                .header("anthropic-version", "2023-06-01")
                .build()
            chain.proceed(request)
        }
        .build()

    return Retrofit.Builder()
        .client(client)
        .baseUrl("https://api.anthropic.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AnthropicService::class.java)
}
