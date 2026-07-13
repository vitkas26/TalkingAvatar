package kg.nurtelecom.o.talkingavatar.speech.akylai

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

// AkylAI-STT / AkylAI-TTS-mini — PyTorch-модели с Hugging Face, на девайсе напрямую не крутятся.
// Обёрнуты локальным HTTP-сервисом (FastAPI/Flask, поднимается отдельно, см. README пилота).
// BASE_URL — заглушка на локальный сервис: с эмулятора "10.0.2.2" указывает на localhost хоста,
// с реального устройства нужен реальный IP ноутбука в той же сети.
object AkylAiConfig {
    const val BASE_URL = "http://10.191.239.144:8000/"
}

data class AkylAiSttResponse(val text: String?)
data class AkylAiTtsRequest(val text: String, val language: String)

interface AkylAiApiService {
    @Multipart
    @POST("stt/transcribe")
    suspend fun transcribe(@Part audio: MultipartBody.Part): AkylAiSttResponse

    @Streaming
    @POST("tts/synthesize")
    suspend fun synthesize(@Body request: AkylAiTtsRequest): ResponseBody
}
