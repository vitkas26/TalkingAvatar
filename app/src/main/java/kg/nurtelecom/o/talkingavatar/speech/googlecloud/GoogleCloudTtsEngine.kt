package kg.nurtelecom.o.talkingavatar.speech.googlecloud

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kg.nurtelecom.o.talkingavatar.speech.EngineSettings
import kg.nurtelecom.o.talkingavatar.speech.TtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "GoogleCloudTtsEngine"

// Debug WaveNet-оверрайд (см. EngineSettings.googleCloudTtsTier) — на голос из этой мапы, а не
// gender-based авто-выбор Chirp3-HD на проксе. googleLanguageCode может отличаться от ключа
// (session-языка): Google регистрирует китайские голоса под "cmn-CN", не "zh-CN" — прокси не
// ремапит languageCode/voiceName (TtsRoutes.kt шлёт их as-is в Google), так что явный mismatch
// ("zh-CN" + "cmn-CN-Wavenet-A") — гарантированный 400 от Google.
private data class WavenetVoice(val googleLanguageCode: String, val voiceName: String)

private val wavenetVoiceByLanguage: Map<String, WavenetVoice> = mapOf(
    "ru-RU" to WavenetVoice(googleLanguageCode = "ru-RU", voiceName = "ru-RU-Wavenet-A"),
    "en-US" to WavenetVoice(googleLanguageCode = "en-US", voiceName = "en-US-Wavenet-C"),
    "de-DE" to WavenetVoice(googleLanguageCode = "de-DE", voiceName = "de-DE-Wavenet-G"),
    "tr-TR" to WavenetVoice(googleLanguageCode = "tr-TR", voiceName = "tr-TR-Wavenet-A"),
    "zh-CN" to WavenetVoice(googleLanguageCode = "cmn-CN", voiceName = "cmn-CN-Wavenet-A"),
)

class GoogleCloudTtsEngine(
    private val context: Context,
    private val apiService: GoogleCloudApiService,
    private val engineSettings: EngineSettings,
) : TtsEngine {

    private var mediaPlayer: MediaPlayer? = null

    override suspend fun speak(
        text: String,
        language: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        // Google Cloud TTS кыргызский не поддерживает — не шлём запрос, чтобы не получить
        // невнятную ошибку от прокси. Для ky-KG в UI используется AkylAI-TTS.
        if (language.startsWith("ky")) {
            onError(Exception("Google Cloud TTS: кыргызский (ky) не поддерживается, используйте AkylAI"))
            return
        }
        try {
            val audioFile = File(context.cacheDir, "googlecloud_output.ogg")
            val wavenetVoice = wavenetVoiceByLanguage[language]
                .takeIf { engineSettings.googleCloudTtsTier == "wavenet" }
            val request = if (wavenetVoice != null) {
                GoogleCloudTtsRequest(
                    text = text,
                    languageCode = wavenetVoice.googleLanguageCode,
                    voiceName = wavenetVoice.voiceName,
                )
            } else {
                GoogleCloudTtsRequest(text = text, languageCode = language)
            }
            val body = apiService.synthesize(request)
            withContext(Dispatchers.IO) {
                body.byteStream().use { input ->
                    audioFile.outputStream().use { output -> input.copyTo(output) }
                }
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
                onStart()
                setOnCompletionListener {
                    onDone()
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, what, extra ->
                    onError(Exception("Google Cloud TTS: MediaPlayer ошибка $what/$extra"))
                    release()
                    mediaPlayer = null
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "synthesize failed", e)
            onError(e)
        }
    }

    override fun stop() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: IllegalStateException) {
                Log.d(TAG, "isPlaying()/stop() on invalid-state MediaPlayer: ${e.message}")
            }
            it.release()
        }
        mediaPlayer = null
    }
}
