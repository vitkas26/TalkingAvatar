package kg.nurtelecom.o.talkingavatar.data.di

import kg.nurtelecom.o.talkingavatar.speech.AndroidSttEngine
import kg.nurtelecom.o.talkingavatar.speech.AndroidTtsEngine
import kg.nurtelecom.o.talkingavatar.speech.SttEngine
import kg.nurtelecom.o.talkingavatar.speech.TtsEngine
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val audioModule = module {
    single<SttEngine> { AndroidSttEngine(androidContext()) }
    single<TtsEngine> { AndroidTtsEngine(androidContext()) }
}
