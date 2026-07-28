package kg.nurtelecom.o.talkingavatar.ui.conversation.chrome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.ui.theme.AppColors
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme

// Общий header Welcome/Speaking: язык слева, приветствие по центру, помощь справа. Одинаковый
// на обоих экранах (Listening его не показывает — там свой «Слушает...»).
@Composable
internal fun ConversationHeader(
    onLanguageClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: AppColors = LocalAppColors.current,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(
            iconRes = R.drawable.ic_language,
            contentDescription = "Выбрать язык",
            onClick = onLanguageClick,
        )
        Text(
            text = "Привет! Я — Nurai\nваш цифровой помощник.\nЗадайте мне вопрос!",
            color = colors.greetingText,
            fontFamily = FontFamily.Default,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        HeaderIconButton(
            iconRes = R.drawable.ic_question,
            contentDescription = "Помощь",
            onClick = onHelpClick,
        )
    }
}

@Composable
internal fun HeaderIconButton(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
    )
}

// Коробка размером/позицией видео-прямоугольника с нижним градиентом-затемнением (Welcome/
// Speaking — общий фон под mic-кнопкой/«Ответ в текстовом виде»), контент — то, что лежит
// поверх градиента снизу (свой на каждом экране).
@Composable
internal fun StageGradientBox(
    geometry: StageGeometry,
    colors: AppColors = LocalAppColors.current,
    content: @Composable BoxScope.() -> Unit,
) {
    StagePositionedBox(geometry) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.88f to Color.Transparent,
                        1f to colors.stageBackground,
                    ),
                ),
        )
        content()
    }
}

// NURAi-логотип внизу экрана — одинаков на всех трёх экранах разговора.
@Composable
internal fun BottomLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = "NURAi",
        modifier = modifier.padding(bottom = 40.dp),
    )
}

// Карточка ошибки поверх экрана (напр. "не смогла определить язык") — кремовый фон, ic_error
// слева, текст справа. Заменяет дефолтный Material Snackbar (см. SnackbarHost в ConversationNavHost).
@Composable
internal fun ErrorSnackbar(message: String, modifier: Modifier = Modifier, colors: AppColors = LocalAppColors.current) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(colors.warningBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_error),
            contentDescription = null,
            modifier = Modifier.padding(top = 2.dp).size(48.dp),
        )
        
        Text(
            text = message,
            color = Color.Black,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorSnackbarPreview() {
    TalkingAvatarTheme {
        ErrorSnackbar(
            message = "я не смогла определить язык.",
        )
    }
}
