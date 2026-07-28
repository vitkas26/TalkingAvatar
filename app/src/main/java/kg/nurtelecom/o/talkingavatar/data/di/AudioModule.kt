package kg.nurtelecom.o.talkingavatar.data.di

import kg.nurtelecom.o.talkingavatar.data.speech.AndroidSttEngine
import kg.nurtelecom.o.talkingavatar.data.speech.AndroidTtsEngine
import kg.nurtelecom.o.talkingavatar.data.speech.DebugRoutingSttEngine
import kg.nurtelecom.o.talkingavatar.data.speech.DebugRoutingTtsEngine
import kg.nurtelecom.o.talkingavatar.data.speech.EngineSettings
import kg.nurtelecom.o.talkingavatar.data.speech.LanguageAwareSttEngine
import kg.nurtelecom.o.talkingavatar.data.speech.LanguageAwareTtsEngine
import kg.nurtelecom.o.talkingavatar.domain.gateway.SttEngine
import kg.nurtelecom.o.talkingavatar.domain.gateway.TtsEngine
import kg.nurtelecom.o.talkingavatar.data.speech.akylai.AkylAiApiService
import kg.nurtelecom.o.talkingavatar.data.speech.akylai.AkylAiConfig
import kg.nurtelecom.o.talkingavatar.data.speech.akylai.AkylAiSttEngine
import kg.nurtelecom.o.talkingavatar.data.speech.akylai.AkylAiTtsEngine
import kg.nurtelecom.o.talkingavatar.data.speech.googlecloud.GoogleCloudApiService
import kg.nurtelecom.o.talkingavatar.data.speech.googlecloud.GoogleCloudConfig
import kg.nurtelecom.o.talkingavatar.data.speech.googlecloud.GoogleCloudSttEngine
import kg.nurtelecom.o.talkingavatar.data.speech.googlecloud.GoogleCloudTtsEngine
import kg.nurtelecom.o.talkingavatar.data.speech.piper.PiperApiService
import kg.nurtelecom.o.talkingavatar.data.speech.piper.PiperConfig
import kg.nurtelecom.o.talkingavatar.data.speech.piper.PiperTtsEngine
import kg.nurtelecom.o.talkingavatar.data.speech.whisper.WhisperApiService
import kg.nurtelecom.o.talkingavatar.data.speech.whisper.WhisperConfig
import kg.nurtelecom.o.talkingavatar.data.speech.whisper.WhisperSttEngine
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Квалификаторы: несколько реализаций SttEngine/TtsEngine одновременно зарегистрированы в Koin
// (system/whisper/akylai/languageAware), get() по голому интерфейсу без квалификатора инферится
// как SttEngine и резолвится в единственный unqualified single<SttEngine> — то есть в самого себя
// (DebugRoutingSttEngine) -> StackOverflowError. Каждая реализация регистрируется под своим именем.
private val system = named("system")
private val whisper = named("whisper")
private val akylai = named("akylai")
private val piper = named("piper")
private val languageAware = named("languageAware")
private val languageAwarePiper = named("languageAwarePiper")
private val googleCloud = named("googleCloud")
private val languageAwareGoogleCloud = named("languageAwareGoogleCloud")

