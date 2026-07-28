# TalkingAvatar (NURAi)

Android-приложение (`kg.nurtelecom.o.talkingavatar`) — голосовой ассистент NURAi с видео-аватаром: пользователь задаёт вопрос голосом, приложение распознаёт речь (STT), получает HTML-ответ от бэкенда, озвучивает его (TTS) и синхронно управляет видео-аватаром. Аватар живёт как непрерывное видео, а состояния (приветствие / слушает / говорит) — переходы поверх него.

## Стек

- **Язык/UI:** Kotlin, Jetpack Compose
- **Архитектура:** Orbit-MVI (`ContainerHost`, State + SideEffect). Целевые правила — см. `CLAUDE.md` (Clean Architecture, SOLID, type-safe Compose Navigation).
- **DI:** Koin
- **Видео-аватар:** ExoPlayer (media3) + `TextureView`, mp4-ассеты по состояниям
- **Свечение:** Lottie (`anim_voice.json`)
- **Сеть:** Retrofit + OkHttp
- **STT/TTS:** несколько провайдеров с маршрутизацией по языку/настройке (см. ниже)
- **VAD:** собственный трекер тишины по амплитуде
- **Хранилище:** DataStore Preferences (настройки движков, токены)

## Экраны и флоу

Один экран (`MainScreen`) с состояниями аватара + единый боттомшит. Отдельного экрана «слушания» нет — это состояния одной видео-поверхности.

1. **Welcome / Idle** — прямоугольное видео (машет рукой), кнопка mic.
2. tap mic → **Listening** — видео-маска стягивается в **круг** вокруг лица (iris/диафрагма, масштаб лица постоянный), свечение (Lottie) + «Слушает…».
3. распознано → запрос к бэку → TTS-ответ → **Speaking** — снова прямоугольник + кнопка **«Ответ в текстовом виде»** + close.
4. tap «Ответ в текстовом виде» → **боттомшит** с HTML-ответом (+ «Продолжить / Завершить разговор»).
5. tap ссылки в ответе → **webview** внутри того же шита (назад — к тексту).

Единый боттомшит (`SheetContent`) обслуживает: **Знакомство**, **Выбор языка**, **Ответ (HTML)**, **Webview** — с внутренним стеком «назад».

Переход welcome↔listening — **aperture/iris**: одна постоянная видео-поверхность (не пересоздаётся), анимируется только clip-маска (прямоугольник↔круг) + лёгкий зум/панорама. Реализация — `MainScreen.ApertureShape` + `progress` (`animateFloatAsState`). Подробности и причина отказа от `SharedTransitionLayout` — в комментариях `MainScreen.kt`.

**Debug-экран настроек** (`SettingsScreen`) показывается первым (для пилота): выбор языка и ручной оверрайд STT/TTS-движка, адреса сервисов, параметры VAD, A/B тумблеры.

## Структура проекта

```
app/src/main/java/kg/nurtelecom/o/talkingavatar/
├── MainActivity.kt              — единственная Activity (SettingsScreen -> MainScreen), edge-to-edge
├── TalkingAvatarApp.kt          — Application, инициализация Koin
├── domain/                      — чистый Kotlin, без Android-типов (presentation → domain ← data)
│   ├── model/                   — Answer, Language
│   ├── repository/QuestionRepository.kt — интерфейс
│   ├── usecase/AskQuestionUseCase.kt
│   └── gateway/                 — SttEngine, TtsEngine (контракты STT/TTS, аналог repository)
├── data/
│   ├── api/ApiService.kt        — Retrofit POST /ask (сейчас мок, отдаёт HTML со ссылками)
│   ├── api/MockAnswers.kt       — заглушки ответов по языкам
│   ├── models/QuestionRequest.kt— QuestionRequest, AnswerResponse(answer: html)
│   ├── repository/QuestionRepositoryImpl.kt — реализация + HTML→текст для TTS
│   ├── speech/                  — реализации SttEngine/TtsEngine (см. раздел ниже) + vad/
│   └── di/                      — Koin-модули: MainModule, AvatarModule, AudioModule, RetrofitFactory
├── network/                     — AuthInterceptor, TokenManager (DataStore)
└── ui/
    ├── avatar/
    │   ├── AvatarRenderer.kt        — интерфейс Render(state, modifier)
    │   ├── VideoLoopAvatarRenderer.kt — ExoPlayer+TextureView, маппинг state->mp4, кроссфейд
    │   ├── AvatarState.kt           — состояния аватара (Welcome/Idle/Listening/Speaking/Error/...)
    │   └── ListeningState.kt        — LottieGlow (свечение круга)
    ├── mainScreen/
    │   ├── MainScreen.kt        — корневой UI, aperture-переход, обвязка состояний
    │   ├── MainVM.kt            — MainViewModel (MainState, sheet-стек, STT/TTS-оркестрация)
    │   ├── SheetContent.kt      — sealed-контент боттомшита
    │   ├── SheetHost.kt         — единый хост шита (Intro/Language/Answer + back/close)
    │   └── SheetWebView.kt      — HtmlAnswerWebView (перехват ссылок) + UrlWebView
    ├── settings/SettingsScreen.kt — debug-настройки (пилот)
    └── theme/                   — Color.kt, Theme.kt, Type.kt
```

