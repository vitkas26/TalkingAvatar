package kg.nurtelecom.o.talkingavatar.data.api

import kg.nurtelecom.o.talkingavatar.data.models.AnswerResponse
import kg.nurtelecom.o.talkingavatar.data.models.QuestionRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/ask")
    suspend fun askQuestion(@Body request: QuestionRequest): AnswerResponse {
        val answers = mockAnswersByLanguage[request.language] ?: mockAnswersByLanguage.getValue("ru-RU")
        // Мок отдаёт HTML со ссылкой (как продовый бэк) — для проверки текст-ответа и webview.
        val paragraphs = answers.random().split("\n").joinToString("") { "<p>${it.trim()}</p>" }
        val html = "$paragraphs<p><a href=\"https://o.kg\">Подробнее на сайте</a></p>"
        return AnswerResponse(html)
    }
}