val audioModule = module {
    single { EngineSettings() }

    // --- Системный Android STT/TTS (без изменений, доступны как один из вариантов выбора) ---
    single<SttEngine>(system) { AndroidSttEngine(androidContext()) }
    single<TtsEngine>(system) { AndroidTtsEngine(androidContext()) }

    // --- Whisper (локальный Ktor-сервис, см. WhisperApiService/WhisperConfig) ---
    single {
        val engineSettings = get<EngineSettings>()
        retrofitService(WhisperConfig.BASE_URL, WhisperApiService::class.java, engineSettings) {
            engineSettings.whisperBaseUrl
        }
    }
    single<SttEngine>(whisper) { WhisperSttEngine(androidContext(), get(), get()) }

    // --- AkylAI (локальный HTTP-сервис вокруг AkylAI-STT/AkylAI-TTS-mini, см. AkylAiConfig) ---
    single {
        val engineSettings = get<EngineSettings>()
        retrofitService(AkylAiConfig.BASE_URL, AkylAiApiService::class.java, engineSettings) {
            engineSettings.akylAiBaseUrl
        }
    }
    single<SttEngine>(akylai) { AkylAiSttEngine(androidContext(), get(), get()) }
    single<TtsEngine>(akylai) { AkylAiTtsEngine(androidContext(), get()) }

    // --- Piper (локальный TTS-сервис, см. PiperApiService/PiperConfig) ---
    single {
        val engineSettings = get<EngineSettings>()
        retrofitService(PiperConfig.BASE_URL, PiperApiService::class.java, engineSettings) {
            engineSettings.piperBaseUrl
        }
    }
    single<TtsEngine>(piper) { PiperTtsEngine(androidContext(), get()) }

    // --- Google Cloud (через google-cloud-proxy на том же VPS, см. GoogleCloudApiService/Config) ---
    // Тот же паттерн host-override + Basic Auth, что и у остальных, плюс X-Proxy-Token поверх.
    single {
        val engineSettings = get<EngineSettings>()
        retrofitService(
            placeholderBaseUrl = GoogleCloudConfig.BASE_URL,
            apiClass = GoogleCloudApiService::class.java,
            engineSettings = engineSettings,
            currentBaseUrl = { engineSettings.googleCloudBaseUrl },
            extraInterceptors = listOf(proxyTokenInterceptor(engineSettings)),
        )
    }
    single<SttEngine>(googleCloud) { GoogleCloudSttEngine(androidContext(), get(), get()) }
    single<TtsEngine>(googleCloud) { GoogleCloudTtsEngine(androidContext(), get(), get()) }

    // --- Языковой роутинг (ky-* -> AkylAI, остальное -> Whisper/системный TTS) ---
    single<SttEngine>(languageAware) {
        LanguageAwareSttEngine(whisperEngine = get(whisper), akylAiEngine = get(akylai))
    }
    single<TtsEngine>(languageAware) {
        LanguageAwareTtsEngine(fallbackEngine = get(system), akylAiTtsEngine = get(akylai))
    }
    // Комбо "Piper + AkylAI": ky-* -> AkylAI, остальное -> Piper вместо системного TTS.
    single<TtsEngine>(languageAwarePiper) {
        LanguageAwareTtsEngine(fallbackEngine = get(piper), akylAiTtsEngine = get(akylai))
    }
    // Комбо "Google Cloud + AkylAI". TTS: ky-* -> AkylAI, остальное -> Google Cloud (как обычно).
    // STT: ВСЕГДА Google Cloud, без языкового роутинга — иначе самозамыкание: Google STT детектит
    // ky-KG -> language сессии становится ky-KG -> LanguageAwareSttEngine перекидывает STT на
    // akylai -> akylai не умеет auto-detect и никогда не шлёт languageDetected -> язык сессии
    // навсегда застревает на ky-KG, разговор больше не может выйти из akylai-STT. Google STT и так
    // корректно распознаёт кыргызский сам (в отличие от Google TTS, где голосов ky-KG просто нет).
    single<SttEngine>(languageAwareGoogleCloud) { get(googleCloud) }
    single<TtsEngine>(languageAwareGoogleCloud) {
        LanguageAwareTtsEngine(fallbackEngine = get(googleCloud), akylAiTtsEngine = get(akylai))
    }

    // --- Финальный движок, который реально инжектится в MainViewModel: поверх языкового
    // роутинга ещё и ручной оверрайд с экрана настроек (debug для пилота) ---
    single<SttEngine> {
        DebugRoutingSttEngine(
            settings = get(),
            systemEngine = get(system),
            whisperEngine = get(whisper),
            akylAiEngine = get(akylai),
            languageAwareEngine = get(languageAware),
            googleCloudEngine = get(googleCloud),
            googleCloudPlusAkylaiEngine = get(languageAwareGoogleCloud),
        )
    }
    single<TtsEngine> {
        DebugRoutingTtsEngine(
            settings = get(),
            systemEngine = get(system),
            akylAiEngine = get(akylai),
            piperEngine = get(piper),
            languageAwareEngine = get(languageAware),
            piperAkylAiEngine = get(languageAwarePiper),
            googleCloudEngine = get(googleCloud),
            googleCloudPlusAkylaiEngine = get(languageAwareGoogleCloud),
        )
    }
}
