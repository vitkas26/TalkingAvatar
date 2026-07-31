package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kg.nurtelecom.o.talkingavatar.domain.model.Language
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme

// Знакомство (Figma frame «Знакомство»): заголовок + описание + список 6 языков + «Понятно».
@Composable
fun IntroContent(languages: List<Language>, onOk: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            "Знакомьтесь, это NURAi — ваш цифровой помощник!",
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Проконсультирует по тарифам, eSIM и услугам без очереди. Просто поговорите с ней!\n\n" +
                    "Можете просто начать говорить, на нужном вам языке, она распознает под какой язык подстроится.",
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text("Nurai понимает 6 языков:", fontSize = 16.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            languages.joinToString("\n") { "• ${it.displayName}" },
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(120.dp))
        PrimaryPillButton("Понятно", onClick = onOk)
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(name = "Sheet — Intro", showBackground = true, heightDp = 900)
@Composable
private fun SheetIntroPreview() {
    TalkingAvatarTheme {
        SheetHostContent(
            content = SheetContent.Intro,
            canGoBack = false,
            languages = Language.entries,
            selectedLanguage = Language.Russian,
            onSelectLanguage = {},
            onLinkClick = {},
            onContinue = {},
            onFinish = {},
            onBack = {},
            onClose = {},
        )
    }
}
