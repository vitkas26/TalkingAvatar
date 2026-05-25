package kg.nurtelecom.o.talkingavatar.ui.avatar

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

@Composable
fun AvatarSceneView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sceneView = remember { buildSceneView(context) }

    DisposableEffect(Unit) {
        onDispose { sceneView.destroy() }
    }

    AndroidView(
        factory = { sceneView },
        modifier = modifier
    )
}

private fun buildSceneView(context: Context): SceneView = SceneView(context).apply {
    modelLoader.loadModelInstance("avatar.glb") { instance ->
        if (instance != null) {
            addChildNode(
                ModelNode(modelInstance = instance, scaleToUnits = 1.8f).apply {
                    // Модель стоит ногами в origin; смещаем вниз, чтобы верхняя часть тела была в центре
                    position = Position(x = 0f, y = -0.9f, z = -2.0f)
                }
            )
        }
    }
}
