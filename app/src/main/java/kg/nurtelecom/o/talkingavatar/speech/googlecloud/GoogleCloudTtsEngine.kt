package kg.nurtelecom.o.talkingavatar.speech.googlecloud

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kg.nurtelecom.o.talkingavatar.speech.TtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "GoogleCloudTtsEngine"

class GoogleCloudTtsEngine(
    private val context: Context,
    private val apiService: GoogleCloudApiService,
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
            val body = apiService.synthesize(GoogleCloudTtsRequest(text = text, languageCode = language))
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
