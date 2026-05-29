package kg.nurtelecom.o.talkingavatar.ui.avatar

import android.opengl.Matrix
import com.google.android.filament.Engine
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.model.ModelInstance
import kotlinx.coroutines.delay

class AvatarRendererImpl(
    private val engine: Engine,
    modelInstance: ModelInstance
) : AvatarRenderer {

    private val morphTargetMap = mutableMapOf<String, MutableList<Pair<Int, Int>>>()
    private val morphOverrides = HashMap<String, Float>(32)

    var pauseAnimation: (() -> Unit)? = null
    var resumeAnimation: (() -> Unit)? = null

    companion object {
        private const val LEFT_ARM_ENTITY = 25
        private const val LEFT_FORE_ARM_ENTITY = 26
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
        val clamped = weight.coerceIn(0f, 1f)
        morphOverrides[name] = clamped
        applyMorphWeight(name, clamped)
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

    override suspend fun playEarListenGesture() {
        pauseAnimation?.invoke()
        delay(32L)
        val tm = engine.transformManager
        val armTi = tm.getInstance(LEFT_ARM_ENTITY)
        val foreTi = tm.getInstance(LEFT_FORE_ARM_ENTITY)

        val frozenArm = FloatArray(16).also { tm.getTransform(armTi, it) }
        val frozenFore = FloatArray(16).also { tm.getTransform(foreTi, it) }

        val rot = FloatArray(16)
        Matrix.setIdentityM(rot, 0)
        Matrix.rotateM(rot, 0, -80f, 1f, 0f, 0f)
        val earArm = FloatArray(16).also { Matrix.multiplyMM(it, 0, frozenArm, 0, rot, 0) }

        Matrix.setIdentityM(rot, 0)
        Matrix.rotateM(rot, 0, 90f, 0f, 1f, 0f)
        val earFore = FloatArray(16).also { Matrix.multiplyMM(it, 0, frozenFore, 0, rot, 0) }

        animateBones(armTi, foreTi, frozenArm, earArm, frozenFore, earFore, steps = 24, stepMs = 25L)
        delay(3000L)
        animateBones(armTi, foreTi, earArm, frozenArm, earFore, frozenFore, steps = 24, stepMs = 25L)

        resumeAnimation?.invoke()
    }

    private suspend fun animateBones(
        armTi: Int, foreTi: Int,
        fromArm: FloatArray, toArm: FloatArray,
        fromFore: FloatArray, toFore: FloatArray,
        steps: Int, stepMs: Long
    ) {
        val tm = engine.transformManager
        repeat(steps) { i ->
            val t = (i + 1).toFloat() / steps
            tm.setTransform(armTi, lerpMat(fromArm, toArm, t))
            tm.setTransform(foreTi, lerpMat(fromFore, toFore, t))
            delay(stepMs)
        }
    }

    private fun lerpMat(a: FloatArray, b: FloatArray, t: Float) =
        FloatArray(16) { i -> a[i] + (b[i] - a[i]) * t }
}
