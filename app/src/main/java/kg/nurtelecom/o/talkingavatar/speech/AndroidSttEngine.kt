package kg.nurtelecom.o.talkingavatar.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

private const val TAG = "AndroidSttEngine"

class AndroidSttEngine(private val context: Context) : SttEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((Throwable) -> Unit)? = null

    // Один слушатель на весь живой инстанс recognizer — колбэки конкретного вызова
    // startListening() читаются из полей выше, обновляемых при каждом запросе.
    private val recognitionListener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val all = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = all?.firstOrNull()
            Log.d(TAG, "onResults all=$all picked=$text")
            if (text != null) onResultCallback?.invoke(text) else onErrorCallback?.invoke(Exception("Речь не распознана"))
        }

        override fun onError(error: Int) {
            Log.d(TAG, "onError code=$error")
            onErrorCallback?.invoke(Exception("Ошибка распознавания речи: код $error"))
        }

        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
        }
        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
        }
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
        }
        override fun onPartialResults(partialResults: Bundle?) {
            Log.d(TAG, "onPartialResults=${partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)}")
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun startListening(language: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit) {
        mainHandler.post { startListeningOnMainThread(language, onResult, onError) }
    }

    private fun startListeningOnMainThread(language: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit) {
        onResultCallback = onResult
        onErrorCallback = onError

        // Держим один живой SpeechRecognizer на весь жизненный цикл движка вместо
        // destroy()+createSpeechRecognizer() на каждый запрос — быстрый recreate подряд
        // рвёт биндинг к сервису распознавания (ERROR_SERVER_DISCONNECTED / код 11).
        val activeRecognizer = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(recognitionListener)
            recognizer = it
        }

        // cancel() вместо destroy(): прерывает предыдущую сессию (если ещё активна),
        // но не рвёт биндинг — тот же инстанс готов к новому startListening() сразу.
        activeRecognizer.cancel()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        Log.d(TAG, "startListening language=$language")
        activeRecognizer.startListening(intent)
    }

    override fun stopListening() {
        Log.d(TAG, "stopListening (cancel)")
        mainHandler.post { recognizer?.cancel() }
    }
}
