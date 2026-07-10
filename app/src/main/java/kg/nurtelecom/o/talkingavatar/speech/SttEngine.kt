package kg.nurtelecom.o.talkingavatar.speech

interface SttEngine {
    fun startListening(language: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit)
    fun stopListening()
}
