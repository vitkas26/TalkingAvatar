package kg.nurtelecom.o.talkingavatar.data.speech.akylai

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kg.nurtelecom.o.talkingavatar.domain.gateway.TtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "AkylAiTtsEngine"

// Реальный HTTP-клиент к локальному AkylAI-TTS-mini сервису (см. AkylAiApiService/AkylAiConfig).
// Пока сервис не поднят — падает ошибкой сети через обычный onError, этого достаточно чтобы
// независимо тестировать роутинг/UI без готового бэкенда.
class AkylAiTtsEngine(
    private val context: Context,
    private val apiService: AkylAiApiService,
) : TtsEngine {

    private var mediaPlayer: MediaPlayer? = null

    override suspend fun speak(
        text: String,
        language: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        try {
            val audioFile = File(context.cacheDir, "akylai_output.wav")
            val body = apiService.synthesize(AkylAiTtsRequest(text = text, language = language))
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
                    onError(Exception("AkylAI-TTS: MediaPlayer ошибка $what/$extra"))
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
            // isPlaying() кидает IllegalStateException в состояниях Error/End (см. тот же баг
            // в AudioPlayer.kt) — если MediaPlayer уже сам себя release() из onCompletion, а
            // stop() дёрнули повторно до того как поле успело обнулиться.
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
