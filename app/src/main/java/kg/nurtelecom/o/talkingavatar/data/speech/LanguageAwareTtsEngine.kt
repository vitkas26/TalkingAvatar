package kg.nurtelecom.o.talkingavatar.data.speech

import android.util.Log
import kg.nurtelecom.o.talkingavatar.domain.gateway.TtsEngine

private const val TAG = "EngineRouting"

// ky-* -> AkylAI-TTS-mini, остальные -> fallbackEngine. Один и тот же класс используется под
// двумя парами в Koin (см. AudioModule): fallback=системный (обычный AUTO) и fallback=Piper
// (комбо "Piper + AkylAI" для тех, кто не хочет системный Android TTS для не-ky языков).
class LanguageAwareTtsEngine(
    private val fallbackEngine: TtsEngine,
    private val akylAiTtsEngine: TtsEngine,
) : TtsEngine {

    private var activeEngine: TtsEngine? = null

    override suspend fun speak(
        text: String,
        language: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val engine = if (language.startsWith("ky")) akylAiTtsEngine else fallbackEngine
        Log.d(TAG, "TTS AUTO: language=$language -> ${engine::class.simpleName}")
        if (activeEngine !== engine) {
            activeEngine?.stop()
        }
        activeEngine = engine
        engine.speak(text, language, onStart, onDone, onError)
    }

    override fun stop() {
        activeEngine?.stop()
    }
}
