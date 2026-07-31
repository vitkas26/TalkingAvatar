package kg.nurtelecom.o.talkingavatar.ui.conversation.navigation

import kotlinx.serialization.Serializable

// Типизированные роуты разговора (см. CLAUDE.md — никаких хардкод-строк в navigate()).
// Без аргументов: вся сессия (язык/ответ/шит) живёт в MainViewModel, общем на все три экрана.
@Serializable
data object Welcome

@Serializable
data object Listening

@Serializable
data object Speaking
