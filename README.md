# TalkingAvatar

Android-приложение (`kg.nurtelecom.o.talkingavatar`) — голосовой ассистент с анимированным аватаром: пользователь задаёт вопрос голосом, приложение распознаёт речь, получает ответ и озвучивает его через синтез речи, синхронизируя с анимацией аватара.

## Стек

- **Язык/UI:** Kotlin, Jetpack Compose
- **Архитектура:** Orbit-MVI (ViewModel как `ContainerHost`, состояние + side-effects)
- **DI:** Koin
- **Сеть:** Retrofit + OkHttp
- **Хранилище:** DataStore Preferences (токены)
- **Анимация аватара:** Lottie Compose
- **Распознавание речи:** нативный Android `SpeechRecognizer`
- **Синтез речи:** нативный Android `TextToSpeech` + `MediaPlayer`

## Структура проекта

```
app/src/main/java/kg/nurtelecom/o/talkingavatar/
├── MainActivity.kt              — единственная Activity, хостит Compose UI
├── TalkingAvatarApp.kt          — Application, инициализация Koin
├── data/
│   ├── api/ApiService.kt        — Retrofit-интерфейс, эндпоинт POST /ask
│   ├── di/MainModule.kt         — Koin-модуль: ViewModel + Retrofit ApiService
│   └── models/QuestionRequest.kt — QuestionRequest, AnswerResponse
├── network/
│   ├── AuthInterceptor.kt       — OkHttp-интерцептор для auth-заголовка
│   └── TokenManager.kt          — хранение access/refresh токенов в DataStore
└── ui/
    ├── mainScreen/
    │   ├── MainScreen.kt        — Compose UI: кнопки, распознавание речи, воспроизведение TTS
    │   └── MainVM.kt            — MainViewModel: состояние (MainState) и side-effects (MainSideEffect)
    ├── theme/                   — Color.kt, Theme.kt, Type.kt
    └── utils/
        ├── AudioPlayer.kt       — обёртка TextToSpeech + MediaPlayer
        └── PulseIndicator.kt    — Compose-анимация "думает" (пульсирующие кольца)
```

## Поток данных (end-to-end)

1. Пользователь жмёт «Задать вопрос» → запрашивается разрешение `RECORD_AUDIO`.
2. `MainViewModel.startListening()` переводит состояние в `isListening=true`, эмитит side-effect `StartSpeechRecognition`.
3. `MainScreen` запускает `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (язык `ru-RU`).
4. Распознанный текст возвращается через `speechLauncher` → `viewModel.onSpeechResult(question)`.
5. `MainViewModel` вызывает `ApiService.askQuestion(QuestionRequest(question))`.
6. Состояние обновляется: `answer`, `isPreparing=true` — на экране идёт анимация ожидания (`PulseIndicator`).
7. Эмитится side-effect `SpeakAnswer` → `MainScreen` вызывает `AudioPlayer.play(text)`.
8. `AudioPlayer` синтезирует текст в WAV-файл через `TextToSpeech.synthesizeToFile`, проигрывает через `MediaPlayer`, сообщает о старте/окончании/ошибке колбэками.
9. Пока `isSpeaking=true`, Lottie-анимация (`talking_man.json`) проигрывает сегмент движения рта (диапазон прогресса 0.3–0.6); в остальное время — состояние покоя.
10. Кнопка «Стоп» останавливает воспроизведение и сбрасывает состояние в любой момент.

## Ключевые файлы

| Файл | Роль |
|---|---|
| `MainActivity.kt` | Единственная Activity, точка входа, хостит `MainScreen()` |
| `TalkingAvatarApp.kt` | `Application`, стартует Koin с `mainModule` |
| `MainScreen.kt` | Compose UI, обработка разрешений, запуск `SpeechRecognizer`, вызов `AudioPlayer` |
| `MainVM.kt` | `MainViewModel` — состояние `MainState` (isListening, question, answer, isSpeaking, isPreparing, error) и side-effects `MainSideEffect` |
| `AudioPlayer.kt` | Синтез речи в файл + воспроизведение через `MediaPlayer`, выбор русского мужского голоса |
| `PulseIndicator.kt` | Кастомная Compose-анимация "думает" |
| `ApiService.kt` | Retrofit-интерфейс с методом `askQuestion`, эндпоинт `POST /ask` |
| `MainModule.kt` | Koin-модуль: собирает `MainViewModel` и Retrofit `ApiService` |
| `QuestionRequest.kt` | Data-классы `QuestionRequest(question: String)`, `AnswerResponse(answer: String)` |
| `AuthInterceptor.kt` | OkHttp-интерцептор, добавляет auth-заголовок к запросам |
| `TokenManager.kt` | Хранение access/refresh токенов через DataStore Preferences |

## Assets

- `app/src/main/assets/talking_man.json` — Lottie-анимация аватара
- `app/src/main/assets/talking_cat.json` (в `res/raw`) — альтернативный Lottie-файл
- `app/src/main/assets/model_nooruz.glb` — glTF-модель
- `app/src/main/res/drawable/ic_thinking.png` — иконка для `PulseIndicator`

## Тесты

`app/src/test` и `app/src/androidTest` содержат стандартный boilerplate (`ExampleUnitTest`, `ExampleInstrumentedTest`) без специфичных для проекта тестов.
