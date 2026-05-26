package kg.nurtelecom.o.talkingavatar.ui.avatar

import android.util.Log
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
import kotlinx.coroutines.coroutineScope
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
            ModelNode(modelInstance = instance, autoAnimate = false, scaleToUnits = 1.8f).apply {
                position = Position(x = 0f, y = -0.9f, z = -2.0f)
            }
        )

        val renderer = AvatarRendererImpl(sceneView.engine, instance)
        onRendererReady?.invoke(renderer)

        if (!testAnimation) return@LaunchedEffect

        // coroutineScope держит оба дочерних launch живыми вместе с LaunchedEffect
        coroutineScope {
            // Статичный тест: замораживаем рот полностью открытым на 5 сек, потом закрываем
            launch {
                try {
                    // Применяем на ВСЕ entity — не только entity 66
                    renderer.setMorphWeight(Viseme.AA.morphTargetName, 1.0f)
                    renderer.setMorphWeight(FacialBlendShape.JAW_OPEN, 1.0f)
                    renderer.setMorphWeight(FacialBlendShape.MOUTH_OPEN, 1.0f)
                    renderer.setMorphWeight(FacialBlendShape.MOUTH_SHRUG_LOWER, 1.0f)
                    Log.d("AvatarRenderer", "Morph weights set to 1.0f — check if mouth is open")
                    delay(5000L)
                    renderer.setMorphWeight(Viseme.AA.morphTargetName, 0f)
                    renderer.setMorphWeight(FacialBlendShape.JAW_OPEN, 0f)
                    renderer.setMorphWeight(FacialBlendShape.MOUTH_OPEN, 0f)
                    renderer.setMorphWeight(FacialBlendShape.MOUTH_SHRUG_LOWER, 0f)
                    Log.d("AvatarRenderer", "Morph weights reset to 0f — check if mouth closed")
                } catch (e: Exception) {
                    Log.e("AvatarRenderer", "Mouth animation error", e)
                }
            }

            // Моргание: случайное каждые 2.5–4.5 сек
            launch {
                try {
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
                } catch (e: Exception) {
                    Log.e("AvatarRenderer", "Blink animation error", e)
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
