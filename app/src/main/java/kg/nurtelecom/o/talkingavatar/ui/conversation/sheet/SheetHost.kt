package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.domain.model.Language
import kg.nurtelecom.o.talkingavatar.ui.conversation.chrome.BottomLogo
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme

// Контент единого боттомшита. Один хост рендерит все состояния (см. SheetContent) через
// AnimatedContent; back/close сверху; BackHandler для системной «назад» внутри шита.
// Сам контент каждого состояния — в своём файле (IntroSheet.kt, LanguagePickerSheet.kt,
// AnswerText.kt, SheetWebView.kt); здесь только шапка/кнопки/диспетчер.
@Composable
fun SheetHostContent(
    content: SheetContent,
    canGoBack: Boolean,
    languages: List<Language>,
    selectedLanguage: Language,
    onSelectLanguage: (Language) -> Unit,
    onLinkClick: (String) -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // BackHandler требует OnBackPressedDispatcherOwner, которого нет в Compose Preview — падает.
    if (!LocalInspectionMode.current) {
        BackHandler(enabled = true) { onBack() }
    }

    // Answer/Web (сам ответ и открытая по ссылке страница) — во весь экран, кнопки
    // Завершить/Продолжить закреплены сверху над контентом, логотип снизу (см. скрин дизайна).
    // Intro/LanguagePicker — по размеру контента, как и раньше, без кнопок/лого.
    val isConversationContent = content is SheetContent.Answer || content is SheetContent.Web

    Column(
        modifier = modifier
            .fillMaxWidth()
            // fillMaxHeight() (без доли) тянет шит вплотную к самому верху экрана — скруглённые
            // "ушки" ModalBottomSheet упираются в статус-бар и визуально пропадают. 0.94f
            // оставляет зазор сверху, чтобы скругление было видно.
            .then(if (isConversationContent) Modifier.fillMaxHeight(0.94f) else Modifier)
            .padding(bottom = 16.dp),
    ) {
        // Шапка: назад (если есть куда) или закрыть. Явный размер IconButton (вместо
        // дефолтного) + минимальный top-паддинг — крестик должен сидеть точно в углу шита,
        // не проваливаться под drag-handle отступ (см. dragHandle = null у ModalBottomSheet).
        // Крестик — везде, КРОМЕ Ответа (Answer/Web): там закрытие идёт через «Завершить
        // разговор», а X снаружи — от Speaking-экрана под шитом (см. дизайн-референс).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            if (!isConversationContent) {
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Закрыть",
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            2.dp,
                            RoundedCornerShape(50.dp),
                            ambientColor = Color.White,
                            spotColor = Color.Gray
                        )
                        .clickable { onClose() },
                )
            }
        }

        if (isConversationContent) {
            ConversationButtonsRow(onFinish = onFinish, onContinue = onContinue)
        }

        AnimatedContent(
            targetState = content,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "sheet-content",
        ) { c ->
            when (c) {
                SheetContent.Intro -> IntroContent(languages, onClose)
                SheetContent.LanguagePicker -> LanguagePickerContent(
                    languages,
                    selectedLanguage,
                    onSelectLanguage
                )

                is SheetContent.Answer -> HtmlAnswerText(
                    html = c.html,
                    onLinkClick = onLinkClick,
                    modifier = Modifier.fillMaxSize(),
                )

                is SheetContent.Web -> UrlWebView(url = c.url, modifier = Modifier.fillMaxSize())
            }
        }

        if (isConversationContent) {
            BottomLogo(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

// Завершить/Продолжить разговор — над контентом Ответа (текст или открытая по ссылке
// страница), не под ним: страница может быть длинной, кнопки должны быть видны сразу.
@Composable
private fun ConversationButtonsRow(onFinish: () -> Unit, onContinue: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onFinish,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            // box-shadow: 0px 17px 100px 0px #6B76E029 (Figma) — Modifier.dropShadow, не
            // classic .shadow(elevation) — тот только имитирует Material-тень, не поддерживает
            // произвольные offset/blur/spread из дизайна.
            modifier = Modifier
                .weight(1f)
                .dropShadow(CircleShape) {
                    color = Color(0x296B76E0)
                    radius = 100f
                    spread = 0f
                    offset = Offset(0f, 17.dp.toPx())
                },
        ) {
            Text("Завершить разговор", fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Button(
            onClick = onContinue,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White),
            // Figma: две наложенные тени —
            // 0px 5.81px 46.47px 0px #0000001F и 0px 0px 11.62px 0px #0000001A.
            // dropShadow можно накладывать цепочкой — один слой на вызов.
            modifier = Modifier
                .weight(1f)
                .dropShadow(CircleShape) {
                    color = Color(0x1F000000)
                    radius = 46.47f
                    spread = 0f
                    offset = Offset(0f, 5.81.dp.toPx())
                }
                .dropShadow(CircleShape) {
                    color = Color(0x1A000000)
                    radius = 11.62f
                    spread = 0f
                    offset = Offset.Zero
                },
        ) {
            Text("Продолжить разговор", fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// WebView сам по себе не рендерится в Compose Preview (нужен реальный Android-рантайм) —
// область под UrlWebView будет пустой, но раскладка вокруг (кнопки/шапка/лого) видна.
@Preview(name = "Sheet — Answer", showBackground = true, heightDp = 900)
@Composable
private fun SheetAnswerPreview() {
    TalkingAvatarTheme {
        SheetHostContent(
            content = SheetContent.Answer(
                "<p>Для максимального интернета — линейка \"O! Комбо\".</p>" +
                        "<p><a href=\"https://o.kg\">Подробнее</a></p>",
            ),
            canGoBack = true,
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
