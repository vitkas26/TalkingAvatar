package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors

// Ответ Нурай — простой HTML (абзацы + ссылки), без картинок/вёрстки/скриптов. Для такого
// WebView избыточен: не участвует в Compose nested-scroll (это и была причина конфликта скролла
// с шитом, см. историю правок), не даёт нормально задать паддинги/типографику. HtmlCompat.
// fromHtml -> Spanned -> AnnotatedString рендерит тот же контент обычным Compose Text — родной
// скролл, родные паддинги, ссылки кликабельны через LinkAnnotation (Compose 1.6+).
@Composable
fun HtmlAnswerText(html: String, onLinkClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val annotated = remember(html) {
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toAnnotatedString(linkColor = colors.accent, onLinkClick = onLinkClick)
    }
    Text(
        text = annotated,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color.Black,
    )
}

private fun Spanned.toAnnotatedString(linkColor: Color, onLinkClick: (String) -> Unit): AnnotatedString {
    val spanned = this
    return buildAnnotatedString {
        append(spanned.toString())
        for (span in spanned.getSpans(0, spanned.length, Any::class.java)) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start < 0 || end < 0 || start >= end) continue
            when (span) {
                is StyleSpan -> when (span.style) {
                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                    Typeface.BOLD_ITALIC -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                        start,
                        end,
                    )
                }
                is URLSpan -> addLink(
                    LinkAnnotation.Url(
                        url = span.url,
                        styles = TextLinkStyles(
                            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        ),
                    ) { onLinkClick(span.url) },
                    start,
                    end,
                )
            }
        }
    }
}
