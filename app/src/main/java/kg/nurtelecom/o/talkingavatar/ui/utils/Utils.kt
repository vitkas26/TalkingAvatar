package kg.nurtelecom.o.talkingavatar.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

fun Modifier.rotateFullScreen(rotationDegrees: Float = -90f) = this
    .layout { measurable, constraints ->
        // Меряем контент как если бы экран был "перевёрнут" (ширина/высота местами)
        val placeable = measurable.measure(
            Constraints.fixed(constraints.maxHeight, constraints.maxWidth)
        )
        // Обратно объявляем родителю ИСХОДНЫЙ размер экрана (не перевёрнутый!)
        layout(placeable.height, placeable.width) {
            // Компенсируем смещение центра: сам placeable измерен в "перевёрнутых"
            // координатах, а после rotationZ его физический центр не совпадает
            // с центром заявленного (обратно-перевёрнутого) bounding box
            placeable.place(
                x = -(placeable.width - placeable.height) / 2,
                y = -(placeable.height - placeable.width) / 2
            )
        }
    }
    .graphicsLayer {
        rotationZ = rotationDegrees
    }