package kg.nurtelecom.o.talkingavatar.domain.model

// Чистая доменная модель (никаких Android/Compose-типов) — поддерживаемые языки NURAi.
enum class Language(val code: String, val displayName: String, val greetingText: String) {
    Russian(
        code = "ru-RU",
        displayName = "Русский",
        greetingText = "Добро пожаловать! Выберите язык для начала беседы.",
    ),
    Kyrgyz(
        code = "ky-KG",
        displayName = "Кыргызча",
        greetingText = "Кош келиңиз! Сүйлөшүүнү баштоо үчүн тилди тандаңыз.",
    ),
    English(
        code = "en-US",
        displayName = "English",
        greetingText = "Welcome! Choose a language to start the conversation.",
    ),
    Turkish(
        code = "tr-TR",
        displayName = "Türkçe",
        greetingText = "Hoş geldiniz! Sohbete başlamak için bir dil seçin.",
    ),
    Chinese(
        code = "zh-CN",
        displayName = "中文",
        greetingText = "欢迎！请选择语言开始对话。",
    ),
    German(
        code = "de-DE",
        displayName = "Deutsch",
        greetingText = "Willkommen! Wählen Sie eine Sprache, um das Gespräch zu beginnen.",
    ),
}

// STT-движки (сейчас только google-cloud-proxy) могут вернуть код в другом регистре, без
// региона, или с гугловским квирком ("cmn" вместо "zh" для китайского — см. GoogleCloudTtsEngine
// wavenetVoiceByLanguage) — нормализуем к Language.code проекта. Языки вне списка из 6
// поддерживаемых — null, вызывающий не обновляет selectedLanguage этим значением.
fun normalizeDetectedLanguage(raw: String): Language? {
    val primary = raw.substringBefore("-").lowercase().let { if (it == "cmn") "zh" else it }
    return Language.entries.firstOrNull { it.code.substringBefore("-").lowercase() == primary }
}
