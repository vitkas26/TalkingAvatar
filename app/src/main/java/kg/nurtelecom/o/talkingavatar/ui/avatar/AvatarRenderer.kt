package kg.nurtelecom.o.talkingavatar.ui.avatar

import androidx.compose.runtime.Composable
import kg.nurtelecom.o.talkingavatar.statemachine.AvatarState

interface AvatarRenderer {
    @Composable
    fun Render(state: AvatarState)
}
