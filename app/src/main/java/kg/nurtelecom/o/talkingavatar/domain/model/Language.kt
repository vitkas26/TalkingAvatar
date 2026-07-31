package kg.nurtelecom.o.talkingavatar.domain.model

// Чистая доменная модель (никаких Android/Compose-типов) — поддерживаемые языки NURAi.
enum class Language(val code: String, val displayName: String) {
    Russian(code = "ru-RU", displayName = "Русский"),
    Kyrgyz(code = "ky-KG", displayName = "Кыргызча"),
    English(code = "en-US", displayName = "English"),
    Turkish(code = "tr-TR", displayName = "Türkçe"),
    Chinese(code = "zh-CN", displayName = "中文"),
    German(code = "de-DE", displayName = "Deutsch"),
}
