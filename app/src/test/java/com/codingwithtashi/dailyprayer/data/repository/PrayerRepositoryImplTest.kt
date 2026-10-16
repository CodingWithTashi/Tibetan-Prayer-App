package com.codingwithtashi.dailyprayer.data.repository

import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.core.text.PrayerTextCache
import com.codingwithtashi.dailyprayer.data.FakeClock
import com.codingwithtashi.dailyprayer.data.inMemoryDb
import com.codingwithtashi.dailyprayer.data.local.PrayerDatabase
import com.codingwithtashi.dailyprayer.data.local.entity.PrayerEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerRepositoryImplTest {
    private lateinit var db: PrayerDatabase
    private lateinit var repo: PrayerRepositoryImpl
    private val clock = FakeClock()

    @Before fun setUp() {
        db = inMemoryDb(ApplicationProvider.getApplicationContext())
        repo = PrayerRepositoryImpl(db.prayerDao(), clock, PrayerTextCache())
    }

    @After fun tearDown() = db.close()

    private suspend fun add(slug: String, sort: Int = 1000, content: String = "") =
        db.prayerDao().insert(PrayerEntity(slug = slug, title = slug, sortOrder = sort, content = content)).toInt()

    @Test fun setSavedSetsAndClearsSavedAtAndLabel() = runTest {
        val id = add("a")
        repo.setSaved(id, true)
        repo.setSavedLabel(id, "  Morning  ")
        var p = repo.get(id)!!
        assertThat(p.isSaved).isTrue()
        assertThat(p.savedAt).isEqualTo(clock.now().toEpochMilli())
        assertThat(p.savedLabel).isEqualTo("Morning")
        repo.setSaved(id, false)
        p = repo.get(id)!!
        assertThat(p.isSaved).isFalse()
        assertThat(p.savedAt).isNull()
        assertThat(p.savedLabel).isNull()
    }

    @Test fun savedLabelIsTrimmedCappedAndBlankBecomesNull() = runTest {
        val id = add("a")
        repo.setSavedLabel(id, "x".repeat(40))
        assertThat(repo.get(id)!!.savedLabel).hasLength(28)
        repo.setSavedLabel(id, "   ")
        assertThat(repo.get(id)!!.savedLabel).isNull()
    }

    @Test fun observeSavedOrderedBySavedAt() = runTest {
        val a = add("a"); val b = add("b")
        repo.setSaved(b, true)
        clock.instant = clock.instant.plusSeconds(10)
        repo.setSaved(a, true)
        assertThat(repo.observeSaved().first().map { it.id }).containsExactly(b, a).inOrder()
    }

    @Test fun countNeverBelowZero() = runTest {
        val id = add("a")
        assertThat(repo.decrementCount(id)).isEqualTo(0)
        assertThat(repo.incrementCount(id)).isEqualTo(1)
        assertThat(repo.incrementCount(id)).isEqualTo(2)
        assertThat(repo.decrementCount(id)).isEqualTo(1)
        repo.setCount(id, -5)
        assertThat(repo.get(id)!!.count).isEqualTo(0)
    }

    @Test fun continueReadingWindow() = runTest {
        val a = add("a"); val b = add("b"); val c = add("c")
        assertThat(repo.observeContinueReading().first()).isNull()
        repo.setReadProgress(a, 0.01f)
        assertThat(repo.observeContinueReading().first()).isNull()
        repo.setReadProgress(a, 0.5f)
        clock.instant = clock.instant.plusSeconds(5)
        repo.setReadProgress(b, 0.98f)
        assertThat(repo.observeContinueReading().first()!!.id).isEqualTo(a)
        clock.instant = clock.instant.plusSeconds(5)
        repo.setReadProgress(c, 0.2f)
        assertThat(repo.observeContinueReading().first()!!.id).isEqualTo(c)
    }

    @Test fun getByIdsKeepsInputOrder() = runTest {
        val a = add("a", 3); val b = add("b", 1); val c = add("c", 2)
        assertThat(repo.getByIds(listOf(c, a, 999, b)).map { it.id }).containsExactly(c, a, b).inOrder()
        assertThat(repo.observeAll().first().map { it.id }).containsExactly(b, c, a).inOrder()
    }

    @Test fun upsertRemoteInsertsThenUpdates() = runTest {
        val id = repo.upsertRemote(" New prayer ", "body", "http://a", "En", "daily")
        val p = repo.get(id)!!
        assertThat(p.slug).isNull()
        assertThat(p.title).isEqualTo("New prayer")
        assertThat(p.category.key).isEqualTo("daily")
        assertThat(repo.upsertRemote("New prayer", "body2")).isEqualTo(id)
        assertThat(repo.get(id)!!.content).isEqualTo("body2")
        assertThat(repo.get(id)!!.audioUrl).isEqualTo("http://a")
    }

    @Test fun textIsSplit() = runTest {
        val id = add("a", content = "ཀ་ཁ། ག་ང། ".repeat(10))
        assertThat(repo.text(id).verses).isNotEmpty()
    }
}
