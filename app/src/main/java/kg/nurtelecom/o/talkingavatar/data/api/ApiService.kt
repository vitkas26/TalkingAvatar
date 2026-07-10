package kg.nurtelecom.o.talkingavatar.data.api

import kg.nurtelecom.o.talkingavatar.data.models.AnswerResponse
import kg.nurtelecom.o.talkingavatar.data.models.QuestionRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/ask")
    suspend fun askQuestion(@Body request: QuestionRequest): AnswerResponse {
        val answers = mockAnswersByLanguage[request.language] ?: mockAnswersByLanguage.getValue("ru-RU")
        return AnswerResponse(answers.random())
    }
}
