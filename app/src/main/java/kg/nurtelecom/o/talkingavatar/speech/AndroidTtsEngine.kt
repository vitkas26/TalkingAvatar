package kg.nurtelecom.o.talkingavatar.speech

import android.content.Context
import kg.nurtelecom.o.talkingavatar.ui.utils.AudioPlayer

class AndroidTtsEngine(context: Context) : TtsEngine {

    private val audioPlayer = AudioPlayer(context).apply { initialize() }

    override suspend fun speak(
        text: String,
        language: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        audioPlayer.play(text = text, languageTag = language, onStart = onStart, onFinish = onDone, onError = onError)
    }

    override fun stop() {
        audioPlayer.stop()
    }
}
