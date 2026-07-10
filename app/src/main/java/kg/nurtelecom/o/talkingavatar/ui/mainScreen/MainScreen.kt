package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kg.nurtelecom.o.talkingavatar.statemachine.AvatarState
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val viewModel = koinViewModel<MainViewModel>()
    val avatarRenderer = koinInject<AvatarRenderer>()
    val state by viewModel.collectAsState()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (state.isSpeaking)
                viewModel.stopAndRestart()
            else
                viewModel.startListening()
        } else scope.launch { snackBarHostState.showSnackbar("Требуется разрешение на микрофон") }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MainSideEffect.ShowError -> {
                scope.launch { snackBarHostState.showSnackbar(sideEffect.message) }
            }
            MainSideEffect.RequestListening -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val avatarState = when {
        state.isLanguageSheetOpen -> AvatarState.Idle
        state.isListening -> AvatarState.Listening
        state.isPreparing -> AvatarState.Processing
        state.isSpeaking -> AvatarState.Speaking
        state.error != null -> AvatarState.Error
        !state.hasSelectedLanguage -> AvatarState.Welcome
        else -> AvatarState.Idle
    }

    Box(modifier = Modifier.fillMaxSize()) {
        avatarRenderer.Render(avatarState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { viewModel.showLanguageSheet() }) {
                Text("Выбрать язык")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
                enabled = !state.isPreparing && !state.isSpeaking) {
                Text("Задать вопрос")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.stopSpeaking() },
                enabled = state.isSpeaking
            ) {
                Text("Стоп")
            }

            SnackbarHost(hostState = snackBarHostState)
        }
    }

    if (state.isLanguageSheetOpen) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideLanguageSheet() },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Language.entries.forEach { language ->
                    TextButton(
                        onClick = { viewModel.selectLanguage(language) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(language.displayName)
                    }
                }
            }
        }
    }
}
