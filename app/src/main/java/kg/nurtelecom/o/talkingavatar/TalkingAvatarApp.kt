package kg.nurtelecom.o.talkingavatar

import android.app.Application
import kg.nurtelecom.o.talkingavatar.data.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TalkingAvatarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TalkingAvatarApp)
            androidLogger()
            modules(appModule)
        }
    }
}
