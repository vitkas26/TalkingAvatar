package kg.nurtelecom.o.talkingavatar.ui.conversation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kg.nurtelecom.o.talkingavatar.domain.model.Language
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.ErrorSnackbar
import kg.nurtelecom.o.talkingavatar.ui.conversation.navigation.Listening
import kg.nurtelecom.o.talkingavatar.ui.conversation.navigation.Speaking
import kg.nurtelecom.o.talkingavatar.ui.conversation.navigation.Welcome
import kg.nurtelecom.o.talkingavatar.ui.conversation.screen.ListeningScreen
import kg.nurtelecom.o.talkingavatar.ui.conversation.screen.SpeakingScreen
import kg.nurtelecom.o.talkingavatar.ui.conversation.screen.WelcomeScreen
import kg.nurtelecom.o.talkingavatar.ui.conversation.sheet.SheetHostContent
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

// Хост разговора: Welcome/Listening/Speaking — отдельные Nav-destination с обычным fade между
// ними (см. обсуждение архитектуры). MainViewModel общий на все три (резолвится один раз здесь,
// т.к. ConversationNavHost вызывается прямо из MainActivity — ViewModelStoreOwner = Activity,
// экраны получают его как параметр конструктора, а не сами лезут в Koin/backstack).
// Навигация — чистое следствие MainState (isListening/isPreparing/isSpeaking): VM ничего не
// знает о NavController, только меняет стейт; этот эффект переводит экран когда стейт поменялся.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationNavHost() {
    val viewModel = koinViewModel<MainViewModel>()
    val avatarRenderer = koinInject<AvatarRenderer>()
    val state by viewModel.collectAsState()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            if (state.isSpeaking) viewModel.stopAndRestart() else viewModel.startListening()
        } else scope.launch { snackBarHostState.showSnackbar("Требуется разрешение на микрофон") }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MainSideEffect.ShowError -> scope.launch { snackBarHostState.showSnackbar(sideEffect.message) }
        }
    }

    // activePhase — то, какой экран сейчас реально показан (не запрос к NavController на каждую
    // рекомпозицию, а то что мы сами туда навигировали последним разом).
    var activePhase by remember { mutableStateOf<Any>(Welcome) }
    var reverseFromListening by remember { mutableStateOf(false) }
    LaunchedEffect(state.isListening, state.isPreparing, state.isSpeaking, state.showWelcome) {
        when {
            (state.isListening || state.isPreparing) && activePhase != Listening -> {
                activePhase = Listening
                navController.navigate(Listening)
            }
            state.isSpeaking && activePhase != Speaking -> {
                activePhase = Speaking
                navController.navigate(Speaking) { popUpTo<Welcome>() }
            }
            // showWelcome — явный сигнал от VM (стоп/отмена/ошибка сразу, естественное
            // завершение речи — через 5-минутный idle-таймер, см. MainVM.resetIdleTimer).
            // Не просто "все три флага false", иначе после Speaking сразу скакало бы в Welcome
            // без ожидания.
            state.showWelcome && !state.isListening && !state.isPreparing && !state.isSpeaking &&
                activePhase != Welcome -> {
                reverseFromListening = activePhase == Listening
                activePhase = Welcome
                navController.navigate(Welcome) { popUpTo<Welcome> { inclusive = true } }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Welcome,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition = { fadeOut(tween(350)) },
            popEnterTransition = { fadeIn(tween(350)) },
            popExitTransition = { fadeOut(tween(350)) },
        ) {
            composable<Welcome> {
                WelcomeScreen(
                    state = state,
                    avatarRenderer = avatarRenderer,
                    reverseFromListening = reverseFromListening,
                    onLanguageClick = viewModel::showLanguageSheet,
                    onHelpClick = viewModel::showIntroSheet,
                    onMicTap = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                )
            }
            composable<Listening> {
                ListeningScreen(
                    question = state.question,
                    avatarRenderer = avatarRenderer,
                    onCancel = viewModel::cancelListening,
                )
            }
            composable<Speaking> {
                SpeakingScreen(
                    state = state,
                    avatarRenderer = avatarRenderer,
                    onShowTextAnswer = viewModel::showTextAnswer,
                    onStop = viewModel::stopConversation,
                )
            }
        }

        // Единый боттомшит поверх любого экрана разговора (см. SheetContent).
        val sheetTop = state.sheetTop
        if (sheetTop != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = viewModel::closeSheet,
                sheetState = sheetState,
                dragHandle = null, // в Figma нет ручки-полоски — она давала лишний отступ сверху
                // Явно = тому же фону, что у HtmlAnswerWebView, иначе шов между дефолтным
                // M3 surface-тоном шита и белым фоном html-контента.
                containerColor = LocalAppColors.current.surface,
            ) {
                SheetHostContent(
                    content = sheetTop,
                    canGoBack = state.sheet.size > 1,
                    languages = Language.entries,
                    selectedLanguage = state.selectedLanguage,
                    onSelectLanguage = { viewModel.selectLanguage(it) },
                    onLinkClick = { viewModel.openWebInSheet(it) },
                    onContinue = {
                        viewModel.closeSheet()
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onFinish = { viewModel.closeSheet(); viewModel.stopConversation() },
                    onBack = { viewModel.sheetBack() },
                    onClose = { viewModel.closeSheet() },
                )
            }
        }

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            snackbar = { data -> ErrorSnackbar(message = data.visuals.message) },
        )
    }
}
