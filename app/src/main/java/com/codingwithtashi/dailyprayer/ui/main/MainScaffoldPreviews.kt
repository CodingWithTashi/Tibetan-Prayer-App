package com.codingwithtashi.dailyprayer.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.navigation.ListenFilter
import com.codingwithtashi.dailyprayer.navigation.MainTab
import com.codingwithtashi.dailyprayer.ui.components.MiniPlayerUi
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

private val sampleMini = MiniPlayerUi("ཀ", "Refuge and Bodhicitta", "Verse 2 / 8", isPlaying = true, progress = 0.4f)

@Composable
private fun Sample(language: AppLanguage = AppLanguage.EN, dark: Boolean = false, tab: MainTab = MainTab.TODAY) {
    PrayerTheme(darkTheme = dark, language = language, reduceMotion = true) {
        MainScaffoldContent(
            tab = tab, listenFilter = ListenFilter.ALL, miniPlayer = sampleMini,
            onSelectTab = {}, onListenFilterChange = {}, onOpenSettings = {}, onOpenNotifications = {},
            onOpenRoutine = {}, onNewRoutine = {}, onOpenReader = { _, _ -> }, onOpenPlayer = {},
            onMiniPlayerOpen = {}, onMiniPlayerPlayPause = {}, onMiniPlayerClose = {},
        )
    }
}

@Preview(name = "Phone", widthDp = 390, heightDp = 844)
@Composable
private fun PhonePreview() = Sample()

@Preview(name = "Phone dark", widthDp = 390, heightDp = 844)
@Composable
private fun PhoneDarkPreview() = Sample(dark = true, tab = MainTab.LIBRARY)

@Preview(name = "Tablet rail", widthDp = 840, heightDp = 1000)
@Composable
private fun TabletPreview() = Sample()

@Preview(name = "Phone bo", widthDp = 390, heightDp = 844, locale = "bo")
@Composable
private fun PhoneBoPreview() = Sample(AppLanguage.BO)

@Preview(name = "Tablet rail bo", widthDp = 840, heightDp = 1000, locale = "bo")
@Composable
private fun TabletBoPreview() = Sample(AppLanguage.BO, tab = MainTab.SAVED)
