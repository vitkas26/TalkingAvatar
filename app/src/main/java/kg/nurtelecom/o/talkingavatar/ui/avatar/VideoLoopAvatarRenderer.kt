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
import androidx.compose.runtime.getValue
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kg.nurtelecom.o.talkingavatar.statemachine.AvatarState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "VideoLoopAvatar"
private const val PROCESSING_LOTTIE_ASSET = "avatar_processing.lottie"

// Единственное место маппинга состояния на файлы видео — добавлять/менять ассеты только здесь.
// Несколько файлов на состояние = при каждом входе в состояние выбирается случайный, для
// живости повтора. Processing сюда не входит: для него отдельная Lottie-ветка в Render() ниже.
private val stateToVideoAssets = mapOf(
    AvatarState.Welcome to listOf("avatar_welcome.mp4", "avatar_welcome1.mp4"),
    AvatarState.Idle to listOf("avatar_idle.mp4"),
    AvatarState.Listening to listOf("avatar_listening.mp4"),
    AvatarState.Speaking to listOf("avatar_speaking.mp4"),
    AvatarState.Error to listOf("avatar_error.mp4"),
)

private val stateToPlaceholderColor = mapOf(
    AvatarState.Welcome to Color(0xFF00897B),
    AvatarState.Idle to Color(0xFF546E7A),
    AvatarState.Listening to Color(0xFF1E88E5),
    AvatarState.Processing to Color(0xFFFB8C00),
    AvatarState.Speaking to Color(0xFF43A047),
    AvatarState.Error to Color(0xFFE53935),
    AvatarState.WebViewMode to Color(0xFF546E7A),
    AvatarState.EmergencyMode to Color(0xFFE53935),
    AvatarState.ManualLanguageSelection to Color(0xFF546E7A),
)

class VideoLoopAvatarRenderer @OptIn(UnstableApi::class) constructor
    (private val exoPlayer: ExoPlayer) : AvatarRenderer {

    // Список кандидатов активного состояния — читается из onPlaybackStateChanged при STATE_ENDED,
    // чтобы для состояний с несколькими клипами (Welcome) перевыбирать случайный при каждом
    // окончании петли, а не один раз за весь заход в состояние.
    private var activeCandidates: List<String> = emptyList()

    // Живёт на весь ExoPlayer (singleton), не привязан к конкретной композиции — переключение
    // видео может прийти как из Composable (смена state), так и из Player.Listener (реролл
    // по конце петли), нужен общий scope для обоих случаев. AndroidUiDispatcher.Main — не просто
    // Dispatchers.Main: он также даёт MonotonicFrameClock, без которого Animatable.animateTo()
    // падает с IllegalStateException ("MonotonicFrameClock is not available").
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
        exoPlayer.setSeekParameters(SeekParameters.EXACT)
        // Звук из видео-клипов не нужен: голос озвучивает TtsEngine отдельно, звуковая
        // дорожка в файле — просто исходник со съёмки. Полностью отключаем аудио-трек
        // (не просто volume=0), чтобы аудио-рендерер вообще не поднимался.
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
            .build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Срабатывает только для состояний с несколькими клипами (repeatMode=OFF там,
                // см. ниже) — для одиночных клипов стоит REPEAT_MODE_ONE, ended сюда не долетает.
                if (playbackState == Player.STATE_ENDED) {
                    val next = activeCandidates.randomOrNull() ?: return
                    Log.d(TAG, "loop ended -> reshuffled next=$next")
                    switchTo(next)
                }
            }

            override fun onRenderedFirstFrame() {
                // Новый кадр реально готов — гасим белый оверлей, открывая видео.
                scope.launch { overlayAlpha.animateTo(0f, tween(800)) }
            }
        })
    }

    @Composable
    override fun Render(state: AvatarState) {
        val context = LocalContext.current

        if (state == AvatarState.Processing) {
            val lottieExists = remember {
                context.assets.list("")?.contains(PROCESSING_LOTTIE_ASSET) == true
            }
            if (lottieExists) {
                val composition by rememberLottieComposition(LottieCompositionSpec.Asset(PROCESSING_LOTTIE_ASSET))
                val progress by animateLottieCompositionAsState(
                    composition,
                    iterations = LottieConstants.IterateForever,
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                )
                return
            }
            Log.d(TAG, "avatar state=$state -> asset '$PROCESSING_LOTTIE_ASSET' missing, showing placeholder")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(stateToPlaceholderColor[state] ?: Color.Gray),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = state.name, color = Color.White)
            }
            return
        }

        val available = remember(state) {
            val candidates = stateToVideoAssets[state].orEmpty()
            val existing = context.assets.list("")?.toSet().orEmpty()
            candidates.filter { it in existing }
        }
        val assetName = available.firstOrNull()

        if (assetName != null) {
            LaunchedEffect(state) {
                val picked = available.random()
                Log.d(TAG, "avatar state=$state -> candidates=$available picked=$picked")
                // Один клип на состояние — обычный внутренний луп ExoPlayer (плавнее).
                // Несколько клипов — repeatMode выключен, при STATE_ENDED слушатель в init{}
                // сам перевыбирает случайный следующий, так что во время использования (не
                // только между перезапусками) видео реально меняется.
                activeCandidates = if (available.size > 1) available else emptyList()
                exoPlayer.repeatMode = if (available.size > 1) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
                // Без явного stop() перед сменой MediaItem: ExoPlayer сам переключает
                // рендерер на новый item, а stop() только добавляет лишний пустой кадр
                // на стыке между состояниями. Чёрный оверлей (см. switchTo) маскирует его.
                switchTo(picked)
            }
            // TextureView, не PlayerView/SurfaceView: SurfaceView внутри Compose AndroidView
            // ломает z-order/clipping с соседними composable, TextureView композится как
            // обычная View и этих проблем не имеет — для маленького зацикленного видео
            // разница в производительности не важна.
            Box(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .background(stateToPlaceholderColor[state] ?: Color.Gray),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = state.name, color = Color.White)
            }
        }
    }
}
