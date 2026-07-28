package kg.nurtelecom.o.talkingavatar

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kg.nurtelecom.o.talkingavatar.ui.conversation.ConversationNavHost
import kg.nurtelecom.o.talkingavatar.ui.settings.SettingsScreen
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            TalkingAvatarTheme {
                // Debug-экран настроек для пилота показывается первым — см. SettingsScreen.
                var showSettings by remember { mutableStateOf(true) }
                if (showSettings) {
                    SettingsScreen(onStart = { showSettings = false })
                } else {
                    ConversationNavHost()
                }
            }
        }
    }
}