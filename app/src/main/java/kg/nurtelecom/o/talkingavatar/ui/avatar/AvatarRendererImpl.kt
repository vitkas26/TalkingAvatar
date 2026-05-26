package kg.nurtelecom.o.talkingavatar.ui.avatar

import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.model.ModelInstance

class AvatarRendererImpl(
    private val engine: Engine,
    modelInstance: ModelInstance
) : AvatarRenderer {

    // name → list of (entity, morphTargetIndex in that entity)
    // One blend shape can exist in multiple meshes (e.g. visemes in Head and Teeth),
    // and each mesh has its own index — must apply to ALL meshes to avoid "horror smile".
    private val morphTargetMap = mutableMapOf<String, MutableList<Pair<Int, Int>>>()

    init {
        cacheMorphTargetIndices(modelInstance.asset)
    }

    private fun cacheMorphTargetIndices(asset: FilamentAsset) {
        morphTargetMap.clear()
        val renderableManager = engine.renderableManager

        asset.entities.forEach { entity ->
            val renderableInstance = renderableManager.getInstance(entity)
            if (renderableInstance == 0) return@forEach

            val morphCount = renderableManager.getMorphTargetCount(renderableInstance)
            if (morphCount == 0) return@forEach

            val names = asset.getMorphTargetNames(entity)
            Log.d("AvatarRenderer", "Entity $entity: GPU morphCount=$morphCount, GLTF nameCount=${names.size}")

            names.forEachIndexed { index, name ->
                // Применяем только если индекс входит в реальный GPU-буфер
                if (index < morphCount) {
                    morphTargetMap.getOrPut(name) { mutableListOf() }.add(entity to index)
                }
            }
        }

        Log.d("AvatarRenderer", "Cached ${morphTargetMap.size} morph targets: ${morphTargetMap.keys.sorted()}")
        Log.d("AvatarRenderer", "jawOpen → ${morphTargetMap["jawOpen"]}")
        Log.d("AvatarRenderer", "viseme_aa → ${morphTargetMap["viseme_aa"]}")
        Log.d("AvatarRenderer", "eyeBlinkLeft → ${morphTargetMap["eyeBlinkLeft"]}")
        Viseme.entries.forEach { v ->
            if (!morphTargetMap.containsKey(v.morphTargetName)) {
                Log.w("AvatarRenderer", "Missing viseme: ${v.morphTargetName}")
            }
        }
    }

    override fun setMorphWeight(name: String, weight: Float) {
        val locations = morphTargetMap[name] ?: return
        val renderableManager = engine.renderableManager
        val clamped = weight.coerceIn(0f, 1f)

        locations.forEach { (entity, index) ->
            val instance = renderableManager.getInstance(entity)
            if (instance != 0) {
                renderableManager.setMorphWeights(instance, floatArrayOf(clamped), index)
            }
        }
    }

    override fun applyVisemeFrame(frame: VisemeFrame) {
        frame.weights.forEach { (viseme, weight) ->
            setMorphWeight(viseme.morphTargetName, weight)
        }
    }

    override fun setIdle() {
        Viseme.entries.forEach { setMorphWeight(it.morphTargetName, 0f) }
        setMorphWeight(FacialBlendShape.JAW_OPEN, 0f)
    }
}
