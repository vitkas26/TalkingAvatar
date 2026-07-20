package kg.nurtelecom.o.talkingavatar.ui.avatar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kg.nurtelecom.o.talkingavatar.R
import kg.nurtelecom.o.talkingavatar.ui.theme.ListeningBackground
import kg.nurtelecom.o.talkingavatar.ui.theme.ListeningStatusPink

@Composable
fun ListeningStateContent(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ListeningBackground),
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
                .size(48.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Закрыть",
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.ic_avatar_bg),
                    contentDescription = null,
                    modifier = Modifier.size(260.dp),
                )
                Image(
                    painter = painterResource(R.drawable.ic_avatar_listening),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Слушает...",
                color = ListeningStatusPink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "NURAi",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
        )
    }
}
