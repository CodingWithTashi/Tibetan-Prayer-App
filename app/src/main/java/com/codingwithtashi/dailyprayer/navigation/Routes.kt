package com.codingwithtashi.dailyprayer.navigation

import kotlinx.serialization.Serializable

@Serializable data object Onboarding
@Serializable data object Main
@Serializable data class RoutineDetail(val routineId: Long)
@Serializable data class RoutineEditor(val routineId: Long? = null)
@Serializable data class Reader(val prayerId: Int, val routineId: Long? = null)
@Serializable data object Settings
@Serializable data object Notifications
@Serializable data object NowPlaying

/** The four tabs hosted inside the main scaffold (not navigation destinations). */
enum class MainTab { TODAY, LIBRARY, SAVED, LISTEN }

enum class ListenFilter { ALL, DOWNLOADED }
