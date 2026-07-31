package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kg.nurtelecom.o.talkingavatar.domain.model.Language
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors
import kg.nurtelecom.o.talkingavatar.ui.theme.TalkingAvatarTheme

// Выбор языка (Figma frame «Выбор языка»): заголовок + сетка языков-пилюль с radio + «Выбрать».
// Выбор двухшаговый (как в дизайне): отметить язык -> «Выбрать» подтверждает.
@Composable
fun LanguagePickerContent(
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

@Preview(name = "Sheet — LanguagePicker", showBackground = true, heightDp = 900)
@Composable
private fun SheetLanguagePickerPreview() {
    TalkingAvatarTheme {
        SheetHostContent(
            content = SheetContent.LanguagePicker,
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
