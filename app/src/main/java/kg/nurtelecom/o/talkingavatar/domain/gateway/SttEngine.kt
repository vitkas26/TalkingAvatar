package kg.nurtelecom.o.talkingavatar.domain.gateway

import kg.nurtelecom.o.talkingavatar.domain.model.Language

interface SttEngine {
    // onProcessingStarted: движок зовёт это когда запись закончилась и началась обработка
    // (сетевой запрос к Whisper/AkylAI) — микрофон уже не пишет, но результат ещё не готов.
    // Без этого колбэка UI виснет в состоянии "слушает" вплоть до онResult/onError, хотя
    // реально мик давно отпущен (см. фикс "висит в Listening на кыргызском").
    //
    // onResult: (распознанный текст, детектированный язык). Второй параметр уже нормализован
    // до доменного Language самим движком (см. GoogleCloudLanguageMapper) — null, если движок
    // не умеет детектить (Android/Whisper/AkylAI всегда шлют null) или сырой код не смэтчился
    // ни на один из поддерживаемых языков. Решение обновлять ли язык сессии — на стороне
    // вызывающего (см. MainVM.onSpeechResult), а вот разбор сырого формата провайдера — нет,
    // это забота data-слоя.
    fun startListening(
        language: String,
        onProcessingStarted: () -> Unit = {},
        onResult: (String, Language?) -> Unit,
        onError: (Throwable) -> Unit,
    )
    fun stopListening()
}
