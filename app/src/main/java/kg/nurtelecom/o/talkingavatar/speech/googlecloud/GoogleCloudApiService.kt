package kg.nurtelecom.o.talkingavatar.speech.googlecloud

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

object GoogleCloudConfig {
    const val BASE_URL = "https://138.16.155.105/google-cloud-proxy/"
}

data class GoogleCloudTtsRequest(
    val text: String,
    val languageCode: String,
    val voiceName: String? = null,
    val gender: String = "FEMALE",
)

data class GoogleCloudSttResponse(
    val transcript: String?,
    val confidence: Double?,
    val languageDetected: String?,
    val processingTimeMs: Long?,
)

interface GoogleCloudApiService {
    @Streaming
    @POST("tts")
    suspend fun synthesize(@Body request: GoogleCloudTtsRequest): ResponseBody

    @POST("stt")
    suspend fun transcribe(
        @Body audio: RequestBody,
        @Query("lang") lang: String,
        @Query("alt") alt: String?,
    ): GoogleCloudSttResponse
}
