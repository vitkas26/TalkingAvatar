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
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AvatarSceneView(
    modifier: Modifier = Modifier,
    onRendererReady: ((AvatarRenderer) -> Unit)? = null
) {
    val context = LocalContext.current
    val sceneView = remember { SceneView(context) }

    LaunchedEffect(Unit) {
        val instance = sceneView.modelLoader.loadModelInstance("model.glb") ?: return@LaunchedEffect
        val modelNode = ModelNode(modelInstance = instance, autoAnimate = false, scaleToUnits = 2.2f).apply {
            position = Position(x = 0f, y = -1.8f, z = 0f)
        }
        sceneView.addChildNode(modelNode)

        val renderer = AvatarRendererImpl(sceneView.engine, instance)
        onRendererReady?.invoke(renderer)

        // Blink: random every 2.5–4.5 sec
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

        // Head sway: three incommensurable sine waves so motion never repeats exactly
        launch {
            var t = 0f
            while (true) {
                t += 0.05f
                modelNode.rotation = Rotation(
                    x = sin(t * 0.31f) * 1.0f,   // subtle nod  ±1°, ~20 s period
                    y = sin(t * 0.53f) * 3.0f,   // look left/right ±3°, ~12 s period
                    z = sin(t * 0.23f) * 1.5f    // head tilt ±1.5°, ~27 s period
                )
                delay(50L)
            }
        }

        // Eye gaze shifts: every 3–7 sec glance in a random direction
        val gazeNames = listOf(
            FacialBlendShape.EYE_LOOK_OUT_LEFT, FacialBlendShape.EYE_LOOK_IN_RIGHT,
            FacialBlendShape.EYE_LOOK_IN_LEFT,  FacialBlendShape.EYE_LOOK_OUT_RIGHT,
            FacialBlendShape.EYE_LOOK_UP_LEFT,  FacialBlendShape.EYE_LOOK_UP_RIGHT
        )
        launch {
            while (true) {
                delay(3000L + Random.nextLong(4000L))
                gazeNames.forEach { renderer.setMorphWeight(it, 0f) }

                val strength = Random.nextFloat() * 0.20f + 0.08f
                when (Random.nextInt(5)) {
                    0 -> {
                        renderer.setMorphWeight(FacialBlendShape.EYE_LOOK_OUT_LEFT, strength)
                        renderer.setMorphWeight(FacialBlendShape.EYE_LOOK_IN_RIGHT, strength)
                    }
                    1 -> {
                        renderer.setMorphWeight(FacialBlendShape.EYE_LOOK_IN_LEFT, strength)
                        renderer.setMorphWeight(FacialBlendShape.EYE_LOOK_OUT_RIGHT, strength)
                    }
                    2 -> {
                        renderer.setMorphWeight(FacialBlendShape.EYE_LOOK_UP_LEFT, strength * 0.5f)
                        renderer.setMorphWeight(FacialBlendShape.EYE_LOOK_UP_RIGHT, strength * 0.5f)
                    }
                    // 3, 4 → stay at center
                }

                delay(1000L + Random.nextLong(2000L))
                gazeNames.forEach { renderer.setMorphWeight(it, 0f) }
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
