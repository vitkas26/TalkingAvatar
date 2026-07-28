package kg.nurtelecom.o.talkingavatar.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.nurtelecom.o.talkingavatar.domain.model.Answer
import kg.nurtelecom.o.talkingavatar.domain.model.Language
import kg.nurtelecom.o.talkingavatar.domain.model.normalizeDetectedLanguage
import kg.nurtelecom.o.talkingavatar.domain.usecase.AskQuestionUseCase
import kg.nurtelecom.o.talkingavatar.domain.gateway.SttEngine
import kg.nurtelecom.o.talkingavatar.domain.gateway.TtsEngine
import kg.nurtelecom.o.talkingavatar.ui.conversation.sheet.SheetContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container

data class MainState(
    val isListening: Boolean = false,
    val question: String = "",
    val answer: Answer? = null,
    val isSpeaking: Boolean = false,
    val isPreparing: Boolean = false,
    val error: String? = null,
    val selectedLanguage: Language = Language.Russian,
    val showWelcome: Boolean = true,
    // Стек контента единого боттомшита (пусто = закрыт). Верхний = текущий; back = pop.
    val sheet: List<SheetContent> = emptyList(),
) {
    val sheetTop: SheetContent? get() = sheet.lastOrNull()
}

sealed class MainSideEffect {
    data class ShowError(val message: String) : MainSideEffect()
}

