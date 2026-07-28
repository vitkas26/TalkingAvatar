package kg.nurtelecom.o.talkingavatar.data.speech.whisper

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// Локальный Ktor-сервер Whisper (см. AkylAiApiService — тот же паттерн: свой сервис вместо
// облачного OpenAI). BASE_URL — структурная заглушка для Retrofit, реальный адрес берётся из
// EngineSettings.whisperBaseUrl через интерсептор в AudioModule.
object WhisperConfig {
    const val BASE_URL = "https://138.16.155.105/whisper/"
}

data class WhisperSttResponse(val text: String?)

interface WhisperApiService {
    // language — необязательное поле ISO-639-1 (ru/en/tr/...). Не передаём вообще (null),
    // если код языка неизвестен клиенту — сервер сам делает автодетект.
    @Multipart
    @POST("stt/transcribe")
    suspend fun transcribe(
        @Part audio: MultipartBody.Part,
        @Part("language") language: RequestBody? = null,
    ): WhisperSttResponse
}
