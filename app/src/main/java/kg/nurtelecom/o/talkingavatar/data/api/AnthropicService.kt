package kg.nurtelecom.o.talkingavatar.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface AnthropicService {
    @POST("v1/messages")
    suspend fun sendMessage(@Body request: AnthropicRequest): AnthropicResponse
}

data class AnthropicRequest(
    val model: String = "claude-sonnet-4-6",
    val max_tokens: Int = 300,
    val system: String? = null,
    val messages: List<AnthropicMessage>
)

data class AnthropicMessage(val role: String, val content: String)

data class AnthropicResponse(val content: List<AnthropicContent>) {
    fun text() = content.firstOrNull { it.type == "text" }?.text.orEmpty()
}

data class AnthropicContent(val type: String, val text: String)
