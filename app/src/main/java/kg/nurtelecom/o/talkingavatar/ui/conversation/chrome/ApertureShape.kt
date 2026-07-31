package kg.nurtelecom.o.talkingavatar.ui.conversation.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarState
import kotlin.math.roundToInt

// --- Геометрия видео-сцены Welcome/Listening/Speaking ------------------------------------
// Общая для всех трёх экранов: одинаковый прямоугольник (позиция/aspect/скругление), чтобы
// nav-fade между ними не дёргал раскладку. Aperture (rect<->circle) теперь ЛОКАЛЬНАЯ анимация
// входа на конкретном экране (Listening при монтировании закрывается в круг, Welcome при
// возврате из Listening разворачивается обратно) — не общий прогресс на весь переход экрана.
internal val STAGE_TOP = 108.dp
internal val STAGE_H_PADDING = 16.dp
internal const val GREETING_ASPECT = 2047f / 2742f
// Зум видео анимируется по progress: 1.0 в welcome (виден полный кадр — руки), 1.4 в listening
// (тесно голова+шея в круге). Top-anchor. Не чистый постоянный масштаб, но лёгкий зум-ин во
// время iris смотрится естественно.
internal const val GREETING_ZOOM = 1.1f
internal const val LISTENING_ZOOM = 1.0f
// Доля высоты СЫРОГО кадра, где центр лица (замерено по avatar_listening.mp4 ~0.22). Экранная
// доля = RAW_FACE_FRACTION * текущий зум (top-anchor). Тюнить.
internal const val RAW_FACE_FRACTION = 0.22f
internal val CIRCLE_DIAMETER = 220.dp
internal val CIRCLE_TOP_PADDING = 96.dp // отступ круга от верха в состоянии listening
internal val GREETING_CORNER = 24.dp
internal val LOTTIE_GLOW_DIAMETER = 460.dp
// Сам glow-blob внутри anim_voice.json смещён относительно центра своего канваса (проверено
// debug-маркером: маска/круг центрируются в коде математически верно, а видимый blob — нет).
// Компенсация в dp, тюнить по месту если поменяют lottie-файл.
internal val LOTTIE_GLOW_OFFSET_X = 32.dp
internal val LOTTIE_GLOW_OFFSET_Y = 4.dp
internal const val APERTURE_TRANSITION_MS = 700

// Маска-вырез поверх видео-сцены (размер сцены = size в createOutline). progress 0 -> полный
// прямоугольник со скруглением cornerPx; 1 -> круг радиуса rPx с центром в точке лица
// (0.5*width, faceFraction*height). Клип применяется к full-size Box (видео его заполняет),
// поэтому корректно обрезает даже TextureView (проверенная схема, в отличие от клипа
// маленького Box с overflow-ребёнком).
internal class ApertureShape(
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

// Раскладка сцены в px — считается один раз из ширины экрана, переиспользуется Welcome/
// Listening/Speaking, чтобы прямоугольник не гулял между экранами (nav-fade это скрыл бы криво).
internal data class StageGeometry(
    val stageLeftPx: Float,
    val stageTopPx: Float,
    val stageWPx: Float,
    val stageHPx: Float,
    val circleRPx: Float,
    val cxPx: Float,
    val listeningCyPx: Float,
)

@Composable
internal fun rememberStageGeometry(maxWidthPx: Float, density: Density): StageGeometry =
    remember(maxWidthPx, density) {
        with(density) {
            val stageLeft = STAGE_H_PADDING.toPx()
            val stageW = maxWidthPx - stageLeft * 2f
            val circleR = CIRCLE_DIAMETER.toPx() / 2f
            StageGeometry(
                stageLeftPx = stageLeft,
                stageTopPx = STAGE_TOP.toPx(),
                stageWPx = stageW,
                stageHPx = stageW / GREETING_ASPECT,
                circleRPx = circleR,
                cxPx = maxWidthPx / 2f,
                listeningCyPx = CIRCLE_TOP_PADDING.toPx() + circleR,
            )
        }
    }

// Box, позиционированный/размеренный под прямоугольник сцены (без клипа/маски) — общая
// «рамка» для всего что должно лежать поверх видео тем же прямоугольником (напр. градиент
// затемнения снизу, см. StageGradientBox в ConversationChrome.kt).
@Composable
internal fun StagePositionedBox(
    geometry: StageGeometry,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .offset { IntOffset(geometry.stageLeftPx.roundToInt(), geometry.stageTopPx.roundToInt()) }
            .size(with(density) { geometry.stageWPx.toDp() }, with(density) { geometry.stageHPx.toDp() }),
        content = content,
    )
}

// Видео-прямоугольник + анимируемая apertura-маска + зум. Общий кусок для Welcome (progress
// 0 или анимируется 1->0 при возврате) и Listening (анимируется 0->1 на входе) — раньше был
// скопирован в оба экрана целиком.
@Composable
internal fun ApertureVideoBox(
    geometry: StageGeometry,
    progress: Float,
    avatarState: AvatarState,
    avatarRenderer: AvatarRenderer,
    panYPx: Float = 0f,
) {
    val density = LocalDensity.current
    val videoZoom = lerp(GREETING_ZOOM, LISTENING_ZOOM, progress)
    val faceFracNow = RAW_FACE_FRACTION * videoZoom
    val cornerPx = lerp(with(density) { GREETING_CORNER.toPx() }, geometry.circleRPx, progress)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(geometry.stageLeftPx.roundToInt(), (geometry.stageTopPx + panYPx).roundToInt())
            }
            .size(with(density) { geometry.stageWPx.toDp() }, with(density) { geometry.stageHPx.toDp() })
            .clip(ApertureShape(progress, faceFracNow, cornerPx, geometry.circleRPx)),
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
}
