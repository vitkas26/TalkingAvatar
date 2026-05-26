package kg.nurtelecom.o.talkingavatar.ui.avatar

interface AvatarRenderer {
    fun setMorphWeight(name: String, weight: Float)
    fun applyVisemeFrame(frame: VisemeFrame)
    fun setIdle()
}
