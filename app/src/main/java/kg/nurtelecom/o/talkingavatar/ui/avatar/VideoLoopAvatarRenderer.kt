package kg.nurtelecom.o.talkingavatar.ui.avatar

import android.util.Log
import android.view.TextureView
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "VideoLoopAvatar"

// Единственное место маппинга состояния на файл видео — добавлять/менять ассеты только здесь.
private val stateToVideoAsset = mapOf(
    AvatarState.Welcome to "avatar_welcome.mp4",
    AvatarState.Idle to "avatar_idle.mp4",
    AvatarState.Speaking to "avatar_speaking.mp4",
    AvatarState.Error to "avatar_error.mp4",
    AvatarState.Listening to "avatar_listening.mp4",
)

private val stateToPlaceholderColor = mapOf(
    AvatarState.Welcome to Color(0xFF00897B),
    AvatarState.Idle to Color(0xFF546E7A),
    AvatarState.Listening to Color(0xFF1E88E5),
    AvatarState.Speaking to Color(0xFF43A047),
    AvatarState.Error to Color(0xFFE53935),
    AvatarState.WebViewMode to Color(0xFF546E7A),
    AvatarState.EmergencyMode to Color(0xFFE53935),
    AvatarState.ManualLanguageSelection to Color(0xFF546E7A),
)

class VideoLoopAvatarRenderer @OptIn(UnstableApi::class) constructor
    (private val exoPlayer: ExoPlayer) : AvatarRenderer {

    // Живёт на весь ExoPlayer (singleton), не привязан к конкретной композиции — фейд (см. ниже)
    // запускается и из Composable (смена state), и из Player.Listener (onRenderedFirstFrame).
    // AndroidUiDispatcher.Main — не просто Dispatchers.Main: он также даёт MonotonicFrameClock,
    // без которого Animatable.animateTo() падает с IllegalStateException
    // ("MonotonicFrameClock is not available").
    private val scope = CoroutineScope(AndroidUiDispatcher.Main + SupervisorJob())

    // Белый оверлей поверх TextureView: коротко проявляем перед сменой MediaItem, снова
    // гасим по onRenderedFirstFrame нового клипа — маскирует рывок/чёрный кадр на стыке
    // вместо второго плеера с кроссфейдом (усложнение того не стоит для этого случая).
    private val overlayAlpha = Animatable(1f)

    private fun switchTo(asset: String) {
        scope.launch { overlayAlpha.animateTo(1f, tween(800)) }
        exoPlayer.setMediaItem(MediaItem.fromUri("asset:///$asset"))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    init {
        // Точный сик на границе лупа сглаживает откат не-на-нулевой-кадр. Остаточный дёрг на
        // стыке цикла — известное ограничение ExoPlayer для looped single-item плейбека, зависит
        // от GOP/keyframe-раскладки самого файла; если после подключения чистового видео (без
        // чёрных кадров в начале/конце) рывок всё ещё заметен — это его нижний предел без
        // двойного ping-pong буфера, которым здесь намеренно не усложняем.
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.setSeekParameters(SeekParameters.EXACT)
        // Звук из видео-клипов не нужен: голос озвучивает TtsEngine отдельно, звуковая
        // дорожка в файле — просто исходник со съёмки. Полностью отключаем аудио-трек
        // (не просто volume=0), чтобы аудио-рендерер вообще не поднимался.
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
            .build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                // Новый кадр реально готов — гасим белый оверлей, открывая видео.
                scope.launch { overlayAlpha.animateTo(0f, tween(800)) }
            }
        })
    }

    @Composable
    override fun Render(state: AvatarState, modifier: Modifier) {
        val context = LocalContext.current

        val assetName = remember(state) {
            val candidate = stateToVideoAsset[state]
            val existing = context.assets.list("")?.toSet().orEmpty()
            candidate?.takeIf { it in existing }
        }

        if (assetName != null) {
            LaunchedEffect(state) {
                Log.d(TAG, "avatar state=$state -> loading asset=$assetName")
                // Без явного stop() перед сменой MediaItem: ExoPlayer сам переключает
                // рендерер на новый item, а stop() только добавляет лишний пустой кадр
                // на стыке между состояниями. Белый оверлей (см. switchTo) маскирует его.
                switchTo(assetName)
            }
            // TextureView, не PlayerView/SurfaceView: SurfaceView внутри Compose AndroidView
            // ломает z-order/clipping с соседними composable, TextureView композится как
            // обычная View и этих проблем не имеет — для маленького зацикленного видео
            // разница в производительности не важна.
            Box(modifier = modifier) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            )
                            // Прикрепляем к плееру сразу в factory (синхронно, до LaunchedEffect),
                            // иначе play() из LaunchedEffect иногда успевает раньше, чем update{}
                            // выставит video output — рендерер видео не включается вообще.
                            exoPlayer.setVideoTextureView(this)
                        }
                    },
                )
                // Белый оверлей: гасится по onRenderedFirstFrame нового клипа (см. init{}),
                // маскирует рывок/чёрный кадр на стыке смены видео.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = overlayAlpha.value)),
                )
            }
        } else {
            Log.d(TAG, "avatar state=$state -> asset '$assetName' missing, showing placeholder")
            // Заглушка: видео-файл под это состояние ещё не добавлен в assets.
            Box(
                modifier = modifier.background(stateToPlaceholderColor[state] ?: Color.Gray),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = state.name, color = Color.White)
            }
        }
    }
}
