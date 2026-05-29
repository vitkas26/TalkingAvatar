package kg.nurtelecom.o.talkingavatar.data.repository

import kg.nurtelecom.o.talkingavatar.data.api.AnthropicMessage
import kg.nurtelecom.o.talkingavatar.data.api.AnthropicRequest
import kg.nurtelecom.o.talkingavatar.data.api.AnthropicService
import kg.nurtelecom.o.talkingavatar.domain.repository.ChatRepository

private const val SYSTEM_PROMPT = """Ты — голосовой AI-ассистент. Отвечай коротко и по делу, 1–3 предложения максимум.
Говори на том языке, на котором задан вопрос. Не используй markdown, списки или символы — только живую речь."""

class ChatRepositoryImpl(private val service: AnthropicService) : ChatRepository {
    override suspend fun ask(question: String): String {
        val request = AnthropicRequest(
            system = SYSTEM_PROMPT,
            messages = listOf(AnthropicMessage(role = "user", content = question))
        )
        return service.sendMessage(request).text().ifBlank { "Не могу ответить на этот вопрос." }
    }
}
