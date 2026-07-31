package kg.nurtelecom.o.talkingavatar.ui.avatar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface AvatarRenderer {
    @Composable
    fun Render(state: AvatarState, modifier: Modifier = Modifier)
}
