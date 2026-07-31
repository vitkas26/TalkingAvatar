package kg.nurtelecom.o.talkingavatar.data.repository

import androidx.core.text.HtmlCompat
import kg.nurtelecom.o.talkingavatar.data.api.ApiService
import kg.nurtelecom.o.talkingavatar.data.models.QuestionRequest
import kg.nurtelecom.o.talkingavatar.domain.model.Answer
import kg.nurtelecom.o.talkingavatar.domain.repository.QuestionRepository

// Единственное место, где DTO (AnswerResponse) превращается в доменную модель (Answer).
// Бэк шлёт HTML со ссылками; spokenText — тот же текст без тегов, для TTS (домену/презентации
// не нужно знать про формат хранения — только про html/spokenText).
class QuestionRepositoryImpl(private val apiService: ApiService) : QuestionRepository {
    override suspend fun ask(question: String, language: String): Answer {
        val response = apiService.askQuestion(QuestionRequest(question, language))
        return Answer(html = response.answer, spokenText = htmlToPlainText(response.answer))
    }
}

private fun htmlToPlainText(html: String): String =
    HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
