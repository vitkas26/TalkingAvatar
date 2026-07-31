package kg.nurtelecom.o.talkingavatar.data.speech.piper

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

// Локальный Piper TTS-сервер — синтез речи на сервере, ответ raw WAV (см. AkylAiApiService,
// тот же паттерн: baseUrl ниже только структурная заглушка, реальный адрес — из
// EngineSettings.piperBaseUrl через интерсептор в AudioModule).
object PiperConfig {
    const val BASE_URL = "https://138.16.155.105/piper/"
}

// language — необязателен (ru/en/de/zh/tr), сервер по умолчанию "ru". ky не поддерживается —
// для kyrgyz используется AkylAI-TTS-mini, не Piper. speed не передаём — не запрошено в UI,
// сервер сам подставит дефолт 1.0 (Gson по умолчанию не сериализует null-поля).
data class PiperTtsRequest(val text: String, val language: String? = null)
data class PiperErrorResponse(val error: String?)

interface PiperApiService {
    @Streaming
    @POST("tts/synthesize")
    suspend fun synthesize(@Body request: PiperTtsRequest): ResponseBody
}