`AvatarState` — presentation-состояние (управляет только тем что рисовать), поэтому лежит в `ui/avatar/`, не в domain. `SttEngine`/`TtsEngine` — чистые контракты без Android-типов в сигнатуре, поэтому в `domain/gateway/`; все конкретные реализации (Android/Whisper/AkylAI/Piper/GoogleCloud, роутеры, VAD) — инфраструктура, в `data/speech/`.

## STT / TTS (маршрутизация)

Единые интерфейсы `SttEngine` / `TtsEngine`, поверх — обёртки-роутеры:
- `DebugRoutingStt/TtsEngine` — выбор провайдера по `EngineSettings` (debug-экран).
- `LanguageAwareStt/TtsEngine` — выбор по языку (напр. ky-KG → AkylAI, остальное → провайдер X).

Провайдеры (все в `data/speech/`):
- **Android** нативный (`AndroidSttEngine`, `AndroidTtsEngine`).
- **Whisper** (`data/speech/whisper`) — cloud STT.
- **AkylAI** (`data/speech/akylai`) — STT + TTS (в т.ч. кыргызский).
- **Piper** (`data/speech/piper`) — TTS.
- **Google Cloud** (`data/speech/googlecloud`) — STT + TTS через прокси (v1/v2, Chirp3-HD/WaveNet).

Retrofit/OkHttp-клиенты для всех провайдеров собираются одной фабрикой — `data/di/RetrofitFactory.kt` (`retrofitService(...)`: host-override из `EngineSettings` + Basic Auth + доп. интерсепторы), чтобы не копировать builder на каждый провайдер.

**VAD** (`data/speech/vad`): `AudioLevel` + `SilenceTracker` — конец фразы по паузе (порог дБ и длительность настраиваются в debug-экране).

Ответ бэка — **HTML со ссылками**. Для озвучки HTML стрипается в текст (`htmlToPlainText`), сам HTML показывается в шите; ссылки открываются webview. Модель допускает и структурный ответ в будущем (см. обсуждение в истории/`CLAUDE.md`).

## Assets

- `app/src/main/assets/avatar_{welcome,idle,listening,speaking,error}.mp4` — видео-аватар по состояниям (834×1112).
- `app/src/main/res/raw/anim_voice.json` — Lottie-свечение круга.
- `app/src/main/res/raw/nurai_cert.pem` — сертификат для сети.

## Дизайн

- `docs/figma/document.json` — полный дамп дизайна (Figma REST): все ноды/тексты/цвета/размеры.
- `docs/figma/index.md` — список экранов «Актуальный дизайн» с текстами.
- `docs/figma/screens/` — PNG-рендеры (частично; `_export.py` перезапускает выгрузку).

## Правила разработки

См. **`CLAUDE.md`** (корень) — обязательные принципы для нового кода: Clean Architecture (presentation/domain/data), SOLID, MVI, type-safe Compose Navigation (аргументы только через конструктор экрана, без хардкод-путей).

## Сборка / запуск

```
./gradlew :app:installDebug        # собрать и поставить на подключённое устройство
```
minSdk 24, compileSdk/targetSdk 36. Первым открывается debug-`SettingsScreen` → «Начать» → `MainScreen`.

## Тесты

`app/src/test` и `app/src/androidTest` — стандартный boilerplate без проектных тестов.
