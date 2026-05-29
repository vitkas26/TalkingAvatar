package kg.nurtelecom.o.talkingavatar.domain.repository

interface ChatRepository {
    suspend fun ask(question: String): String
}
