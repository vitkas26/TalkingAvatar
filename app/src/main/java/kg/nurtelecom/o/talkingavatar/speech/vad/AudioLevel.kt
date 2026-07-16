package kg.nurtelecom.o.talkingavatar.speech.vad

import kotlin.math.log10
import kotlin.math.sqrt

// Настоящий RMS по 16-bit LE PCM (не проверка одного сэмпла на амплитуду) — источник для
// Whisper/GoogleCloud, которые читают сырые PCM-байты напрямую из AudioRecord.
fun pcm16BytesDbfs(bytes: ByteArray, length: Int): Double {
    if (length < 2) return Double.NEGATIVE_INFINITY
    var sumSquares = 0.0
    var sampleCount = 0
    var i = 0
    while (i + 1 < length) {
        val sample = ((bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xff)).toShort()
        sumSquares += sample.toDouble() * sample
        sampleCount++
        i += 2
    }
    if (sampleCount == 0) return Double.NEGATIVE_INFINITY
    val rms = sqrt(sumSquares / sampleCount)
    if (rms <= 0.0) return Double.NEGATIVE_INFINITY
    return 20 * log10(rms / 32768.0)
}

// Приближение dBFS из MediaRecorder.getMaxAmplitude() (линейный пик 0..32767) — источник для
// AkylAI, чей AAC-энкодер не даёт доступа к сырым сэмплам. Грубее настоящего RMS (пик, не RMS,
// и обновляется только на poll-тик), но это всё что даёт MediaRecorder без смены энкодера.
fun mediaRecorderAmplitudeDbfs(amplitude: Int): Double {
    if (amplitude <= 0) return Double.NEGATIVE_INFINITY
    return 20 * log10(amplitude / 32767.0)
}
