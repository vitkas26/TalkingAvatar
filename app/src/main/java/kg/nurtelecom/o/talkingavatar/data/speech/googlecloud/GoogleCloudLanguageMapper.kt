package kg.nurtelecom.o.talkingavatar.data.speech.googlecloud

import kg.nurtelecom.o.talkingavatar.domain.model.Language

// DTO(сырой languageDetected от google-cloud-proxy) -> domain(Language) — по CLAUDE.md такой
// маппинг живёт в data, не в domain. Единственный провайдер, который вообще шлёт детект языка
// (Android/Whisper/AkylAI всегда шлют null), и единственный источник гугловской квирки —
// "cmn" вместо "zh" для китайского (см. GoogleCloudTtsEngine.wavenetVoiceByLanguage, та же
// квирка в обратную сторону). Языки вне списка из 6 поддерживаемых — null, вызывающий не
// обновляет selectedLanguage этим значением.
fun normalizeDetectedLanguage(raw: String): Language? {
    val primary = raw.substringBefore("-").lowercase().let { if (it == "cmn") "zh" else it }
    return Language.entries.firstOrNull { it.code.substringBefore("-").lowercase() == primary }
}
