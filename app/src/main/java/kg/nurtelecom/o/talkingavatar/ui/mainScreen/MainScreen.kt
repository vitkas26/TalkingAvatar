package kg.nurtelecom.o.talkingavatar.ui.mainScreen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.statemachine.AvatarState
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.LottieGlow
import kg.nurtelecom.o.talkingavatar.ui.theme.HomeGreetingGray
import kg.nurtelecom.o.talkingavatar.ui.theme.ListeningBackground
import kg.nurtelecom.o.talkingavatar.ui.theme.ListeningStatusPink
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import kotlin.math.roundToInt

// --- Геометрия перехода Greeting <-> Listening (aperture/iris) ---------------------------
// Эффект: видео стоит на месте в ПОСТОЯННОМ масштабе, круглая маска-вырез закрывается/
// раскрывается вокруг лица (см. дизайн-референс Full.mp4). Не shared-element (тот масштабирует
// контент — не тот эффект). Одна видео-поверхность рендерится один раз (не пересоздаётся),
// прогресс 0=прямоугольник (greeting) .. 1=круг (listening) двигает только clip-маску + панораму.
private val STAGE_TOP = 108.dp           // верх видео-прямоугольника (под хедером)
private val STAGE_H_PADDING = 16.dp      // горизонтальные поля прямоугольника
private const val GREETING_ASPECT = 2047f / 2742f
// Зум видео анимируется по progress: 1.0 в welcome (виден полный кадр — руки), 1.4 в listening
// (тесно голова+шея в круге). Top-anchor. Не чистый постоянный масштаб, но лёгкий зум-ин во
// время iris смотрится естественно.
private const val GREETING_ZOOM = 1.1f
private const val LISTENING_ZOOM = 1.0f
// Доля высоты СЫРОГО кадра, где центр лица (замерено по avatar_listening.mp4 ~0.22). Экранная
// доля = RAW_FACE_FRACTION * текущий зум (top-anchor). Тюнить.
private const val RAW_FACE_FRACTION = 0.22f
private val CIRCLE_DIAMETER = 220.dp
private val CIRCLE_TOP_PADDING = 96.dp   // отступ круга от верха в состоянии listening
private val GREETING_CORNER = 24.dp
private val LOTTIE_GLOW_DIAMETER = 460.dp
private const val TRANSITION_MS = 900

