package kg.nurtelecom.o.talkingavatar.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Семантические цвета бренда NURAi — единая точка правки палитры (в т.ч. под будущие
// праздничные темы: собрать второй AppColors и передать его в TalkingAvatarTheme).
// Компоненты читают LocalAppColors.current.xxx, не хардкодят Color(0xFF...) сами.
data class AppColors(
    val accent: Color,                    // акцент бренда — выбранный язык (контур/точка radio), "Слушает...", статус-текст
    val primaryButtonGradient: List<Color>, // радиальный градиент pill-кнопки (Понятно / Выбрать), центр -> край
    val stageBackground: Color,           // фон сцены аватара и низ градиента под видео-рамкой
    val border: Color,                    // тонкий контур невыбранной ячейки языка
    val borderMuted: Color,               // контур невыбранного radio-кружка в ячейке
    val surface: Color,                   // фон ячейки языка, белые круглые кнопки (close/stop) поверх видео
    val greetingText: Color,              // текст приветствия на welcome-экране
    val warningBackground: Color,         // фон карточки-предупреждения (snackbar с ic_error)
)

val DefaultAppColors = AppColors(
    accent = Color(0xFFF0047F),
    primaryButtonGradient = listOf(Color(0xFF3E4E5D), Color(0xFF969FA8)),
    stageBackground = Color(0xFFEDEDED),
    border = Color(0xFFE5E5E5),
    borderMuted = Color(0xFFCCCCCC),
    surface = Color.White,
    greetingText = Color(0xFF4A4A4A),
    warningBackground = Color(0xFFF4EEDD),
)

val LocalAppColors = staticCompositionLocalOf { DefaultAppColors }
