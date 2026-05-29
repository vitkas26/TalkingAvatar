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
import androidx.compose.foundation.layout.systemBarsPadding
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
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarSceneView
import kg.nurtelecom.o.talkingavatar.ui.utils.AudioPlayer
import kg.nurtelecom.o.talkingavatar.ui.utils.PulseIndicator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private val STOP_COMMANDS = setOf("стоп", "stop", "ок", "ok", "хватит", "тихо", "замолчи")

private fun isStopCommand(text: String): Boolean {
    val lower = text.lowercase().trim()
    return STOP_COMMANDS.any { lower == it || lower.contains(it) }
}

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

    val isSpeakingRef = remember { mutableStateOf(false) }
    LaunchedEffect(state.isSpeaking) { isSpeakingRef.value = state.isSpeaking }

    val isPreparingRef = remember { mutableStateOf(false) }

    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    LaunchedEffect(state.isPreparing) {
        isPreparingRef.value = state.isPreparing
        if (state.isPreparing) {
            speechRecognizer.cancel()
            speechRecognizer.startListening(recognitionIntent)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
        else scope.launch { snackBarHostState.showSnackbar("Требуется разрешение на микрофон") }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                val busyWithAi = isSpeakingRef.value || isPreparingRef.value
                when {
                    text.isNullOrBlank() -> {
                        if (isPreparingRef.value) speechRecognizer.startListening(recognitionIntent)
                        else viewModel.startListening()
                    }
                    busyWithAi && isStopCommand(text) -> viewModel.stopAndRestart()
                    busyWithAi -> speechRecognizer.startListening(recognitionIntent)
                    else -> viewModel.onSpeechResult(text)
                }
            }

            override fun onError(error: Int) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isPreparingRef.value) speechRecognizer.startListening(recognitionIntent)
                    else viewModel.startListening()
                }, 300L)
            }
        })
        onDispose {
            audioPlayer.release()
            speechRecognizer.destroy()
        }
    }

    LaunchedEffect(Unit) {
        audioPlayer.initialize()
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.startListening()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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

            MainSideEffect.TriggerEarListen -> {
                scope.launch { avatarRenderer?.playEarListenGesture() }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AvatarSceneView(
            modifier = Modifier.fillMaxSize(),
            onRendererReady = { avatarRenderer = it }
        )

        if (state.isPreparing) {
            PulseIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp),
                icon = R.drawable.ic_thinking
            )
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
