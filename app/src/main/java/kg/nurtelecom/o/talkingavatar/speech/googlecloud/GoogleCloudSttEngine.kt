package kg.nurtelecom.o.talkingavatar.speech.googlecloud

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kg.nurtelecom.o.talkingavatar.speech.EngineSettings
import kg.nurtelecom.o.talkingavatar.speech.SttEngine
import kg.nurtelecom.o.talkingavatar.speech.vad.SilenceTracker
import kg.nurtelecom.o.talkingavatar.speech.vad.VadConfig
import kg.nurtelecom.o.talkingavatar.speech.vad.VadDecision
import kg.nurtelecom.o.talkingavatar.speech.vad.pcm16BytesDbfs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "GoogleCloudSttEngine"
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

// Шлём WAV (заголовок + LINEAR16/16kHz/mono), как и WhisperSttEngine — V1-провайдер прокси
// прекрасно работает с явным заголовком (проверено вручную curl'ом), а V2 (autoDecodingConfig)
// без заголовка вообще не может определить контейнер и падает с 400 "unsupported encoding".
// Раньше слали голый PCM без заголовка — работало только под V1, ломало V2.
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
        onResult: (String, String?) -> Unit,
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
            val silenceTracker = SilenceTracker(
                VadConfig(
                    silenceThresholdDb = engineSettings.vadSilenceThresholdDb,
                    silenceDurationMs = engineSettings.vadSilenceDurationMs,
                    maxRecordingMs = engineSettings.vadMaxRecordingMs,
                ),
            )
            val startTime = System.currentTimeMillis()
            while (true) {
                val read = newRecord.read(readBuffer, 0, readBuffer.size)
                if (read > 0) pcm.write(readBuffer, 0, read)
                val elapsed = System.currentTimeMillis() - startTime
                val levelDb = pcm16BytesDbfs(readBuffer, read)
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
                val audioFile = File(context.cacheDir, "googlecloud_input.wav")
                writeWavFile(audioFile, pcm.toByteArray(), SAMPLE_RATE)
                val body = audioFile.asRequestBody("audio/wav".toMediaType())
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
                    if (text.isNullOrBlank()) {
                        onError(Exception("Google Cloud STT: пустой результат"))
                    } else {
                        onResult(text, response.languageDetected)
                    }
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

// AudioRecord отдаёт сырой PCM без контейнера — оборачиваем в canonical 44-байтный WAV-заголовок
// (mono/16-bit/16kHz), как в WhisperSttEngine.writeWavFile.
private fun writeWavFile(file: File, pcmData: ByteArray, sampleRate: Int) {
    val channels = 1
    val bitsPerSample = 16
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8
    val dataSize = pcmData.size

    file.outputStream().use { out ->
        out.write("RIFF".toByteArray())
        out.write(intToBytesLE(36 + dataSize))
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(intToBytesLE(16))
        out.write(shortToBytesLE(1))
        out.write(shortToBytesLE(channels))
        out.write(intToBytesLE(sampleRate))
        out.write(intToBytesLE(byteRate))
        out.write(shortToBytesLE(blockAlign))
        out.write(shortToBytesLE(bitsPerSample))
        out.write("data".toByteArray())
        out.write(intToBytesLE(dataSize))
        out.write(pcmData)
    }
}

private fun intToBytesLE(v: Int) = byteArrayOf(
    (v and 0xff).toByte(),
    ((v shr 8) and 0xff).toByte(),
    ((v shr 16) and 0xff).toByte(),
    ((v shr 24) and 0xff).toByte(),
)

private fun shortToBytesLE(v: Int) = byteArrayOf(
    (v and 0xff).toByte(),
    ((v shr 8) and 0xff).toByte(),
)
