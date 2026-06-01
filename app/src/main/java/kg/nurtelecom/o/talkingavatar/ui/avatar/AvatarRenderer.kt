package kg.nurtelecom.o.talkingavatar.ui.avatar

interface AvatarRenderer {
    fun setMorphWeight(name: String, weight: Float)
    fun applyVisemeFrame(frame: VisemeFrame)
    fun setLipSyncAmplitude(amplitude: Float)
    fun setIdle()
    fun setListening(active: Boolean)
}
