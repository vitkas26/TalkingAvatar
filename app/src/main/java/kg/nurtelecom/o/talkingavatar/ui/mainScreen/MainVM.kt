package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kg.nurtelecom.o.talkingavatar.data.api.AnthropicMessage
import kg.nurtelecom.o.talkingavatar.data.api.AnthropicRequest
import kg.nurtelecom.o.talkingavatar.data.api.AnthropicService
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

private const val SYSTEM_PROMPT = """Ты — голосовой AI-ассистент. Отвечай коротко и по делу, 1–3 предложения максимум.
Говори на том языке, на котором задан вопрос. Не используй markdown, списки или символы — только живую речь."""

class MainViewModel(private val anthropicService: AnthropicService) : ViewModel(),
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
        val request = AnthropicRequest(
                system = SYSTEM_PROMPT,
                messages = listOf(AnthropicMessage(role = "user", content = question))
            )
            var lastError: Exception? = null
                try {
                    val response = anthropicService.sendMessage(request)
                    val answer = response.text().ifBlank { "Не могу ответить на этот вопрос." }
                    reduce { state.copy(answer = answer) }
                    postSideEffect(MainSideEffect.SpeakAnswer(answer))
                    return@intent
                } catch (e: HttpException) {
                    lastError = e
                    if (e.code() == 429) delay(3000L)
                } catch (e: Exception) {
                    lastError = e
                }

            reduce { state.copy(error = lastError.message, isPreparing = false) }
            val httpEx = lastError as? HttpException
            val msg = when {
                httpEx?.code() == 429 -> "Слишком много запросов, попробуй чуть позже"
                httpEx != null -> "HTTP ${httpEx.code()}: ${httpEx.response()?.errorBody()?.string()}"
                else -> lastError?.message ?: "Ошибка сети"
            }
        Log.d("@@@", "onSpeechResult: $msg")
            postSideEffect(MainSideEffect.ShowError(msg))
            startListening()
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
