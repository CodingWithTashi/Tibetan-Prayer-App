package com.codingwithtashi.dailyprayer.ui.library

import androidx.lifecycle.SavedStateHandle
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.ui.FakePrayerRepo
import com.codingwithtashi.dailyprayer.ui.testPrayer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(vararg p: Prayer) = LibraryViewModel(SavedStateHandle(), FakePrayerRepo(p.toList()))

    @Test fun queryMatchesEnglishCaseInsensitivelyAndTibetanSubstring() = runTest(UnconfinedTestDispatcher()) {
        val vm = vm(testPrayer(1, "Heart Sutra", "སྙིང་"), testPrayer(2, "Tara", "སྒྲོལ་མ"))
        backgroundScope.launch { vm.state.collect {} }
        vm.onEvent(LibraryEvent.SetQuery("heart"))
        assertThat(vm.state.value.items.map { it.id }).containsExactly(1)
        vm.onEvent(LibraryEvent.SetQuery(" སྒྲོལ "))
        assertThat(vm.state.value.items.map { it.id }).containsExactly(2)
    }

    @Test fun categoryFilterAndEmptyResult() = runTest(UnconfinedTestDispatcher()) {
        val vm = vm(testPrayer(1, category = PrayerCategory.DAILY), testPrayer(2, category = PrayerCategory.SUTRA))
        backgroundScope.launch { vm.state.collect {} }
        vm.onEvent(LibraryEvent.SetCategory(PrayerCategory.SUTRA))
        assertThat(vm.state.value.items.map { it.id }).containsExactly(2)
        vm.onEvent(LibraryEvent.SetQuery("zzz"))
        assertThat(vm.state.value.items).isEmpty()
        vm.onEvent(LibraryEvent.SetCategory(null))
        vm.onEvent(LibraryEvent.SetQuery(""))
        assertThat(vm.state.value.items).hasSize(2)
    }

    @Test fun otherChipOnlyWhenNeeded() = runTest(UnconfinedTestDispatcher()) {
        val without = vm(testPrayer(1))
        backgroundScope.launch { without.state.collect {} }
        assertThat(without.state.value.categories).doesNotContain(PrayerCategory.OTHER)
        assertThat(without.state.value.categories.first()).isNull()
        val with = vm(testPrayer(1), testPrayer(2, category = PrayerCategory.OTHER))
        backgroundScope.launch { with.state.collect {} }
        assertThat(with.state.value.categories.last()).isEqualTo(PrayerCategory.OTHER)
    }
}
