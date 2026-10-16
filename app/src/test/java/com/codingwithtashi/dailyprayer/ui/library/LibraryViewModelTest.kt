package com.codingwithtashi.dailyprayer.ui.library

import androidx.lifecycle.SavedStateHandle
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

internal fun testPrayer(
    id: Int,
    titleEn: String = "Prayer $id",
    title: String = "ཞུ$id",
    category: PrayerCategory = PrayerCategory.DAILY,
    saved: Boolean = false,
    label: String? = null,
) = Prayer(
    id = id, slug = null, title = title, titleEn = titleEn, content = "", category = category, sortOrder = id,
    audioUrl = "", hasAudio = false, isDownloaded = false, audioPath = null, audioDurationMs = null,
    isSaved = saved, savedLabel = label, savedAt = null, count = 0, lastReadProgress = 0f, lastReadAt = null, syllable = "ཞ",
)

class FakeLibraryRepo(initial: List<Prayer>) : PrayerRepository {
    val prayers = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<Prayer>> = prayers
    override fun observeSaved(): Flow<List<Prayer>> = prayers.map { l -> l.filter { it.isSaved } }
    override fun observeWithAudio() = TODO()
    override fun observe(id: Int) = TODO()
    override suspend fun get(id: Int) = prayers.value.firstOrNull { it.id == id }
    override suspend fun getByIds(ids: List<Int>) = TODO()
    override fun observeContinueReading() = TODO()
    override suspend fun setSaved(id: Int, saved: Boolean) = prayers.update(id) { it.copy(isSaved = saved) }
    override suspend fun setSavedLabel(id: Int, label: String?) = prayers.update(id) { it.copy(savedLabel = label) }
    override suspend fun setCount(id: Int, count: Int) = TODO()
    override suspend fun incrementCount(id: Int) = TODO()
    override suspend fun decrementCount(id: Int) = TODO()
    override suspend fun setReadProgress(id: Int, progress: Float) = TODO()
    override suspend fun text(id: Int): PrayerText = TODO()
    override suspend fun updateAudioDuration(id: Int, ms: Long) = TODO()
    override suspend fun upsertRemote(title: String, content: String, audioUrl: String?, titleEn: String?, category: String?) = TODO()
}

private fun MutableStateFlow<List<Prayer>>.update(id: Int, f: (Prayer) -> Prayer) {
    value = value.map { if (it.id == id) f(it) else it }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(vararg p: Prayer) = LibraryViewModel(SavedStateHandle(), FakeLibraryRepo(p.toList()))

    @Test fun queryMatchesEnglishCaseInsensitivelyAndTibetanSubstring() = runTest(UnconfinedTestDispatcher()) {
        val vm = vm(testPrayer(1, "Heart Sutra", "སྙིང་"), testPrayer(2, "Tara", "སྒྲོལ་མ"))
        backgroundScope.launch { vm.state.collect {} }
        vm.setQuery("heart")
        assertThat(vm.state.value.items.map { it.id }).containsExactly(1)
        vm.setQuery(" སྒྲོལ ")
        assertThat(vm.state.value.items.map { it.id }).containsExactly(2)
    }

    @Test fun categoryFilterAndEmptyResult() = runTest(UnconfinedTestDispatcher()) {
        val vm = vm(testPrayer(1, category = PrayerCategory.DAILY), testPrayer(2, category = PrayerCategory.SUTRA))
        backgroundScope.launch { vm.state.collect {} }
        vm.setCategory(PrayerCategory.SUTRA)
        assertThat(vm.state.value.items.map { it.id }).containsExactly(2)
        vm.setQuery("zzz")
        assertThat(vm.state.value.items).isEmpty()
        vm.setCategory(null)
        vm.setQuery("")
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
