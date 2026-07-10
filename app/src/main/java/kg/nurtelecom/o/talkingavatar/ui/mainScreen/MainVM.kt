package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.nurtelecom.o.talkingavatar.data.api.ApiService
import kg.nurtelecom.o.talkingavatar.data.models.QuestionRequest
import kg.nurtelecom.o.talkingavatar.speech.SttEngine
import kg.nurtelecom.o.talkingavatar.speech.TtsEngine
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
    val hasSelectedLanguage: Boolean = false,
    val isLanguageSheetOpen: Boolean = false,
)

sealed class MainSideEffect {
    data class ShowError(val message: String) : MainSideEffect()
    data object RequestListening : MainSideEffect()
}

class MainViewModel(
    private val apiService: ApiService,
    private val sttEngine: SttEngine,
    private val ttsEngine: TtsEngine,
) : ViewModel(), ContainerHost<MainState, MainSideEffect> {

    override val container: Container<MainState, MainSideEffect> = viewModelScope.container(MainState())

    // Приветствие звучит только после явного выбора языка (см. selectLanguage), не на старте —
    // до выбора языка аватар просто "зовёт" через Welcome-состояние, без озвучки. И после
    // приветствия, и после каждого ответа сразу просим слушать дальше — permission на микрофон
    // дёргает MainScreen по сайд-эффекту (см. onSpeechFinished).
    private fun speakGreeting() = intent {
        ttsEngine.speak(
            text = state.selectedLanguage.greetingText,
            language = state.selectedLanguage.code,
            onStart = { onSpeakingStarted() },
            onDone = { onSpeechFinished() },
            onError = { error -> onTtsError(error) },
        )
    }

    private fun onSpeechFinished() = intent {
        reduce { state.copy(isSpeaking = false, isPreparing = false) }
        postSideEffect(MainSideEffect.RequestListening)
    }

    fun showLanguageSheet() = intent {
        reduce { state.copy(isLanguageSheetOpen = true) }
    }

    fun hideLanguageSheet() = intent {
        reduce { state.copy(isLanguageSheetOpen = false) }
    }

    fun selectLanguage(language: Language) = intent {
        reduce { state.copy(selectedLanguage = language, hasSelectedLanguage = true, isLanguageSheetOpen = false) }
        speakGreeting()
    }

    fun stopAndRestart() {
        stopSpeaking()
        startListening()
    }

    fun startListening() = intent {
        reduce { state.copy(isListening = true, error = null) }
        sttEngine.startListening(
            language = state.selectedLanguage.code,
            onResult = { text -> onSpeechResult(text) },
            onError = { error -> onSttError(error) },
        )
    }

    fun onSpeechResult(question: String) = intent {
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

    private fun onSttError(error: Throwable) = intent {
        reduce { state.copy(isListening = false, error = error.message) }
        postSideEffect(MainSideEffect.ShowError(error.message ?: "Ошибка распознавания речи"))
    }

    private fun onTtsError(error: Throwable) = intent {
        reduce { state.copy(isSpeaking = false, isPreparing = false, error = error.message) }
        postSideEffect(MainSideEffect.ShowError("Ошибка TTS: ${error.message}"))
    }
}
