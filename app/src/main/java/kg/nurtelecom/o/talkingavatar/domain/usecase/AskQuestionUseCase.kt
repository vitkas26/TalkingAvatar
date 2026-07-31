package kg.nurtelecom.o.talkingavatar.domain.usecase

import kg.nurtelecom.o.talkingavatar.domain.model.Answer
import kg.nurtelecom.o.talkingavatar.domain.repository.QuestionRepository

// Presentation (MainViewModel) зависит от этого use case, не от ApiService/DTO напрямую.
class AskQuestionUseCase(private val repository: QuestionRepository) {
    suspend operator fun invoke(question: String, language: String): Answer =
        repository.ask(question, language)
}
