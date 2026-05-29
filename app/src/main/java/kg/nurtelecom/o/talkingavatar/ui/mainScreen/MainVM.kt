package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.nurtelecom.o.talkingavatar.domain.usecase.AskQuestionUseCase
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import retrofit2.HttpException

data class MainState(
    val isListening: Boolean = false,
    val question: String = "",
    val answer: String = "",
    val isSpeaking: Boolean = false,
    val isPreparing: Boolean = false,
    val error: String? = null,
)

sealed class MainSideEffect {
    data class StartSpeechRecognition(val language: String = "ru-RU") : MainSideEffect()
    data class SpeakAnswer(val text: String) : MainSideEffect()
    data object StopSpeaking : MainSideEffect()
    data object TriggerEarListen : MainSideEffect()
    data class ShowError(val message: String) : MainSideEffect()
}

class MainViewModel(private val askQuestion: AskQuestionUseCase) : ViewModel(),
    ContainerHost<MainState, MainSideEffect> {

    override val container: Container<MainState, MainSideEffect> = viewModelScope.container(MainState())

    fun stopAndRestart() {
        onSpeakingFinished()
    }

    fun startListening() = intent {
        if (state.isSpeaking || state.isPreparing) return@intent
        reduce { state.copy(isListening = true) }
        postSideEffect(MainSideEffect.StartSpeechRecognition())
    }

    fun onSpeechResult(question: String) = intent {
        reduce { state.copy(isListening = false, question = question, isPreparing = true) }

        if (EAR_LISTEN_KEYWORDS.any { question.lowercase().contains(it) }) {
            postSideEffect(MainSideEffect.TriggerEarListen)
        }

        try {
            val answer = askQuestion(question)
            reduce { state.copy(answer = answer) }
            postSideEffect(MainSideEffect.SpeakAnswer(answer))
        } catch (e: HttpException) {
            val msg = if (e.code() == 429) {
                delay(3000L)
                "Слишком много запросов, попробуй чуть позже"
            } else {
                "HTTP ${e.code()}: ${e.response()?.errorBody()?.string()}"
            }
            reduce { state.copy(error = msg, isPreparing = false) }
            postSideEffect(MainSideEffect.ShowError(msg))
            startListening()
        } catch (e: Exception) {
            val msg = e.message ?: "Ошибка сети"
            reduce { state.copy(error = msg, isPreparing = false) }
            postSideEffect(MainSideEffect.ShowError(msg))
            startListening()
        }
    }

    fun onSpeakingStarted() = intent {
        reduce { state.copy(isSpeaking = true, isPreparing = false) }
    }

    fun onSpeakingFinished() = intent {
        reduce { state.copy(isSpeaking = false, isPreparing = false) }
        postSideEffect(MainSideEffect.StopSpeaking)
        startListening()
    }

    companion object {
        private val EAR_LISTEN_KEYWORDS = listOf(
            "подними руку", "приложи руку", "руку к уху",
            "подслушай", "подслушивай", "послушай внимательно"
        )
    }
}
