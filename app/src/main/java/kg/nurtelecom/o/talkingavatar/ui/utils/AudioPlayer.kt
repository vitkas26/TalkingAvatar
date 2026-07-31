package kg.nurtelecom.o.talkingavatar.ui.utils

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.util.Locale

private const val TAG = "AudioPlayer"

// Заполняется по факту прогона на тестовом телефоне — подставить реальное имя после прослушивания.
// en/zh не заданы: там "любой голос под язык" уже случайно попал в женский, трогать не стал.
private val preferredFemaleVoiceByLanguage = mapOf(
    "ru" to "ru-ru-x-rue-local", // кандидат на замену rud (сейчас мужик) — нужно прослушать
    "tr" to "tr-tr-x-efu-network", // кандидат, вариантов было 3 (tmc/efu/mfm) — нужно прослушать
)

class AudioPlayer(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isReady = false
    private var pendingPlay: (() -> Unit)? = null

    fun initialize() {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                Log.d(TAG, "onInit status=$status")
                isReady = true
                pendingPlay?.invoke()
                pendingPlay = null
            }
        }
    }

    fun play(
        text: String,
        languageTag: String = "ru-RU",
        onStart: () -> Unit = {},
        onFinish: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        // TextToSpeech(...) возвращает объект сразу, но движок биндится асинхронно —
        // synthesizeToFile() до onInit(SUCCESS) падает с ERROR. Копим один отложенный
        // вызов и проигрываем его, когда движок реально готов.
        if (!isReady) {
            pendingPlay = { playReady(text, languageTag, onStart, onFinish, onError) }
            return
        }
        playReady(text, languageTag, onStart, onFinish, onError)
    }

    private fun applyVoice(languageTag: String) {
        val locale = Locale.forLanguageTag(languageTag)
        tts?.language = locale
        val voices = tts?.voices.orEmpty()

        val preferredName = preferredFemaleVoiceByLanguage[locale.language]
        val exactMatch = voices.find { it.name == preferredName }

        // fallback-цепочка, если точное имя не нашлось (другое устройство/движок)
        val heuristicMatch = voices.find {
            it.locale.language == locale.language && it.name.contains("female", ignoreCase = true)
        }
        val anyVoiceForLanguage = voices.find { it.locale.language == locale.language }

        val chosen = exactMatch ?: heuristicMatch ?: anyVoiceForLanguage
        // TextToSpeech.setVoice(null) кидает NPE внутри самого Android API (voice.getName()
        // без null-проверки) — если под язык вообще нет голоса (например ky-KG), просто не
        // трогаем voice, движок озвучит текущим/дефолтным голосом вместо краша.
        if (chosen != null) {
            tts?.voice = chosen
        }
        Log.d(TAG, "languageTag=$languageTag -> voice=${chosen?.name} (exact=${exactMatch != null})")
    }

    private fun playReady(
        text: String,
        languageTag: String,
        onStart: () -> Unit,
        onFinish: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        applyVoice(languageTag)
        val audioFile = File(context.cacheDir, "tts_output.wav")
        val utteranceId = "utt_${System.currentTimeMillis()}"

        try {
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?){}

                override fun onDone(utteranceId: String?) {
                    playAudioFile(audioFile, onStart, onFinish, onError)
                }

                override fun onError(utteranceId: String?) {
                    onError(Exception("TTS ошибка преобразования текста"))
                }
            })

            val result = tts?.synthesizeToFile(text, null, audioFile, utteranceId)
            if (result == TextToSpeech.ERROR) {
                onError(Exception("Ошибка преобразования текста в файл"))
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun playAudioFile(
        file: File,
        onStart: () -> Unit,
        onFinish: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                onStart()
                setOnCompletionListener {
                    onFinish()
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, what, extra ->
                    onError(Exception("MediaPlayer ошибка $what подробности = $extra"))
                    release()
                    mediaPlayer = null
                    true
                }
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stop() {
        mediaPlayer?.let {
            // isPlaying() кидает IllegalStateException в состояниях Error/End (например если
            // MediaPlayer уже сам себя release() из onCompletion, а stop() дёрнули повторно
            // до того как поле mediaPlayer успело обнулиться, при быстром переключении движков).
            try {
                if (it.isPlaying) it.stop()
            } catch (e: IllegalStateException) {
                Log.d(TAG, "isPlaying()/stop() on invalid-state MediaPlayer: ${e.message}")
            }
            it.release()
        }
        mediaPlayer = null
        tts?.stop()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
