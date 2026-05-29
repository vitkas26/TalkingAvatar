package kg.nurtelecom.o.talkingavatar.ui.utils

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale

class AudioPlayer(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timelineJob: Job? = null
    private val lipSyncEngine = AudioLipSyncEngine()

    fun initialize() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru", "RU")
                applyFemaleVoice()
            }
        }
    }

    private fun applyFemaleVoice() {
        val ruVoices = tts?.voices?.filter { it.locale.language == "ru" } ?: return
        val voice = ruVoices.find { it.name == "ru-ru-x-ruf-network" }
            ?: ruVoices.find { it.name == "ru-ru-x-ruf-local" }
            ?: ruVoices.find { it.name.contains("ruf") }
        if (voice != null) tts?.voice = voice
    }

    fun play(
        text: String,
        onStart: () -> Unit = {},
        onFinish: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
        onAmplitude: ((Float) -> Unit)? = null
    ) {
        val audioFile = File(context.cacheDir, "tts_output.wav")
        val utteranceId = "utt_${System.currentTimeMillis()}"

        try {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    val timeline = if (onAmplitude != null) computeAmplitudeTimeline(audioFile) else null
                    playAudioFile(audioFile, timeline, onStart, onFinish, onError, onAmplitude)
                }
                override fun onError(utteranceId: String?) {
                    onError(Exception("TTS ошибка синтеза"))
                }
            })
            val result = tts?.synthesizeToFile(text, null, audioFile, utteranceId)
            if (result == TextToSpeech.ERROR) onError(Exception("Ошибка TTS synthesizeToFile"))
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun computeAmplitudeTimeline(file: File, chunkMs: Int = 40): List<Float> {
        val sampleRate = readWavSampleRate(file)
        val samplesPerChunk = sampleRate * chunkMs / 1000
        val timeline = mutableListOf<Float>()
        lipSyncEngine.reset()
        try {
            FileInputStream(file).use { fis ->
                fis.skip(44)
                val byteBuffer = ByteArray(samplesPerChunk * 2)
                val shortBuffer = ShortArray(samplesPerChunk)
                while (true) {
                    val bytesRead = fis.read(byteBuffer)
                    if (bytesRead <= 0) break
                    val samplesRead = bytesRead / 2
                    for (i in 0 until samplesRead) {
                        shortBuffer[i] = ((byteBuffer[i * 2].toInt() and 0xFF) or
                                (byteBuffer[i * 2 + 1].toInt() shl 8)).toShort()
                    }
                    timeline.add(lipSyncEngine.processChunk(shortBuffer.copyOf(samplesRead)))
                }
            }
        } catch (_: Exception) {}
        lipSyncEngine.reset()
        return timeline
    }

    private fun readWavSampleRate(file: File): Int {
        val header = ByteArray(44)
        FileInputStream(file).use { it.read(header) }
        return (header[24].toInt() and 0xFF) or
                ((header[25].toInt() and 0xFF) shl 8) or
                ((header[26].toInt() and 0xFF) shl 16) or
                ((header[27].toInt() and 0xFF) shl 24)
    }

    private fun playAudioFile(
        file: File,
        amplitudeTimeline: List<Float>?,
        onStart: () -> Unit,
        onFinish: () -> Unit,
        onError: (Throwable) -> Unit,
        onAmplitude: ((Float) -> Unit)?
    ) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                playbackParams = android.media.PlaybackParams().setPitch(1.4f)
                start()
                onStart()

                if (amplitudeTimeline != null && onAmplitude != null) {
                    timelineJob?.cancel()
                    timelineJob = scope.launch {
                        for (amplitude in amplitudeTimeline) {
                            withContext(Dispatchers.Main) { onAmplitude(amplitude) }
                            delay(40L)
                        }
                        withContext(Dispatchers.Main) { onAmplitude(0f) }
                    }
                }

                setOnCompletionListener {
                    timelineJob?.cancel()
                    onAmplitude?.invoke(0f)
                    onFinish()
                    release()
                }
                setOnErrorListener { _, what, extra ->
                    timelineJob?.cancel()
                    onError(Exception("MediaPlayer error $what/$extra"))
                    release()
                    true
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stop() {
        timelineJob?.cancel()
        mediaPlayer?.let {
            try { if (it.isPlaying) it.stop(); it.release() } catch (_: Exception) {}
        }
        mediaPlayer = null
        tts?.stop()
    }

    fun release() {
        stop()
        scope.cancel()
        tts?.shutdown()
        tts = null
    }
}
