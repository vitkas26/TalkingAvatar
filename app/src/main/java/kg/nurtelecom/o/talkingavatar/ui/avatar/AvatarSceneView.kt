package kg.nurtelecom.o.talkingavatar.ui.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// testAnimation — временный параметр для Этапа 2; убрать после проверки блендшейпов
@Composable
fun AvatarSceneView(
    modifier: Modifier = Modifier,
    onRendererReady: ((AvatarRenderer) -> Unit)? = null,
    testAnimation: Boolean = true
) {
    val context = LocalContext.current
    val sceneView = remember { SceneView(context) }

    LaunchedEffect(Unit) {
        val instance = sceneView.modelLoader.loadModelInstance("model.glb") ?: return@LaunchedEffect
        sceneView.addChildNode(
            ModelNode(modelInstance = instance, scaleToUnits = 1.8f).apply {
                position = Position(x = 0f, y = -0.9f, z = -2.0f)
            }
        )

        val renderer = AvatarRendererImpl(sceneView.engine, instance)
        onRendererReady?.invoke(renderer)

        if (!testAnimation) return@LaunchedEffect

        // Jaw: плавное открытие/закрытие по синусоиде ~4 раза в секунду
        launch {
            while (true) {
                val time = System.currentTimeMillis() / 1000.0
                val jaw = ((sin(time * 4.0) + 1.0) / 2.0).toFloat() * 0.6f
                renderer.setMorphWeight(FacialBlendShape.JAW_OPEN, jaw)
                delay(16L)
            }
        }

        // Blink: случайное моргание каждые 2.5–4.5 секунды
        launch {
            while (true) {
                delay(2500L + Random.nextLong(2000L))
                for (i in 0..4) {
                    val w = i / 4f
                    renderer.setMorphWeight(FacialBlendShape.EYE_BLINK_LEFT, w)
                    renderer.setMorphWeight(FacialBlendShape.EYE_BLINK_RIGHT, w)
                    delay(25L)
                }
                for (i in 4 downTo 0) {
                    val w = i / 4f
                    renderer.setMorphWeight(FacialBlendShape.EYE_BLINK_LEFT, w)
                    renderer.setMorphWeight(FacialBlendShape.EYE_BLINK_RIGHT, w)
                    delay(25L)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { sceneView.destroy() }
    }

    AndroidView(
        factory = { sceneView },
        modifier = modifier
    )
}
