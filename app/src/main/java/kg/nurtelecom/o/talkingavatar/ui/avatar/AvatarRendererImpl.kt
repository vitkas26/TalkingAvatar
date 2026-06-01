package kg.nurtelecom.o.talkingavatar.ui.avatar

import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.model.ModelInstance

class AvatarRendererImpl(
    private val engine: Engine,
    modelInstance: ModelInstance
) : AvatarRenderer {

    private val morphTargetMap = mutableMapOf<String, MutableList<Pair<Int, Int>>>()
    private val morphOverrides = HashMap<String, Float>(32)

    @Volatile var isListening: Boolean = false
        private set

    @Volatile var leanDeg: Float = 0f
        private set
    private var leanTarget = 0f

    override fun setListening(active: Boolean) {
        isListening = active
    }

    private fun lerpListeningState() {
        leanTarget = if (isListening) 25f else 0f
        val prev = leanDeg
        val next = prev + (leanTarget - prev) * 0.06f
        leanDeg = if (!isListening && next < 0.1f) 0f else next
    }

    fun applyHeadOverride() {
        lerpListeningState()
        // lean is applied via modelNode.rotation in AvatarSceneView — nothing to do here
    }

    companion object {
        private const val HEAD_ENTITY = 21
        private const val NECK_ENTITY = 20
    }

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
            names.forEachIndexed { index, name ->
                if (index < morphCount) {
                    morphTargetMap.getOrPut(name) { mutableListOf() }.add(entity to index)
                }
            }
        }
    }

    override fun setMorphWeight(name: String, weight: Float) {
        morphOverrides[name] = weight.coerceIn(0f, 1f)
    }

    private fun applyMorphWeight(name: String, weight: Float) {
        val locations = morphTargetMap[name] ?: return
        val rm = engine.renderableManager
        locations.forEach { (entity, index) ->
            val inst = rm.getInstance(entity)
            if (inst != 0) rm.setMorphWeights(inst, floatArrayOf(weight), index)
        }
    }

    fun reapplyMorphOverrides() {
        morphOverrides.forEach { (name, weight) -> applyMorphWeight(name, weight) }
    }

    override fun applyVisemeFrame(frame: VisemeFrame) {
        frame.weights.forEach { (viseme, weight) ->
            setMorphWeight(viseme.morphTargetName, weight)
        }
    }

    override fun setLipSyncAmplitude(amplitude: Float) {
        setMorphWeight(Viseme.AA.morphTargetName, amplitude * 0.6f)
        setMorphWeight(FacialBlendShape.JAW_OPEN, amplitude * 0.35f)
        setMorphWeight(FacialBlendShape.MOUTH_OPEN, amplitude * 0.25f)
        setMorphWeight(FacialBlendShape.MOUTH_SHRUG_LOWER, amplitude * 0.15f)
    }

    override fun setIdle() {
        Viseme.entries.forEach { setMorphWeight(it.morphTargetName, 0f) }
        setMorphWeight(FacialBlendShape.JAW_OPEN, 0f)
        setMorphWeight(FacialBlendShape.MOUTH_OPEN, 0f)
        setMorphWeight(FacialBlendShape.MOUTH_SHRUG_LOWER, 0f)
    }

}
