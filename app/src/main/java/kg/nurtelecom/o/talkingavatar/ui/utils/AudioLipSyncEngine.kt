package kg.nurtelecom.o.talkingavatar.ui.utils

import kotlin.math.sqrt

class AudioLipSyncEngine {
    private var smoothed = 0f

    fun processChunk(pcm: ShortArray): Float {
        if (pcm.isEmpty()) return smoothed
        var sumSq = 0.0
        for (s in pcm) sumSq += s.toDouble() * s
        val rms = sqrt(sumSq / pcm.size).toFloat()
        val normalized = (rms / Short.MAX_VALUE.toFloat() * 3f).coerceIn(0f, 1f)
        val alpha = if (normalized > smoothed) 0.6f else 0.3f
        smoothed = alpha * normalized + (1f - alpha) * smoothed
        return smoothed
    }

    fun reset() { smoothed = 0f }
}
