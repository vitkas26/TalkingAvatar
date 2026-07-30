package kg.nurtelecom.o.talkingavatar.ui.conversation.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarState
import kg.nurtelecom.o.talkingavatar.ui.avatar.LottieGlow
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.APERTURE_TRANSITION_MS
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.ApertureVideoBox
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.BottomLogo
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.LISTENING_ZOOM
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.LOTTIE_GLOW_DIAMETER
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.LOTTIE_GLOW_OFFSET_X
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.LOTTIE_GLOW_OFFSET_Y
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.RAW_FACE_FRACTION
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.rememberStageGeometry
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme
import kotlin.math.roundToInt

// Listening — экран монтируется с ПОЛНЫМ прямоугольником (тем же кадром что и Welcome — nav-fade
// уже спрятал стык видео), и ТОЛЬКО ПОСЛЕ этого локально закрывается в круг вокруг лица
// (см. обсуждение: апертура — не переход между экранами, а entrance-анимация внутри экрана).
@Composable
fun ListeningScreen(question: String, avatarRenderer: AvatarRenderer, onCancel: () -> Unit) {
    val colors = LocalAppColors.current
    // Compose Preview (LocalInspectionMode) не умеет прогонять suspend-анимации — animateTo там
    // либо не запускается, либо валит рендер превью. В превью сразу показываем финальный кадр
    // (круг закрыт, текст виден), без анимации.
    val inPreview = LocalInspectionMode.current
    val progress = remember { Animatable(if (inPreview) 1f else 0f) }
    // «Слушает...» появляется ПОСЛЕ того как апертура закрылась в круг, не одновременно с ней.
    var apertureDone by remember { mutableStateOf(inPreview) }
    LaunchedEffect(Unit) {
        if (!inPreview) {
            progress.animateTo(1f, tween(APERTURE_TRANSITION_MS, easing = FastOutSlowInEasing))
            apertureDone = true
        }
    }
    val textAlpha by animateFloatAsState(if (apertureDone) 1f else 0f, tween(250), label = "listening-text")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.stageBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val density = LocalDensity.current
        val geometry = rememberStageGeometry(constraints.maxWidth.toFloat(), density)
        val stageTop = geometry.stageTopPx
        val stageH = geometry.stageHPx
        val circleR = geometry.circleRPx
        val cx = geometry.cxPx
        val listeningCy = geometry.listeningCyPx

        val p = progress.value
        // Панорама сцены вверх, чтобы при p=1 центр круга пришёл в listeningCy у верха экрана.
        // Тот же coerceIn, что и в ApertureShape, — чтобы при клампе (лицо у самого верха кадра)
        // glow и круг оставались концентричными.
        val circleCenterP1 = (RAW_FACE_FRACTION * LISTENING_ZOOM * stageH).coerceIn(circleR, stageH - circleR)
        val panY = lerp(0f, listeningCy - stageTop - circleCenterP1, p)

        val glowAlpha = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)

        // Glow ПОД видео-кругом (рисуется первым, чтобы кружок с лицом был поверх). Плюс
        // компенсация внутреннего смещения самого blob'а в lottie-файле (см. LOTTIE_GLOW_OFFSET_*).
        if (glowAlpha > 0f) {
            val glowR = with(density) { LOTTIE_GLOW_DIAMETER.toPx() } / 2f
            val glowOffsetX = with(density) { LOTTIE_GLOW_OFFSET_X.toPx() }
            val glowOffsetY = with(density) { LOTTIE_GLOW_OFFSET_Y.toPx() }
            LottieGlow(
                diameter = LOTTIE_GLOW_DIAMETER,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (cx - glowR + glowOffsetX).roundToInt(),
                            (listeningCy - glowR + glowOffsetY).roundToInt(),
                        )
                    }
                    .graphicsLayer { alpha = glowAlpha },
            )
        }

        ApertureVideoBox(geometry, p, AvatarState.Listening, avatarRenderer, panYPx = panY)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer { alpha = textAlpha }
                .offset {
                    IntOffset(
                        0,
                        (listeningCy + circleR + with(density) { 32.dp.toPx() }).roundToInt()
                    )
                }
                .padding(horizontal = 32.dp),
        ) {
            Text(
                text = "Слушает...",
                color = colors.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            // То, что распознал STT — появляется по мере поступления результата (question в
            // MainState сбрасывается на "" при каждом новом startListening, см. MainVM).
            if (question.isNotBlank()) {
                Text(
                    text = question,
                    color = colors.greetingText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Image(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(36.dp)
                .clickable(onClick = onCancel),
            painter = painterResource(R.drawable.ic_close), contentDescription = "Закрыть"
        )


        BottomLogo(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Preview(name = "Listening", showBackground = true, heightDp = 900)
@Composable
private fun ListeningScreenPreview() {
    TalkingAvatarTheme {
        ListeningScreen(
            question = "Как подключить безлимитный интернет?",
            avatarRenderer = PreviewAvatarRenderer,
            onCancel = {},
        )
    }
}
