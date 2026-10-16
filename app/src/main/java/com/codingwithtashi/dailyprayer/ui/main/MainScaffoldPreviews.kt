package com.codingwithtashi.dailyprayer.ui.main

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.navigation.MainTab
import com.codingwithtashi.dailyprayer.ui.library.LibraryContent
import com.codingwithtashi.dailyprayer.ui.listen.ListenContent
import com.codingwithtashi.dailyprayer.ui.preview.PreviewData
import com.codingwithtashi.dailyprayer.ui.saved.SavedContent
import com.codingwithtashi.dailyprayer.ui.today.TodayContent
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

@Composable
private fun PreviewTab(tab: MainTab, listState: LazyListState, modifier: Modifier, language: AppLanguage) {
    when (tab) {
        MainTab.TODAY -> TodayContent(PreviewData.today(language), listState, {}, modifier)
        MainTab.LIBRARY -> LibraryContent(PreviewData.library(), listState, {}, {}, {}, modifier)
        MainTab.SAVED -> SavedContent(PreviewData.saved(), listState, {}, {}, {}, { _, _ -> }, {}, {}, modifier)
        MainTab.LISTEN -> ListenContent(PreviewData.listen(language), listState, "Listen", {}, {}, modifier)
    }
}

@Composable
private fun Sample(language: AppLanguage = AppLanguage.EN, dark: Boolean = false, tab: MainTab = MainTab.TODAY) {
    PrayerTheme(darkTheme = dark, language = language, reduceMotion = true) {
        MainScaffoldContent(
            tab = tab, miniPlayer = PreviewData.miniPlayer,
            onSelectTab = {}, onMiniPlayerOpen = {}, onMiniPlayerPlayPause = {}, onMiniPlayerClose = {},
            tabContent = { current, listState, modifier -> PreviewTab(current, listState, modifier, language) },
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
