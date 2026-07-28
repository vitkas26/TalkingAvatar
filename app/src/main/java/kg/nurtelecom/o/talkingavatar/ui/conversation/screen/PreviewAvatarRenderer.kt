package kg.nurtelecom.o.talkingavatar.ui.conversation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarState

// Только для @Preview (см. низ Welcome/Listening/Speaking Screen.kt): реальный AvatarRenderer
// тянет ExoPlayer/Context из Koin, недоступных в превью. Красит фон под конкретный AvatarState —
// видно смену состояния прямо в превью, без запуска на устройстве.
internal object PreviewAvatarRenderer : AvatarRenderer {
    @Composable
    override fun Render(state: AvatarState, modifier: Modifier) {
        val color = when (state) {
            AvatarState.Welcome -> Color(0xFFB0A6C8)
            AvatarState.Listening -> Color(0xFF8FB8D8)
            AvatarState.Speaking -> Color(0xFFA6C8A6)
            AvatarState.Idle -> Color(0xFFD0D0D0)
            AvatarState.Error -> Color(0xFFD8A6A6)
            AvatarState.WebViewMode, AvatarState.EmergencyMode, AvatarState.ManualLanguageSelection -> Color.Gray
        }
        Box(modifier.background(color), contentAlignment = Alignment.Center) {
            Text(state.name)
        }
    }
}
