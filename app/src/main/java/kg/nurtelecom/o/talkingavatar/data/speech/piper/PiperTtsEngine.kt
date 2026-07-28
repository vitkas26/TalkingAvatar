package kg.nurtelecom.o.talkingavatar.data.speech.piper

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.google.gson.Gson
import kg.nurtelecom.o.talkingavatar.domain.gateway.TtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File

private const val TAG = "PiperTtsEngine"

class PiperTtsEngine(
    private val context: Context,
    private val apiService: PiperApiService,
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
            val audioFile = File(context.cacheDir, "piper_output.wav")
            // ISO-639-1 из BCP-47 кода ("ru-RU" -> "ru"), как в WhisperSttEngine. Пусто/неизвестно —
            // не шлём поле, сервер возьмёт свой дефолт "ru".
            val languageCode = language.substringBefore("-").lowercase().takeIf { it.isNotBlank() }
            val body = apiService.synthesize(PiperTtsRequest(text = text, language = languageCode))
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
                    onError(Exception("Piper-TTS: MediaPlayer ошибка $what/$extra"))
                    release()
                    mediaPlayer = null
                    true
                }
            }
        } catch (e: HttpException) {
            // Сервер на 400/500 шлёт JSON {"error": "..."} вместо WAV — вытаскиваем текст
            // ошибки оттуда, если получается, иначе просто HTTP-код.
            val message = runCatching {
                e.response()?.errorBody()?.string()
                    ?.let { Gson().fromJson(it, PiperErrorResponse::class.java) }
                    ?.error
            }.getOrNull() ?: e.message()
            Log.e(TAG, "synthesize failed: $message", e)
            onError(Exception("Piper-TTS: $message"))
        } catch (e: Exception) {
            Log.e(TAG, "synthesize failed", e)
            onError(e)
        }
    }

    override fun stop() {
        mediaPlayer?.let {
            // isPlaying() кидает IllegalStateException в состояниях Error/End (см. тот же баг
            // в AudioPlayer.kt/AkylAiTtsEngine.kt) — если MediaPlayer уже сам себя release()
            // из onCompletion, а stop() дёрнули повторно до того как поле успело обнулиться.
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
