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
import kg.nurtelecom.o.talkingavatar.ui.conversation.MainViewModel
import kg.nurtelecom.o.talkingavatar.ui.settings.SettingsScreen
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    // Тот же экземпляр, что резолвит koinViewModel<MainViewModel>() внутри ConversationNavHost
    // (общий ViewModelStore Activity) — здесь нужен вне Compose, для onUserInteraction().
    private val mainViewModel: MainViewModel by viewModel()

    // Фреймворк зовёт это на КАЖДОМ touch/key событии, дошедшем до Activity, ещё до View-дерева —
    // тот же приём, которым PowerManager сам ловит активность для экрана. Двигает вперёд
    // 5-минутное ожидание после ответа (см. MainVM.markUserActive), больше ничего не делает.
    override fun onUserInteraction() {
        super.onUserInteraction()
        mainViewModel.markUserActive()
    }

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