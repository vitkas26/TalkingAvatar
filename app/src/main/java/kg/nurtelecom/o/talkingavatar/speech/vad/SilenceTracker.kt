package kg.nurtelecom.o.talkingavatar.speech.vad

// Общая VAD-логика для Whisper/AkylAI/GoogleCloud STT (см. speech/vad/AudioLevel.kt для источника
// dBFS-сэмплов) — раньше каждый движок дублировал свой fixed-duration record loop, теперь решение
// "продолжать/остановить" живёт в одном месте. Чистая логика без зависимостей от Android API,
// движки кормят её своими dBFS-сэмплами (raw PCM RMS или MediaRecorder.getMaxAmplitude()).
data class VadConfig(
    val silenceThresholdDb: Double,
    val silenceDurationMs: Long,
    val maxRecordingMs: Long,
)

sealed class VadDecision {
    data object Continue : VadDecision()
    data object StopSilence : VadDecision()
    data object StopMaxDuration : VadDecision()
}

class SilenceTracker(private val config: VadConfig) {

    // Считать тишину поводом для остановки начинаем только после того как речь реально
    // прозвучала хоть раз — иначе пользователь, помедливший перед началом фразы, обрежется
    // мгновенно на первом же vadSilenceDurationMs тишины с t=0.
    var hasDetectedSpeech: Boolean = false
        private set

    private var silenceStartMs: Long? = null

    fun onSample(levelDb: Double, elapsedMs: Long): VadDecision {
        if (elapsedMs >= config.maxRecordingMs) return VadDecision.StopMaxDuration

        val isSpeech = levelDb > config.silenceThresholdDb
        if (isSpeech) {
            hasDetectedSpeech = true
            silenceStartMs = null
        } else if (hasDetectedSpeech) {
            val start = silenceStartMs ?: elapsedMs.also { silenceStartMs = it }
            if (elapsedMs - start >= config.silenceDurationMs) return VadDecision.StopSilence
        }
        return VadDecision.Continue
    }
}
