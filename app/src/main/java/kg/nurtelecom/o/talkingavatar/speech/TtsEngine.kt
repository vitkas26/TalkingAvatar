package kg.nurtelecom.o.talkingavatar.speech

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
