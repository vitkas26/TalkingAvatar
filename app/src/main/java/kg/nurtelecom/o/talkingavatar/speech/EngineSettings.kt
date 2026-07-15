package kg.nurtelecom.o.talkingavatar.speech

import kg.nurtelecom.o.talkingavatar.speech.akylai.AkylAiConfig
import kg.nurtelecom.o.talkingavatar.speech.googlecloud.GoogleCloudConfig
import kg.nurtelecom.o.talkingavatar.speech.piper.PiperConfig
import kg.nurtelecom.o.talkingavatar.speech.whisper.WhisperConfig

enum class SttEngineChoice { AUTO, SYSTEM, WHISPER, AKYLAI, GOOGLE_CLOUD }
enum class TtsEngineChoice { AUTO, SYSTEM, AKYLAI, PIPER, PIPER_AKYLAI, GOOGLE_CLOUD }

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

    // То же самое для google-cloud-proxy (Ktor-прокси к Google Cloud TTS/STT на том же VPS).
    var googleCloudBaseUrl: String = GoogleCloudConfig.BASE_URL

    // Доп. авторизация для google-cloud-proxy поверх Basic Auth — значение не хардкодится,
    // задаётся с экрана настроек. Пусто — заголовок X-Proxy-Token не отправляется.
    var googleCloudProxyToken: String = ""

    // Альтернативные языки для STT-детекции (query-параметр `alt` у google-cloud-proxy /stt).
    var googleCloudAltLanguages: String = "ky-KG,tr-TR"

    // Basic Auth для тестового VPS-стенда за nginx (30 дней, не продакшен-секрет).
    // Читается BasicAuthInterceptor в AudioModule на каждый запрос, как и base URL выше.
    var basicAuthUser: String = "admin"
    var basicAuthPassword: String = "VIKtor26!@88"
}
