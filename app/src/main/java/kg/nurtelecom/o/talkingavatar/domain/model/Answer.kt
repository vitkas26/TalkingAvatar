package kg.nurtelecom.o.talkingavatar.domain.model

// Ответ на вопрос пользователя. Бэк присылает HTML со ссылками (см. data.repository) — html
// показывается в боттомшите как есть, spokenText — та же строка без тегов, для TTS.
data class Answer(val html: String, val spokenText: String)
