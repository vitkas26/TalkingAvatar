package kg.nurtelecom.o.talkingavatar.statemachine

enum class AvatarState {
    Welcome,
    Idle,
    Listening,
    Speaking,
    WebViewMode,
    Error,
    EmergencyMode,
    ManualLanguageSelection,
}
