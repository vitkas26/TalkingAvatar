package kg.nurtelecom.o.talkingavatar.speech.akylai

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kg.nurtelecom.o.talkingavatar.speech.EngineSettings
import kg.nurtelecom.o.talkingavatar.speech.SttEngine
import kg.nurtelecom.o.talkingavatar.speech.vad.SilenceTracker
import kg.nurtelecom.o.talkingavatar.speech.vad.VadConfig
import kg.nurtelecom.o.talkingavatar.speech.vad.VadDecision
import kg.nurtelecom.o.talkingavatar.speech.vad.mediaRecorderAmplitudeDbfs
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
private const val AMPLITUDE_POLL_INTERVAL_MS = 100L

// Реальный HTTP-клиент к локальному AkylAI-STT сервису (см. AkylAiApiService/AkylAiConfig).
// Пока сервис не поднят — падает с connection-refused через обычный onError, этого достаточно
// чтобы независимо тестировать роутинг/UI без готового бэкенда.
class AkylAiSttEngine(
    private val context: Context,
    private val apiService: AkylAiApiService,
    private val engineSettings: EngineSettings,
) : SttEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var recorder: MediaRecorder? = null
    private var recordingJob: Job? = null

    override fun startListening(
        language: String,
        onProcessingStarted: () -> Unit,
        onResult: (String, String?) -> Unit,
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
            val silenceTracker = SilenceTracker(
                VadConfig(
                    silenceThresholdDb = engineSettings.vadSilenceThresholdDb,
                    silenceDurationMs = engineSettings.vadSilenceDurationMs,
                    maxRecordingMs = engineSettings.vadMaxRecordingMs,
                ),
            )
            var elapsed = 0L
            while (true) {
                delay(AMPLITUDE_POLL_INTERVAL_MS)
                elapsed += AMPLITUDE_POLL_INTERVAL_MS
                val amplitude = runCatching { newRecorder.maxAmplitude }.getOrDefault(0)
                val levelDb = mediaRecorderAmplitudeDbfs(amplitude)
                if (silenceTracker.onSample(levelDb, elapsed) != VadDecision.Continue) break
            }
            stopRecorder()
            withContext(Dispatchers.Main) { onProcessingStarted() }

            if (!silenceTracker.hasDetectedSpeech) {
                Log.d(TAG, "VAD: тишина, не отправляем на сервер")
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
                    if (text.isNullOrBlank()) onError(Exception("AkylAI-STT: пустой результат")) else onResult(text, null)
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
