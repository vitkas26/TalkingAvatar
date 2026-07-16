package kg.nurtelecom.o.talkingavatar.speech

import kg.nurtelecom.o.talkingavatar.speech.akylai.AkylAiConfig
import kg.nurtelecom.o.talkingavatar.speech.googlecloud.GoogleCloudConfig
import kg.nurtelecom.o.talkingavatar.speech.piper.PiperConfig
import kg.nurtelecom.o.talkingavatar.speech.whisper.WhisperConfig

enum class SttEngineChoice { AUTO, SYSTEM, WHISPER, AKYLAI, GOOGLE_CLOUD, GOOGLE_CLOUD_PLUS_AKYLAI }
enum class TtsEngineChoice { AUTO, SYSTEM, AKYLAI, PIPER, PIPER_AKYLAI, GOOGLE_CLOUD, GOOGLE_CLOUD_PLUS_AKYLAI }

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

    // Доп. авторизация для google-cloud-proxy поверх Basic Auth (тестовый VPS-стенд,
    // не продакшен-секрет, как и basicAuthPassword выше). Editable с экрана настроек
    // на случай если токен на VPS сменится.
    var googleCloudProxyToken: String = "18140044a93a50e72bdde439c025f867fb328c84948e142ac610bef0181b862b"

    // Ручной оверрайд alt-языков для STT-детекции (query-параметр `alt` у google-cloud-proxy
    // /stt) — если пусто, GoogleCloudSttEngine берёт разумный дефолт по основному языку из
    // GoogleCloudSttEngine.defaultAltLanguagesByPrimary. Непусто — значение здесь применяется
    // ко всем языкам одинаково, независимо от выбранного основного.
    var googleCloudAltLanguages: String = ""

    // Debug A/B-переключатель версии Google Speech-to-text API на прокси ("v1"/"v2",
    // query-параметр apiVersion у google-cloud-proxy /stt). Временная опция для сравнения
    // точности — не постоянная архитектурная фича. v2 игнорирует lang/alt (полный автодетект).
    var googleCloudApiVersion: String = "v1"

    // Debug A/B-переключатель голоса Google Cloud TTS ("chirp3hd"/"wavenet") — живое демо для
    // заказчика, выбор голоса на слух, не постоянная фича. chirp3hd (дефолт) не меняет поведение:
    // voiceName не шлём, прокси сам подставляет Chirp3-HD-Aoede по gender.
    var googleCloudTtsTier: String = "chirp3hd"

    // VAD (определение конца фразы по паузе) — общий для Whisper/AkylAI/GoogleCloud STT, см.
    // speech/vad/SilenceTracker.kt. Порог в дБ ещё не откалиброван под шумный ТРЦ (открытый
    // вопрос, ждём вендора микрофона) — настраиваемый с экрана настроек, не хардкожен намертво.
    var vadSilenceThresholdDb: Double = -40.0
    var vadSilenceDurationMs: Long = 4000L // бизнес-требование 3-5 сек, дефолт — середина диапазона
    var vadMaxRecordingMs: Long = 18_000L // safety net, не основной механизм остановки

    // Basic Auth для тестового VPS-стенда за nginx (30 дней, не продакшен-секрет).
    // Читается BasicAuthInterceptor в AudioModule на каждый запрос, как и base URL выше.
    var basicAuthUser: String = "admin"
    var basicAuthPassword: String = "VIKtor26!@88"
}
