package kg.nurtelecom.o.talkingavatar.ui.conversation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarState
import kg.nurtelecom.o.talkingavatar.ui.conversation.MainState
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.ApertureVideoBox
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.BottomLogo
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.StageGradientBox
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.rememberStageGeometry
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme

// Speaking — тот же прямоугольник что и Welcome (тот же STAGE_TOP/aspect/corner — nav-fade между
// ними не дёргает раскладку), без апертуры (круг здесь не нужен). Верхняя строка по Figma:
// белая пилюля «Ответ в текстовом виде >» слева, круглая кнопка закрытия справа — обе с тенью.
@Composable
fun SpeakingScreen(
    state: MainState,
    avatarRenderer: AvatarRenderer,
    onShowTextAnswer: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = LocalAppColors.current
    Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)){
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.stageBackground)
        ) {
            val density = LocalDensity.current
            val geometry = rememberStageGeometry(constraints.maxWidth.toFloat(), density)

            // Экран не покидаем сразу после TTS (см. MainVM idle-таймер — 5 мин ждём продолжения),
            // но видео должно перестать быть "говорящим": озвучка идёт — Speaking, договорил — Idle.
            val avatarState = if (state.isSpeaking) AvatarState.Speaking else AvatarState.Idle
            ApertureVideoBox(geometry, progress = 0f, avatarState, avatarRenderer)

            StageGradientBox(geometry) {}


            BottomLogo(modifier = Modifier.align(Alignment.BottomCenter))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextAnswerPillButton(onClick = onShowTextAnswer, enabled = state.answer != null)
            Spacer(modifier = Modifier.weight(1f))
            CloseCircleButton(onClick = onStop)
        }
    }
}

@Composable
private fun TextAnswerPillButton(onClick: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .shadow(2.dp, CircleShape, ambientColor = Color.Gray, spotColor = Color.White)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("Ответ в текстовом виде", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Normal)
        Spacer(Modifier.padding(horizontal = 2.dp))
        Icon(painter = painterResource(R.drawable.ic_chevron_right), null, modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun CloseCircleButton(onClick: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.ic_close), contentDescription = "Закрыть",
        modifier = Modifier
            .size(36.dp)
            .shadow(2.dp, CircleShape, ambientColor = Color.Gray, spotColor = Color.White)
            .clickable { onClick() }
    )
}

@Preview(name = "Speaking", showBackground = true, heightDp = 900)
@Composable
private fun SpeakingScreenPreview() {
    TalkingAvatarTheme {
        SpeakingScreen(
            state = MainState(),
            avatarRenderer = PreviewAvatarRenderer,
            onShowTextAnswer = {},
            onStop = {},
        )
    }
}
