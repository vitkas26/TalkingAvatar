package kg.nurtelecom.o.talkingavatar.statemachine

enum class AvatarState {
    Welcome,
    Idle,
    Listening,
    Processing,
    Speaking,
    WebViewMode,
    Error,
    EmergencyMode,
    ManualLanguageSelection,
}
