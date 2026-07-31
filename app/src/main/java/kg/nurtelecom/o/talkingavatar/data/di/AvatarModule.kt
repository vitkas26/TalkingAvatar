package kg.nurtelecom.o.talkingavatar.data.di

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kg.nurtelecom.o.talkingavatar.ui.avatar.AvatarRenderer
import kg.nurtelecom.o.talkingavatar.ui.avatar.VideoLoopAvatarRenderer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val avatarModule = module {
    single {
        ExoPlayer.Builder(androidContext()).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d("VideoLoopAvatar", "playbackState=$playbackState")
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoLoopAvatar", "playerError", error)
                }

                override fun onRenderedFirstFrame() {
                    Log.d("VideoLoopAvatar", "renderedFirstFrame")
                }
            })
        }
    }
    single<AvatarRenderer> { VideoLoopAvatarRenderer(get()) }
}
