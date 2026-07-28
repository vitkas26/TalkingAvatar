package kg.nurtelecom.o.talkingavatar.data.di

import kg.nurtelecom.o.talkingavatar.data.api.ApiService
import kg.nurtelecom.o.talkingavatar.data.repository.QuestionRepositoryImpl
import kg.nurtelecom.o.talkingavatar.domain.repository.QuestionRepository
import kg.nurtelecom.o.talkingavatar.domain.usecase.AskQuestionUseCase
import kg.nurtelecom.o.talkingavatar.ui.conversation.MainViewModel
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


val mainModule = module {
    singleOf(::MainViewModel)
    single { provideApiService() }
    single<QuestionRepository> { QuestionRepositoryImpl(get()) }
    singleOf(::AskQuestionUseCase)
}

private fun provideApiService(): ApiService {

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.MINUTES)
        .callTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)

    return Retrofit.Builder()
        .client(okHttpClient.build())
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("https://your-backend-url.com/").build()
        .create(ApiService::class.java)
}