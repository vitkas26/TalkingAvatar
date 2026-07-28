package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.domain.model.Language
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors

// Контент единого боттомшита. Один хост рендерит все состояния (см. SheetContent) через
// AnimatedContent; back/close сверху; BackHandler для системной «назад» внутри шита.
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
    BackHandler(enabled = true) { onBack() }

    Column(modifier = modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)) {
        // Шапка: назад (если есть куда) или закрыть. Явный размер IconButton (вместо
        // дефолтного) + минимальный top-паддинг — крестик должен сидеть точно в углу шита,
        // не проваливаться под drag-handle отступ (см. dragHandle = null у ModalBottomSheet).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canGoBack) {
                TextButton(onClick = onBack) { Text("‹ Назад") }
            }
            Box(Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Закрыть",
                modifier = Modifier
                    .size(64.dp)
                    .clickable { onClose() },
            )
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

                is SheetContent.Answer -> AnswerContent(c.html, onLinkClick, onContinue, onFinish)
                is SheetContent.Web -> UrlWebView(
                    url = c.url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 480.dp),
                )
            }
        }
    }
}

// Знакомство (Figma frame «Знакомство»): заголовок + описание + список 6 языков + «Понятно».
@Composable
private fun IntroContent(languages: List<Language>, onOk: () -> Unit) {
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

// Выбор языка (Figma frame «Выбор языка»): заголовок + сетка языков-пилюль с radio + «Выбрать».
// Выбор двухшаговый (как в дизайне): отметить язык -> «Выбрать» подтверждает.
@Composable
private fun LanguagePickerContent(
    languages: List<Language>,
    selected: Language,
    onConfirm: (Language) -> Unit,
) {
    var picked by remember(selected) { mutableStateOf(selected) }
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            "Выберите язык:",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 20.dp),
        )
        languages.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { lang ->
                    LanguageCell(
                        name = lang.displayName,
                        selected = lang == picked,
                        onClick = { picked = lang },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(120.dp))
        PrimaryPillButton("Выбрать", onClick = { onConfirm(picked) })
        Spacer(Modifier.height(8.dp))
    }
}

// Ячейка языка — пилюля (cornerRadius=100 в Figma). Белый фон всегда; выбранная — розовый
// контур пилюли + классический radio-button (кружок с розовой точкой внутри), невыбранная —
// серый контур + пустой кружок. Раньше здесь была заливка + галочка — не похоже на радиокнопку
// из дизайна, поменяли на настоящий radio-look по референсу.
@Composable
private fun LanguageCell(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val background = if (selected) colors.accent.copy(alpha = 0.1f) else colors.surface
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(0.75.dp, if (selected) colors.accent else colors.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, if (selected) colors.accent else colors.borderMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        }
    }
}

// Первичная кнопка-пилюля в стиле бренда (в Figma — тёмная/розовая liquid-glass; здесь тёмная).
@Composable
private fun PrimaryPillButton(text: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            // Brush.radialGradient() без явного radius берёт minDimension/2 (высоту кнопки) —
            // получается маленький кружок посередине. Радиус = половина ширины растягивает
            // градиент на всю пилюлю (drawWithCache — размер известен только на этапе отрисовки).
            .drawWithCache {
                val brush = Brush.radialGradient(
                    colors = colors.primaryButtonGradient,
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width / 2f,
                )
                onDrawBehind { drawRect(brush) }
            },
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun AnswerContent(
    html: String,
    onLinkClick: (String) -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        HtmlAnswerWebView(
            html = html,
            onLinkClick = onLinkClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 480.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onFinish, modifier = Modifier.weight(1f)) {
                Text("Завершить разговор")
            }
            Button(onClick = onContinue, modifier = Modifier.weight(1f)) {
                Text("Продолжить разговор")
            }
        }
    }
}