class MainViewModel(
    private val askQuestion: AskQuestionUseCase,
    private val sttEngine: SttEngine,
    private val ttsEngine: TtsEngine,
) : ViewModel(), ContainerHost<MainState, MainSideEffect> {

    override val container: Container<MainState, MainSideEffect> = viewModelScope.container(MainState())

    private var idleTimeoutJob: Job? = null

    // Welcome — не разовый экран, а idle-режим: если 10 минут никто не взаимодействует
    // (не жмёт "Задать вопрос", не выбирает язык), аватар сам возвращается в Welcome.
    // Таймер перезапускается на каждое реальное действие пользователя (см. startListening/selectLanguage).
    private fun resetIdleTimer() {
        idleTimeoutJob?.cancel()
        idleTimeoutJob = viewModelScope.launch {
            delay(10.seconds) // TODO тест: вернуть 10.minutes перед пилотом
            intent { reduce { state.copy(showWelcome = true) } }
        }
    }

    // Выставляется из SettingsScreen при переходе на аватар — только язык, без озвучки
    // и без выхода из Welcome. Приветствие звучит только через selectLanguage (боттомшит).
    fun setInitialLanguage(language: Language) = intent {
        reduce { state.copy(selectedLanguage = language) }
    }

    private fun speakGreeting() = intent {
        ttsEngine.speak(
            text = state.selectedLanguage.greetingText,
            language = state.selectedLanguage.code,
            onStart = { onSpeakingStarted() },
            onDone = { onSpeechFinished() },
            onError = { error -> onTtsError(error) },
        )
    }

    // Никакого авто-переслушивания — дальше слушаем только по явному тапу "Задать вопрос".
    // resetIdleTimer() здесь, а не в startListening/selectLanguage — 10 минут это тишина
    // ПОСЛЕ того как аватар домолвил, а не общая длительность цикла Listening->Processing->Speaking.
    private fun onSpeechFinished() = intent {
        reduce { state.copy(isSpeaking = false, isPreparing = false) }
        resetIdleTimer()
    }

    // --- Единый боттомшит: стек контента (см. SheetContent) ---
    fun showLanguageSheet() = intent { reduce { state.copy(sheet = listOf(SheetContent.LanguagePicker)) } }
    fun showIntroSheet() = intent { reduce { state.copy(sheet = listOf(SheetContent.Intro)) } }

    // «Ответ в текстовом виде» на экране Speaking — открыть HTML-ответ в шите.
    fun showTextAnswer() = intent {
        val html = state.answer?.html ?: return@intent
        reduce { state.copy(sheet = listOf(SheetContent.Answer(html))) }
    }

    // Тап по ссылке внутри HTML-ответа — открыть webview поверх (push в стек).
    fun openWebInSheet(url: String) = intent {
        reduce { state.copy(sheet = state.sheet + SheetContent.Web(url)) }
    }

    // Назад внутри шита (напр. Web -> Answer). Пустой стек = закрыт.
    fun sheetBack() = intent { reduce { state.copy(sheet = state.sheet.dropLast(1)) } }
    fun closeSheet() = intent { reduce { state.copy(sheet = emptyList()) } }

    fun hideLanguageSheet() = closeSheet()

    fun selectLanguage(language: Language) = intent {
        reduce { state.copy(selectedLanguage = language, showWelcome = false, sheet = emptyList()) }
        speakGreeting()
    }

    fun stopAndRestart() {
        stopSpeaking()
        startListening()
    }

    fun startListening() = intent {
        reduce { state.copy(isListening = true, showWelcome = false, error = null, question = "") }
        sttEngine.startListening(
            language = state.selectedLanguage.code,
            onProcessingStarted = { onSttProcessingStarted() },
            onResult = { text, detectedLanguage -> onSpeechResult(text, detectedLanguage) },
            onError = { error -> onSttError(error) },
        )
    }

    // Запись закончилась (микрофон уже отпущен), идёт распознавание на сервере — показываем
    // Processing вместо того чтобы висеть в Listening до onResult/onError (актуально для
    // Whisper/AkylAI, где это отдельный сетевой запрос после записи).
    private fun onSttProcessingStarted() = intent {
        reduce { state.copy(isListening = false, isPreparing = true) }
    }

    fun onSpeechResult(question: String, detectedLanguageCode: String? = null) = intent {
        // Автообновление языка сессии по STT-детекту (сейчас только google-cloud-proxy шлёт
        // languageDetected — Android/Whisper/AkylAI всегда null). Ручной выбор (selectLanguage/
        // setInitialLanguage) остаётся единственным другим писателем selectedLanguage и всегда
        // выигрывает на практике: этот блок срабатывает только внутри реального STT-запроса.
        val detectedLanguage = detectedLanguageCode
            ?.takeIf { it.isNotBlank() }
            ?.let { normalizeDetectedLanguage(it) }
        if (detectedLanguage != null && detectedLanguage != state.selectedLanguage) {
            reduce { state.copy(selectedLanguage = detectedLanguage) }
        }
        reduce { state.copy(isListening = false, question = question) }
        try {
            val answer = askQuestion(question, state.selectedLanguage.code)
            reduce { state.copy(answer = answer, isPreparing = true) }
            ttsEngine.speak(
                text = answer.spokenText,
                language = state.selectedLanguage.code,
                onStart = { onSpeakingStarted() },
                onDone = { onSpeechFinished() },
                onError = { error -> onTtsError(error) },
            )
        } catch (e: Exception) {
            reduce { state.copy(error = e.message) }
            postSideEffect(MainSideEffect.ShowError(e.message ?: "Ошибка сети"))
        }
    }

    fun onSpeakingStarted() = intent {
        reduce { state.copy(isSpeaking = true, isPreparing = false) }
    }

    fun stopSpeaking() = intent {
        ttsEngine.stop()
        reduce { state.copy(isSpeaking = false, isPreparing = false) }
    }

    fun cancelListening() = intent {
        sttEngine.stopListening()
        reduce { state.copy(isListening = false) }
        resetIdleTimer()
    }

    private fun onSttError(error: Throwable) = intent {
        reduce { state.copy(isListening = false, isPreparing = false, error = error.message) }
        resetIdleTimer()
        postSideEffect(MainSideEffect.ShowError(error.message ?: "Ошибка распознавания речи"))
    }

    private fun onTtsError(error: Throwable) = intent {
        reduce { state.copy(isSpeaking = false, isPreparing = false, error = error.message) }
        resetIdleTimer()
        postSideEffect(MainSideEffect.ShowError("Ошибка TTS: ${error.message}"))
    }
}
