package kg.nurtelecom.o.talkingavatar.speech

interface SttEngine {
    // onProcessingStarted: движок зовёт это когда запись закончилась и началась обработка
    // (сетевой запрос к Whisper/AkylAI) — микрофон уже не пишет, но результат ещё не готов.
    // Без этого колбэка UI виснет в состоянии "слушает" вплоть до онResult/onError, хотя
    // реально мик давно отпущен (см. фикс "висит в Listening на кыргызском").
    //
    // onResult: (распознанный текст, детектированный язык). Второй параметр — сырой код языка
    // от движка (например languageDetected от google-cloud-proxy), null если движок не умеет
    // детектить (Android/Whisper/AkylAI всегда шлют null). Нормализация в формат Language enum
    // и решение обновлять ли язык сессии — на стороне вызывающего (см. MainVM.onSpeechResult).
    fun startListening(
        language: String,
        onProcessingStarted: () -> Unit = {},
        onResult: (String, String?) -> Unit,
        onError: (Throwable) -> Unit,
    )
    fun stopListening()
}
