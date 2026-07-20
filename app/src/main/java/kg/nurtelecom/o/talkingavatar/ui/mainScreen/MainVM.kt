package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.nurtelecom.o.talkingavatar.data.api.ApiService
import kg.nurtelecom.o.talkingavatar.data.models.QuestionRequest
import kg.nurtelecom.o.talkingavatar.speech.SttEngine
import kg.nurtelecom.o.talkingavatar.speech.TtsEngine
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
    val answer: String = "",
    val isSpeaking: Boolean = false,
    val isPreparing: Boolean = false,
    val error: String? = null,
    val selectedLanguage: Language = Language.Russian,
    val showWelcome: Boolean = true,
    val isLanguageSheetOpen: Boolean = false,
)

sealed class MainSideEffect {
    data class ShowError(val message: String) : MainSideEffect()
}

// STT-движки (сейчас только google-cloud-proxy) могут вернуть код в другом регистре, без
// региона, или с гугловским квирком ("cmn" вместо "zh" для китайского — см. GoogleCloudTtsEngine
// wavenetVoiceByLanguage) — нормализуем к Language.code проекта. Языки вне списка из 6
// поддерживаемых — null, вызывающий не обновляет state.selectedLanguage этим значением.
private fun normalizeDetectedLanguage(raw: String): Language? {
    val primary = raw.substringBefore("-").lowercase().let { if (it == "cmn") "zh" else it }
    return Language.entries.firstOrNull { it.code.substringBefore("-").lowercase() == primary }
}

class MainViewModel(
    private val apiService: ApiService,
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

    fun showLanguageSheet() = intent {
        reduce { state.copy(isLanguageSheetOpen = true) }
    }

    fun hideLanguageSheet() = intent {
        reduce { state.copy(isLanguageSheetOpen = false) }
    }

    fun selectLanguage(language: Language) = intent {
        reduce { state.copy(selectedLanguage = language, showWelcome = false, isLanguageSheetOpen = false) }
        speakGreeting()
    }

    fun stopAndRestart() {
        stopSpeaking()
        startListening()
    }

    fun startListening() = intent {
        reduce { state.copy(isListening = true, showWelcome = false, error = null) }
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
            val response = apiService.askQuestion(QuestionRequest(question, state.selectedLanguage.code))
            val spokenText = response.answer
            reduce { state.copy(answer = spokenText, isPreparing = true) }
            ttsEngine.speak(
                text = spokenText,
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
