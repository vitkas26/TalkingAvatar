package kg.nurtelecom.o.talkingavatar.data.speech

import android.util.Log
import kg.nurtelecom.o.talkingavatar.domain.gateway.SttEngine

private const val TAG = "EngineRouting"

// ky-* -> AkylAI, остальные -> Whisper. Держим ссылку на активный движок, чтобы stopListening()
// не дёргал оба сразу и не оставлял второй молча слушающим после переключения языка.
class LanguageAwareSttEngine(
    private val whisperEngine: SttEngine,
    private val akylAiEngine: SttEngine,
) : SttEngine {

    private var activeEngine: SttEngine? = null

    override fun startListening(
        language: String,
        onProcessingStarted: () -> Unit,
        onResult: (String, String?) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val engine = if (language.startsWith("ky")) akylAiEngine else whisperEngine
        Log.d(TAG, "STT AUTO: language=$language -> ${engine::class.simpleName}")
        if (activeEngine !== engine) {
            activeEngine?.stopListening()
        }
        activeEngine = engine
        engine.startListening(language, onProcessingStarted, onResult, onError)
    }

    override fun stopListening() {
        activeEngine?.stopListening()
    }
}
