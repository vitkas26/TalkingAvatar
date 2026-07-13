package kg.nurtelecom.o.talkingavatar.speech.akylai

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kg.nurtelecom.o.talkingavatar.speech.SttEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

private const val TAG = "AkylAiSttEngine"
private const val RECORD_DURATION_MS = 5_000L
private const val AMPLITUDE_POLL_INTERVAL_MS = 100L
// Грубый порог тишины по getMaxAmplitude() (диапазон 0..32767 у 16-бит PCM). Нет настоящего VAD
// (в отличие от системного SpeechRecognizer) — если за весь клип пик громкости ниже порога,
// считаем что речи не было и не шлём файл на сервер вообще. Порог приблизительный, подбирался
// не на реальных данных — если ловит шум/не ловит тихую речь, подкрутить значение.
private const val SILENCE_AMPLITUDE_THRESHOLD = 400

// Реальный HTTP-клиент к локальному AkylAI-STT сервису (см. AkylAiApiService/AkylAiConfig).
// Пока сервис не поднят — падает с connection-refused через обычный onError, этого достаточно
// чтобы независимо тестировать роутинг/UI без готового бэкенда.
class AkylAiSttEngine(
    private val context: Context,
    private val apiService: AkylAiApiService,
) : SttEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var recorder: MediaRecorder? = null
    private var recordingJob: Job? = null

    override fun startListening(
        language: String,
        onProcessingStarted: () -> Unit,
        onResult: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val audioFile = File(context.cacheDir, "akylai_input.m4a")

        val newRecorder: MediaRecorder
        try {
            @Suppress("DEPRECATION")
            newRecorder = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
        } catch (e: Exception) {
            onError(e)
            return
        }

        recordingJob = scope.launch {
            var peakAmplitude = 0
            var elapsed = 0L
            while (elapsed < RECORD_DURATION_MS) {
                delay(AMPLITUDE_POLL_INTERVAL_MS)
                elapsed += AMPLITUDE_POLL_INTERVAL_MS
                peakAmplitude = maxOf(peakAmplitude, runCatching { newRecorder.maxAmplitude }.getOrDefault(0))
            }
            stopRecorder()
            withContext(Dispatchers.Main) { onProcessingStarted() }

            if (peakAmplitude < SILENCE_AMPLITUDE_THRESHOLD) {
                Log.d(TAG, "silence detected (peak=$peakAmplitude < $SILENCE_AMPLITUDE_THRESHOLD), skipping upload")
                withContext(Dispatchers.Main) { onError(Exception("Речь не распознана")) }
                return@launch
            }

            try {
                val filePart = MultipartBody.Part.createFormData(
                    "audio",
                    audioFile.name,
                    audioFile.asRequestBody("audio/mp4".toMediaType()),
                )
                val response = apiService.transcribe(filePart)
                val text = response.text
                withContext(Dispatchers.Main) {
                    if (text.isNullOrBlank()) onError(Exception("AkylAI-STT: пустой результат")) else onResult(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "transcribe failed", e)
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    override fun stopListening() {
        recordingJob?.cancel()
        stopRecorder()
    }

    private fun stopRecorder() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.d(TAG, "stop() before start or already stopped: ${e.message}")
        }
        recorder?.release()
        recorder = null
    }
}
