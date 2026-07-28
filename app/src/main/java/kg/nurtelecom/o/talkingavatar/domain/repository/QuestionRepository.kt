package kg.nurtelecom.o.talkingavatar.domain.repository

import kg.nurtelecom.o.talkingavatar.domain.model.Answer

// Domain объявляет контракт, data (QuestionRepositoryImpl) — реализует и знает про сеть/DTO.
interface QuestionRepository {
    suspend fun ask(question: String, language: String): Answer
}
