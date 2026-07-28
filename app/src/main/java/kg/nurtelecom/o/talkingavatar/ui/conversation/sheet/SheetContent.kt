package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

// Контент единого боттомшита. Один ModalBottomSheet рендерит разные состояния; внутренний
// стек (List<SheetContent> в MainState) даёт навигацию «назад» внутри шита (напр. Web -> Answer),
// не завязываясь на app-навигацию. Webview открывается только по ссылке из HTML-ответа.
sealed interface SheetContent {
    data object Intro : SheetContent             // знакомство с Нурай
    data object LanguagePicker : SheetContent    // выбор языка
    data class Answer(val html: String) : SheetContent  // текст ответа (HTML со ссылками)
    data class Web(val url: String) : SheetContent      // деталь по ссылке, закрывается назад к Answer
}
