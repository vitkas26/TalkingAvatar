package kg.nurtelecom.o.talkingavatar.speech

import kg.nurtelecom.o.talkingavatar.speech.akylai.AkylAiConfig
import kg.nurtelecom.o.talkingavatar.speech.piper.PiperConfig
import kg.nurtelecom.o.talkingavatar.speech.whisper.WhisperConfig

enum class SttEngineChoice { AUTO, SYSTEM, WHISPER, AKYLAI }
enum class TtsEngineChoice { AUTO, SYSTEM, AKYLAI, PIPER, PIPER_AKYLAI }

// Debug-настройки пилота: меняются с экрана настроек, читаются роутинг-движками
// на каждый вызов — переключение работает без пересборки Koin-графа.
class EngineSettings {
    var sttChoice: SttEngineChoice = SttEngineChoice.AUTO
    var ttsChoice: TtsEngineChoice = TtsEngineChoice.AUTO

    // Адрес локального AkylAI-сервиса — IP ноутбука меняется от сети к сети, задаётся
    // с экрана настроек вместо пересборки APK. Читается интерсептором в AudioModule
    // на каждый запрос, так что меняется на лету без рестарта Koin-графа.
    var akylAiBaseUrl: String = AkylAiConfig.BASE_URL

    // То же самое для локального Whisper-сервера (свой Ktor-сервис, не облачный OpenAI).
    var whisperBaseUrl: String = WhisperConfig.BASE_URL

    // То же самое для локального Piper TTS-сервера.
    var piperBaseUrl: String = PiperConfig.BASE_URL

    // Basic Auth для тестового VPS-стенда за nginx (30 дней, не продакшен-секрет).
    // Читается BasicAuthInterceptor в AudioModule на каждый запрос, как и base URL выше.
    var basicAuthUser: String = "admin"
    var basicAuthPassword: String = "VIKtor26!@88"
}
