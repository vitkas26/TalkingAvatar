package kg.nurtelecom.o.talkingavatar.speech

import android.util.Log

private const val TAG = "EngineRouting"

// Финальный TtsEngine, который реально биндится в Koin — см. DebugRoutingSttEngine.
class DebugRoutingTtsEngine(
    private val settings: EngineSettings,
    private val systemEngine: TtsEngine,
    private val akylAiEngine: TtsEngine,
    private val piperEngine: TtsEngine,
    private val languageAwareEngine: TtsEngine,
    private val piperAkylAiEngine: TtsEngine,
    private val googleCloudEngine: TtsEngine,
    private val googleCloudPlusAkylaiEngine: TtsEngine,
) : TtsEngine {

    private var activeEngine: TtsEngine? = null

    override suspend fun speak(
        text: String,
        language: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val engine = when (settings.ttsChoice) {
            TtsEngineChoice.SYSTEM -> systemEngine
            TtsEngineChoice.AKYLAI -> akylAiEngine
            TtsEngineChoice.PIPER -> piperEngine
            TtsEngineChoice.PIPER_AKYLAI -> piperAkylAiEngine
            TtsEngineChoice.GOOGLE_CLOUD -> googleCloudEngine
            TtsEngineChoice.GOOGLE_CLOUD_PLUS_AKYLAI -> googleCloudPlusAkylaiEngine
            TtsEngineChoice.AUTO -> languageAwareEngine
        }
        Log.d(TAG, "TTS: choice=${settings.ttsChoice} language=$language -> ${engine::class.simpleName}")
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
