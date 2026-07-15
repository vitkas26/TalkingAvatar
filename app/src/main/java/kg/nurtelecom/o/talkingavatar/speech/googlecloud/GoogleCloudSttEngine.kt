package kg.nurtelecom.o.talkingavatar.speech.googlecloud

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kg.nurtelecom.o.talkingavatar.speech.EngineSettings
import kg.nurtelecom.o.talkingavatar.speech.SttEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

private const val TAG = "GoogleCloudSttEngine"

// Нет VAD, как и в WhisperSttEngine — пишем фиксированную длительность.
private const val RECORD_DURATION_MS = 5_000L
private const val SAMPLE_RATE = 16_000

// Дефолтные alt-языки для Google Speech-to-text (alternativeLanguageCodes, лимит Google — 3
// на запрос) по основному языку — используются только когда EngineSettings.googleCloudAltLanguages
// пуст (т.е. пользователь не задал ручной оверрайд с экрана настроек). ky-KG/ru-RU идут друг у
// друга первым приоритетом — самый частый code-switching в реальном использовании на пилоте;
// остальные языки получают ru-RU и ky-KG первыми по той же причине (локальный код-свитчинг
// подмешивается в любой язык на этом пилоте), третий слот — следующий вероятный кандидат.
val defaultAltLanguagesByPrimary: Map<String, String> = mapOf(
    "ky-KG" to "ru-RU,en-US,tr-TR",
    "ru-RU" to "ky-KG,en-US,tr-TR",
    "en-US" to "ru-RU,ky-KG,tr-TR",
    "tr-TR" to "ru-RU,ky-KG,en-US",
    "zh-CN" to "ru-RU,ky-KG,en-US",
    "de-DE" to "ru-RU,ky-KG,en-US",
)

// В отличие от Whisper/AkylAI, google-cloud-proxy ждёт сырые PCM-байты без WAV-заголовка
// (LINEAR16/16kHz/mono, Content-Type: application/octet-stream) — конфиг аудио передаётся
// явно через query-параметры, а не через файл-контейнер.
class GoogleCloudSttEngine(
    private val context: Context,
    private val apiService: GoogleCloudApiService,
    private val engineSettings: EngineSettings,
) : SttEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    override fun startListening(
        language: String,
        onProcessingStarted: () -> Unit,
        onResult: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) {
            onError(Exception("AudioRecord недоступен на устройстве"))
            return
        }

        val newRecord: AudioRecord
        try {
            @Suppress("MissingPermission")
            newRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 4,
            )
            if (newRecord.state != AudioRecord.STATE_INITIALIZED) {
                newRecord.release()
                onError(Exception("AudioRecord не инициализирован"))
                return
            }
            newRecord.startRecording()
            audioRecord = newRecord
        } catch (e: Exception) {
            onError(e)
            return
        }

        recordingJob = scope.launch {
            val pcm = ByteArrayOutputStream()
            val readBuffer = ByteArray(minBufferSize)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < RECORD_DURATION_MS) {
                val read = newRecord.read(readBuffer, 0, readBuffer.size)
                if (read > 0) pcm.write(readBuffer, 0, read)
            }
            stopRecorder()
            withContext(Dispatchers.Main) { onProcessingStarted() }

            try {
                val body = pcm.toByteArray().toRequestBody("application/octet-stream".toMediaType())
                val alt = engineSettings.googleCloudAltLanguages.takeIf { it.isNotBlank() }
                    ?: defaultAltLanguagesByPrimary[language]
                val response = apiService.transcribe(
                    audio = body,
                    lang = language,
                    alt = alt,
                    apiVersion = engineSettings.googleCloudApiVersion,
                )
                val text = response.transcript
                withContext(Dispatchers.Main) {
                    if (text.isNullOrBlank()) onError(Exception("Google Cloud STT: пустой результат")) else onResult(text)
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
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.d(TAG, "stop() before start or already stopped: ${e.message}")
        }
        audioRecord?.release()
        audioRecord = null
    }
}