// Маска-вырез поверх видео-сцены (размер сцены = size в createOutline). progress 0 -> полный
// прямоугольник со скруглением cornerPx; 1 -> круг радиуса rPx с центром в точке лица
// (0.5*width, faceFraction*height). Клип применяется к full-size Box (видео его заполняет),
// поэтому корректно обрезает даже TextureView (проверенная схема, в отличие от клипа
// маленького Box с overflow-ребёнком).
private class ApertureShape(
    private val progress: Float,
    private val faceFraction: Float,
    private val cornerPx: Float,
    private val rPx: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val faceCx = size.width / 2f
        val faceCy = faceFraction * size.height
        val halfW = lerp(size.width / 2f, rPx, progress)
        val halfH = lerp(size.height / 2f, rPx, progress)
        // Центр маски не даём выйти за границы сцены — иначе часть круга оказывается выше
        // верхнего края видео и обрезается плоско (заметно при малом зуме, когда лицо у
        // самого верха кадра и радиус круга больше места над ним).
        val cx = lerp(size.width / 2f, faceCx, progress).coerceIn(halfW, size.width - halfW)
        val cy = lerp(size.height / 2f, faceCy, progress).coerceIn(halfH, size.height - halfH)
        val corner = lerp(cornerPx, rPx, progress)
        return Outline.Rounded(
            RoundRect(
                rect = Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH),
                cornerRadius = CornerRadius(corner, corner),
            ),
        )
    }
}

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
            if (state.isSpeaking) viewModel.stopAndRestart() else viewModel.startListening()
        } else scope.launch { snackBarHostState.showSnackbar("Требуется разрешение на микрофон") }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MainSideEffect.ShowError -> scope.launch { snackBarHostState.showSnackbar(sideEffect.message) }
        }
    }

    val avatarState = when {
        state.isLanguageSheetOpen -> AvatarState.Idle
        state.isListening || state.isPreparing -> AvatarState.Listening
        state.isSpeaking -> AvatarState.Speaking
        state.error != null -> AvatarState.Error
        state.showWelcome -> AvatarState.Welcome
        else -> AvatarState.Idle
    }

    val listening = avatarState == AvatarState.Listening
    val progress by animateFloatAsState(
        targetValue = if (listening) 1f else 0f,
        animationSpec = tween(TRANSITION_MS, easing = FastOutSlowInEasing),
        label = "aperture",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ListeningBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val density = LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        val stageLeft = with(density) { STAGE_H_PADDING.toPx() }
        val stageTop = with(density) { STAGE_TOP.toPx() }
        val stageW = wPx - stageLeft * 2f
        val stageH = stageW / GREETING_ASPECT
        val circleR = with(density) { CIRCLE_DIAMETER.toPx() } / 2f
        val cx = wPx / 2f
        val listeningCy = with(density) { CIRCLE_TOP_PADDING.toPx() } + circleR

        // Зум анимируется -> экранная доля лица тоже (top-anchor: faceScreenFrac = raw * zoom).
        val videoZoom = lerp(GREETING_ZOOM, LISTENING_ZOOM, progress)
        val faceFracNow = RAW_FACE_FRACTION * videoZoom
        // Панорама сцены вверх, чтобы при p=1 центр круга пришёл в listeningCy у верха экрана.
        // Тот же coerceIn, что и в ApertureShape, — чтобы при клампе (лицо у самого верха кадра)
        // glow и круг оставались концентричными.
        val circleCenterP1 = (RAW_FACE_FRACTION * LISTENING_ZOOM * stageH).coerceIn(circleR, stageH - circleR)
        val panY = lerp(0f, listeningCy - stageTop - circleCenterP1, progress)
        val cornerPx = lerp(with(density) { GREETING_CORNER.toPx() }, circleR, progress)

        val greetingAlpha = (1f - progress * 2.2f).coerceIn(0f, 1f)
        val listeningAlpha = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)

        // --- Glow ПОД видео-кругом (рисуется первым, чтобы кружок с лицом был поверх, а
        //     свечение выглядывало кольцом по краю) ---
        if (listeningAlpha > 0f) {
            val glowR = with(density) { LOTTIE_GLOW_DIAMETER.toPx() } / 2f
            LottieGlow(
                diameter = LOTTIE_GLOW_DIAMETER,
                modifier = Modifier
                    .offset { IntOffset((cx - glowR).roundToInt(), (listeningCy - glowR).roundToInt()) }
                    .graphicsLayer { alpha = listeningAlpha },
            )
        }

        // --- Постоянная видео-сцена (full-size), обрезаемая анимируемой маской-вырезом ---
        Box(
            modifier = Modifier
                .offset { IntOffset(stageLeft.roundToInt(), (stageTop + panY).roundToInt()) }
                .size(with(density) { stageW.toDp() }, with(density) { stageH.toDp() })
                .clip(ApertureShape(progress, faceFracNow, cornerPx, circleR)),
        ) {
            avatarRenderer.Render(
                avatarState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = videoZoom
                        scaleY = videoZoom
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    },
            )
        }

        // --- Greeting-обвязка (гаснет в первые ~45% перехода) ---
        if (greetingAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = greetingAlpha }) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderIconButton(
                        iconRes = R.drawable.ic_language,
                        contentDescription = "Выбрать язык",
                        onClick = { viewModel.showLanguageSheet() },
                    )
                    Text(
                        text = "Привет! Я — Nurai\nваш цифровой помощник.\nЗадайте мне вопрос!",
                        color = HomeGreetingGray,
                        fontFamily = FontFamily.Default,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 16.sp,
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    HeaderIconButton(
                        iconRes = R.drawable.ic_question,
                        contentDescription = "Помощь",
                        onClick = { scope.launch { snackBarHostState.showSnackbar("Раздел в разработке") } },
                    )
                }

                // Коробка размером/позицией видео-прямоугольника: нижний градиент + mic-кнопка.
                Box(
                    modifier = Modifier
                        .offset { IntOffset(stageLeft.roundToInt(), stageTop.roundToInt()) }
                        .size(with(density) { stageW.toDp() }, with(density) { stageH.toDp() }),
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.88f to Color.Transparent,
                                    1f to ListeningBackground,
                                ),
                            ),
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = if (state.isSpeaking) "Остановить" else "Задать вопрос",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(110.dp)
                            .padding(bottom = 40.dp)
                            .clickable(enabled = !state.isPreparing) {
                                if (state.isSpeaking) {
                                    viewModel.stopSpeaking()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                    )
                }
            }
        }

        // --- Listening-обвязка поверх круга (текст + крестик; glow — отдельно, ниже по z) ---
        if (listeningAlpha > 0f) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = listeningAlpha }) {
                Text(
                    text = "Слушает...",
                    color = ListeningStatusPink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .offset { IntOffset(0, (listeningCy + circleR + with(density) { 32.dp.toPx() }).roundToInt()) },
                )
                IconButton(
                    onClick = { viewModel.cancelListening() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 24.dp)
                        .size(48.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Закрыть",
                    )
                }
            }
        }

        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "NURAi",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        )

        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
        )
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

@Composable
private fun HeaderIconButton(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(56.dp)
            .clickable(onClick = onClick),
    )
}
