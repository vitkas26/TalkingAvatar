package kg.nurtelecom.o.talkingavatar.ui.avatar

enum class Viseme(val morphTargetName: String, val oculusIndex: Int) {
    SIL("viseme_sil", 0),
    PP("viseme_PP", 1),
    FF("viseme_FF", 2),
    TH("viseme_TH", 3),
    DD("viseme_DD", 4),
    KK("viseme_kk", 5),
    CH("viseme_CH", 6),
    SS("viseme_SS", 7),
    NN("viseme_nn", 8),
    RR("viseme_RR", 9),
    AA("viseme_aa", 10),
    E("viseme_E", 11),
    IH("viseme_I", 12),
    OH("viseme_O", 13),
    OU("viseme_U", 14);

    companion object {
        private val byIndex = entries.associateBy { it.oculusIndex }
        fun fromOculusIndex(index: Int): Viseme? = byIndex[index]
    }
}
