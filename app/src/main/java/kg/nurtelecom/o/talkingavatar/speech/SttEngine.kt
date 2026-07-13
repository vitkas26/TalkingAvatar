package kg.nurtelecom.o.talkingavatar.speech

interface SttEngine {
    // onProcessingStarted: движок зовёт это когда запись закончилась и началась обработка
    // (сетевой запрос к Whisper/AkylAI) — микрофон уже не пишет, но результат ещё не готов.
    // Без этого колбэка UI виснет в состоянии "слушает" вплоть до онResult/onError, хотя
    // реально мик давно отпущен (см. фикс "висит в Listening на кыргызском").
    fun startListening(
        language: String,
        onProcessingStarted: () -> Unit = {},
        onResult: (String) -> Unit,
        onError: (Throwable) -> Unit,
    )
    fun stopListening()
}
