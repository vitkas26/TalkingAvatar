package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarSceneView
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.ui.utils.AudioPlayer
import kg.nurtelecom.o.talkingavatar.ui.utils.PulseIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private val STOP_COMMANDS = setOf("стоп", "stop", "ок", "ok", "хватит", "тихо", "замолчи")

private fun isStopCommand(text: String) =
    STOP_COMMANDS.any { text.lowercase().trim().contains(it) }

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
fun MainScreen() {
    val viewModel = koinViewModel<MainViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    val audioPlayer = remember { AudioPlayer(context) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var avatarRenderer by remember { mutableStateOf<AvatarRenderer?>(null) }

    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
    }

    val pendingTextState = remember { mutableStateOf("") }
    val commitJobRef = remember { mutableStateOf<Job?>(null) }

    val normalListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(v: Float) = Unit
            override fun onBufferReceived(b: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(r: Bundle?) = Unit
            override fun onEvent(e: Int, p: Bundle?) = Unit

            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty().trim()
                if (text.isNotBlank()) {
                    pendingTextState.value = (pendingTextState.value + " " + text).trim()
                    commitJobRef.value?.cancel()
                    commitJobRef.value = scope.launch {
                        delay(2000L)
                        val final = pendingTextState.value
                        pendingTextState.value = ""
                        if (final.isNotBlank()) {
                            speechRecognizer.cancel()
                            viewModel.onSpeechResult(final)
                        }
                    }
                }
                speechRecognizer.startListening(recognitionIntent)
            }

            override fun onError(error: Int) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (pendingTextState.value.isNotBlank()) {
                        speechRecognizer.startListening(recognitionIntent)
                    } else {
                        viewModel.startListening()
                    }
                }, 300L)
            }
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(normalListener)
        onDispose {
            audioPlayer.release()
            speechRecognizer.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
        else scope.launch { snackBarHostState.showSnackbar("Требуется разрешение на микрофон") }
    }

    val busy = state.isSpeaking || state.isPreparing
    LaunchedEffect(busy) {
        if (busy) {
            commitJobRef.value?.cancel()
            pendingTextState.value = ""
        }
        if (!busy) return@LaunchedEffect
        callbackFlow {
            val handler = Handler(Looper.getMainLooper())
            fun restart() {
                handler.postDelayed({
                    if (isActive) speechRecognizer.startListening(recognitionIntent)
                }, 150L)
            }
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(v: Float) = Unit
                override fun onBufferReceived(b: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(r: Bundle?) = Unit
                override fun onEvent(e: Int, p: Bundle?) = Unit
                override fun onResults(results: Bundle) {
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let { trySend(it) }
                    restart()
                }
                override fun onError(error: Int) { restart() }
            })
            speechRecognizer.cancel()
            speechRecognizer.startListening(recognitionIntent)
            awaitClose {
                handler.removeCallbacksAndMessages(null)
                speechRecognizer.cancel()
                speechRecognizer.setRecognitionListener(normalListener)
            }
        }
            .debounce(300L)
            .filter { isStopCommand(it) }
            .collect { viewModel.stopAndRestart() }
    }

    LaunchedEffect(Unit) {
        audioPlayer.initialize()
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.startListening()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(state.isListening) {
        avatarRenderer?.setListening(state.isListening)
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MainSideEffect.StartSpeechRecognition -> {
                speechRecognizer.cancel()
                speechRecognizer.startListening(recognitionIntent)
            }
            is MainSideEffect.SpeakAnswer -> {
                audioPlayer.play(
                    text = sideEffect.text,
                    onStart = { viewModel.onSpeakingStarted() },
                    onFinish = { viewModel.onSpeakingFinished() },
                    onError = {
                        viewModel.onSpeakingFinished()
                        scope.launch { snackBarHostState.showSnackbar("Ошибка TTS: ${it.message}") }
                    },
                    onAmplitude = { amp -> avatarRenderer?.setLipSyncAmplitude(amp) }
                )
            }
            is MainSideEffect.ShowError -> {
                scope.launch { snackBarHostState.showSnackbar(sideEffect.message) }
            }
            MainSideEffect.StopSpeaking -> {
                audioPlayer.stop()
                avatarRenderer?.setIdle()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AvatarSceneView(
            modifier = Modifier.fillMaxSize(),
            onRendererReady = { renderer ->
                avatarRenderer = renderer
                renderer.setListening(state.isListening)
            }
        )
        if (state.isPreparing) {
            PulseIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                icon = R.drawable.ic_thinking,
                size = 72.dp
            )
        }
        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
