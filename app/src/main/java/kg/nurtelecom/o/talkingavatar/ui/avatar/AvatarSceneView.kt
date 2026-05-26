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
        sceneView.addChildNode(
            ModelNode(modelInstance = instance, autoAnimate = false, scaleToUnits = 1.8f).apply {
                position = Position(x = 0f, y = -0.9f, z = -2.0f)
            }
        )

        val renderer = AvatarRendererImpl(sceneView.engine, instance)
        onRendererReady?.invoke(renderer)

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
