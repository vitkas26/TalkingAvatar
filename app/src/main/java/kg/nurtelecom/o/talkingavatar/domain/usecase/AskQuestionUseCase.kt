package kg.nurtelecom.o.talkingavatar.domain.usecase

import kg.nurtelecom.o.talkingavatar.domain.repository.ChatRepository

class AskQuestionUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(question: String): String = repository.ask(question)
}
