package kg.nurtelecom.o.talkingavatar.ui.conversation.sheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kg.nurtelecom.o.talkingavatar.ui.theme.LocalAppColors

// Первичная кнопка-пилюля в стиле бренда (в Figma — тёмная/розовая liquid-glass; здесь тёмная).
// Общая для Intro и LanguagePicker.
@Composable
fun PrimaryPillButton(text: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
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
