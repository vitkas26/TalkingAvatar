package kg.nurtelecom.o.talkingavatar.ui.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val sceneView = remember {
        SceneView(context, isOpaque = false).apply { skybox = null }
    }

    LaunchedEffect(Unit) {
        val instance = sceneView.modelLoader.loadModelInstance("model_di.glb") ?: return@LaunchedEffect
        val renderer = AvatarRendererImpl(sceneView.engine, instance)

        val glbAnimator = instance.animator
        val animDuration = glbAnimator.getAnimationDuration(0)

        val modelNode = object : ModelNode(
            modelInstance = instance, autoAnimate = false, scaleToUnits = 3f
        ) {
            private var animTime = 0f
            private var lastFrameNs = -1L

            override fun onFrame(frameTimeNanos: Long) {
                if (lastFrameNs > 0) {
                    val dt = (frameTimeNanos - lastFrameNs) / 1_000_000_000f
                    animTime = (animTime + dt) % animDuration
                    glbAnimator.applyAnimation(0, animTime)
                    renderer.applyHeadOverride()
                    glbAnimator.updateBoneMatrices()
                }
                lastFrameNs = frameTimeNanos
                super.onFrame(frameTimeNanos)
                renderer.reapplyMorphOverrides()
            }
        }.apply {
            position = Position(x = 0f, y = -2.5f, z = -3f)
        }
        sceneView.addChildNode(modelNode)
        onRendererReady?.invoke(renderer)

        launch {
            while (true) {
                delay(3000L + Random.nextLong(2000L))
                for (i in 0..4) {
                    val w = i / 4f
                    renderer.setMorphWeight(FacialBlendShape.EYE_BLINK_LEFT, w)
                    renderer.setMorphWeight(FacialBlendShape.EYE_BLINK_RIGHT, w)
                    delay(40L)
                }
                for (i in 4 downTo 0) {
                    val w = i / 4f
                    renderer.setMorphWeight(FacialBlendShape.EYE_BLINK_LEFT, w)
                    renderer.setMorphWeight(FacialBlendShape.EYE_BLINK_RIGHT, w)
                    delay(40L)
                }
            }
        }

        launch {
            var t = 0f
            while (true) {
                t += 0.04f
                modelNode.rotation = Rotation(
                    x = sin(t * 0.31f) * 1.0f + renderer.leanDeg,
                    y = sin(t * 0.53f) * 3.0f,
                    z = sin(t * 0.23f) * 1.5f
                )
                delay(80L)
            }
        }

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
                }

                delay(1000L + Random.nextLong(2000L))
                gazeNames.forEach { renderer.setMorphWeight(it, 0f) }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { sceneView.destroy() }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF13111C),
                    Color(0xFF1D1A2F),
                    Color(0xFF0F3460),
                )
            )
        )
    ) {
        AndroidView(
            factory = { sceneView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
