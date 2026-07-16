package kg.nurtelecom.o.talkingavatar.speech

import android.util.Log

private const val TAG = "EngineRouting"

// Финальный SttEngine, который реально биндится в Koin. Поверх языкового роутинга (см.
// LanguageAwareSttEngine) добавляет ручной оверрайд с экрана настроек — для отладки на пилоте,
// чтобы форсировать конкретный движок независимо от языка. AUTO = обычный языковой роутинг.
class DebugRoutingSttEngine(
    private val settings: EngineSettings,
    private val systemEngine: SttEngine,
    private val whisperEngine: SttEngine,
    private val akylAiEngine: SttEngine,
    private val languageAwareEngine: SttEngine,
    private val googleCloudEngine: SttEngine,
    private val googleCloudPlusAkylaiEngine: SttEngine,
) : SttEngine {

    private var activeEngine: SttEngine? = null

    override fun startListening(
        language: String,
        onProcessingStarted: () -> Unit,
        onResult: (String, String?) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val engine = when (settings.sttChoice) {
            SttEngineChoice.SYSTEM -> systemEngine
            SttEngineChoice.WHISPER -> whisperEngine
            SttEngineChoice.AKYLAI -> akylAiEngine
            SttEngineChoice.GOOGLE_CLOUD -> googleCloudEngine
            SttEngineChoice.GOOGLE_CLOUD_PLUS_AKYLAI -> googleCloudPlusAkylaiEngine
            SttEngineChoice.AUTO -> languageAwareEngine
        }
        Log.d(TAG, "STT: choice=${settings.sttChoice} language=$language -> ${engine::class.simpleName}")
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
