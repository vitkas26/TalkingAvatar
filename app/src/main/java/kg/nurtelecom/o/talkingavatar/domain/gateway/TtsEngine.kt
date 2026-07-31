package kg.nurtelecom.o.talkingavatar.domain.gateway

interface TtsEngine {
    suspend fun speak(
        text: String,
        language: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    )

    fun stop()
}
