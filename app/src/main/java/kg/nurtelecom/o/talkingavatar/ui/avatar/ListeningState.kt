package kg.nurtelecom.o.talkingavatar.ui.avatar

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kg.nurtelecom.o.talkingavatar.R

// Пульсирующее свечение вокруг кружка на экране прослушивания. Позиционируется вызывающим
// (см. MainScreen) — здесь только сама Lottie-анимация фиксированного диаметра.
@Composable
fun LottieGlow(diameter: Dp, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.anim_voice))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(diameter),
    )
}
