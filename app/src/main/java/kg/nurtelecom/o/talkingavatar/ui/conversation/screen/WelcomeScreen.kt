package kg.nurtelecom.o.talkingavatar.ui.conversation.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarState
import kg.nurtelecom.o.talkingavatar.ui.conversation.MainState
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.APERTURE_TRANSITION_MS
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.ApertureVideoBox
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.BottomLogo
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.ConversationHeader
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.StageGradientBox
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.rememberStageGeometry
import kg.nurtelecom.o.talkingavatar.ui.conversation.sheet.SheetContent
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme

// Welcome — прямоугольное видео (машет рукой), header (язык/помощь), mic-кнопка.
// reverseFromListening=true при возврате из Listening: экран монтируется уже "кругом" (как
// выглядело Listening в момент nav-fade) и локально разворачивается rect<->circle обратно —
// апертура здесь не общий переход между экранами, а свой entrance-эффект (см. обсуждение:
// экраны отдельные, nav делает fade, апертура — локальная анимация внутри экрана).
@Composable
fun WelcomeScreen(
    state: MainState,
    avatarRenderer: AvatarRenderer,
    reverseFromListening: Boolean,
    onLanguageClick: () -> Unit,
    onHelpClick: () -> Unit,
    onMicTap: () -> Unit,
) {
    val colors = LocalAppColors.current
    // См. ListeningScreen — Compose Preview не прогоняет suspend-анимации, animateTo там ломает рендер.
    val inPreview = LocalInspectionMode.current
    val progress = remember { Animatable(if (reverseFromListening) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reverseFromListening && !inPreview) {
            progress.animateTo(0f, tween(APERTURE_TRANSITION_MS, easing = FastOutSlowInEasing))
        }
    }

    val avatarState = when {
        state.sheetTop is SheetContent.LanguagePicker || state.sheetTop is SheetContent.Intro -> AvatarState.Idle
        state.error != null -> AvatarState.Error
        else -> AvatarState.Welcome
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.stageBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val density = LocalDensity.current
        val geometry = rememberStageGeometry(constraints.maxWidth.toFloat(), density)

        ApertureVideoBox(geometry, progress.value, avatarState, avatarRenderer)

        ConversationHeader(
            onLanguageClick = onLanguageClick,
            onHelpClick = onHelpClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        StageGradientBox(geometry) {
            Image(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "Задать вопрос",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(110.dp)
                    .padding(bottom = 40.dp)
                    .clickable(enabled = !state.isPreparing, onClick = onMicTap),
            )
        }

        BottomLogo(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Preview(name = "Welcome", showBackground = true, heightDp = 900)
@Composable
private fun WelcomeScreenPreview() {
    TalkingAvatarTheme {
        WelcomeScreen(
            state = MainState(),
            avatarRenderer = PreviewAvatarRenderer,
            reverseFromListening = false,
            onLanguageClick = {},
            onHelpClick = {},
            onMicTap = {},
        )
    }
}
