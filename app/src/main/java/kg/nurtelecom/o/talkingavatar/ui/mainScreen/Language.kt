package kg.nurtelecom.o.talkingavatar.ui.mainScreen

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
}
