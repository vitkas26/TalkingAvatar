# Правила разработки (обязательны для Claude)

Соблюдать ВСЕГДА при любых изменениях кода в этом проекте. Это не рекомендации — это требования. Если задача противоречит правилам, сначала предупредить и предложить путь в рамках правил.

Стек: Kotlin, Jetpack Compose, Orbit-MVI, Koin (DI), Compose Navigation, ExoPlayer.

---

## 1. Clean Architecture (Android)

Три слоя, зависимости направлены ТОЛЬКО внутрь (`presentation → domain ← data`):

- **domain** — бизнес-логика. Чистый Kotlin, без Android/Compose/фреймворков. Содержит: use case'ы (интеракторы), доменные модели, интерфейсы репозиториев. Ни на что не зависит.
- **data** — реализация репозиториев, сеть (Retrofit/OkHttp), локальные источники (DataStore), маппинг DTO↔domain-модель. Зависит от domain (реализует его интерфейсы).
- **presentation** — Compose UI + ViewModel (MVI). Зависит от domain (вызывает use case'ы). НЕ обращается к data напрямую.

Правила:
- UI/ViewModel НЕ знают про Retrofit, DTO, DataStore и т.п. — только доменные модели и use case'ы.
- Маппинг DTO→domain живёт в data, domain→UI-модель (если нужна) — в presentation.
- Зависимости между слоями — только через интерфейсы (domain объявляет, data реализует, Koin связывает).
- Никаких Android-типов (`Context`, `Uri`, ...) в domain.

## 2. SOLID

- **S** — один класс = одна ответственность. ViewModel не парсит сеть, репозиторий не форматирует UI-текст, use case делает одно действие.
- **O** — расширять через новые реализации интерфейсов, не правя существующие (напр. новый STT-движок = новый класс под общим интерфейсом).
- **L** — реализации интерфейса взаимозаменяемы, не ломают контракт.
- **I** — узкие интерфейсы под клиента, а не один «god interface».
- **D** — зависеть от абстракций (интерфейсов), инъекция через конструктор + Koin. Никаких `new`/синглтон-обращений к конкретике внутри классов.

## 3. MVI (Orbit)

- Каждый экран = свой ViewModel (`ContainerHost<State, SideEffect>`), состояние — **один** immutable `data class State`.
- Одноразовые события (навигация, тосты, снекбары) — через `SideEffect` (`postSideEffect`), НЕ через флаги в State.
- Мутация состояния только в `intent { reduce { ... } }`. Однонаправленный поток: UI шлёт события → VM редьюсит State → UI рендерит State.
- Бизнес-логики в Composable НЕТ. Composable только рисует State и шлёт события в VM (лямбды).
- VM вызывает use case'ы (domain), не репозитории/сеть напрямую.

## 4. Compose

- Composable-экраны stateless: принимают `state` + лямбды-события, не держат бизнес-состояние. State поднят в VM.
- Не передавать ViewModel вглубь дерева — только в корневой Composable экрана; ниже идут данные и колбэки.
- Побочные эффекты — через `LaunchedEffect`/`collectSideEffect`, не в теле композиции.

## 5. Навигация (Compose Navigation) — строго

- **Type-safe routes.** Маршруты — типизированные (Kotlin `@Serializable`-объекты/классы для typed Navigation, или единый sealed-реестр). Строк-путей руками НЕ писать.
- **НИКАКИХ хардкод-строк путей** в вызовах навигации (`navigate("home/123")` — запрещено). Только типизированные route-объекты / централизованные константы маршрутов.
- **Аргументы экрана передаются ТОЛЬКО через конструктор (параметры) Composable-экрана.** Экран объявляет свои входные данные как параметры функции. Извлечение аргументов из `NavBackStackEntry`/`SavedStateHandle` происходит В СЛОЕ НАВИГАЦИИ (внутри `composable<Route>{}` в NavHost), который достаёт типизированные аргументы и передаёт их явными параметрами в Composable-экрана. Экран НЕ читает `SavedStateHandle`/backstack сам.
- Пример правильно:
  ```kotlin
  // route
  @Serializable data class Answer(val questionId: String)

  // NavHost
  composable<Answer> { entry ->
      val args = entry.toRoute<Answer>()
      AnswerScreen(questionId = args.questionId)   // аргументы — в конструкторе экрана
  }

  // экран — аргументы только параметрами, без backstack внутри
  @Composable fun AnswerScreen(questionId: String) { ... }

  // навигация — типизированно, без строк
  navController.navigate(Answer(questionId = id))
  ```
- Каждый экран-destination — свой ViewModel (Koin, при необходимости scoped на destination). Аргументы конструктора экрана могут прокидываться в параметры VM через Koin `parametersOf`.

---

## Порядок применения к существующему коду

Слои: `domain/` (model, repository, usecase, gateway — чистый Kotlin, порты вроде `SttEngine`/`TtsEngine`), `data/` (repository impl, di, speech — реализации портов: Android/сетевые STT/TTS движки), `ui/` (экраны, VM, аватар-рендеринг). При новых задачах:
1. Новый код писать сразу по правилам (слои, типизированная навигация, args в конструкторе).
2. Существующий код рефакторить под правила по мере касания (не переписывать всё разом без задачи).
3. Не смешивать: не тянуть Retrofit/DTO в новые ViewModel — заводить use case + репозиторий-интерфейс.
