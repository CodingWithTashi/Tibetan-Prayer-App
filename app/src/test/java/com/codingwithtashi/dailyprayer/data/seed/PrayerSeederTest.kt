package com.codingwithtashi.dailyprayer.data.seed

import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.ui.FakeClock
import com.codingwithtashi.dailyprayer.data.inMemoryDb
import com.codingwithtashi.dailyprayer.data.local.PrayerDatabase
import com.codingwithtashi.dailyprayer.data.local.entity.PrayerEntity
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepositoryImpl
import com.codingwithtashi.dailyprayer.data.tempDataStore
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerSeederTest {
    private lateinit var db: PrayerDatabase
    private lateinit var seeder: PrayerSeeder

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = inMemoryDb(ctx)
        seeder = PrayerSeeder(
            ctx, db, db.prayerDao(), db.notificationDao(), UserPreferencesRepositoryImpl(tempDataStore()), FakeClock(),
        )
    }

    @After fun tearDown() = db.close()

    @Test fun freshDbGetsAllRows() = runTest {
        seeder.seed()
        val rows = db.prayerDao().getAllOnce()
        assertThat(rows).hasSize(24)
        assertThat(rows.map { it.slug }.toSet()).hasSize(24)
        assertThat(db.notificationDao().count()).isEqualTo(1)
    }

    /** Onboarding's "Start a morning practice" list resolves these slugs against the seeded rows. */
    @Test fun everyOnboardingCandidateIsSeeded() = runTest {
        seeder.seed()
        val bySlug = db.prayerDao().getAllOnce().associateBy { it.slug }
        assertThat(OnboardingDefaults.CANDIDATE_SLUGS).hasSize(7)
        assertThat(OnboardingDefaults.CANDIDATE_SLUGS.toSet()).hasSize(7)
        OnboardingDefaults.CANDIDATE_SLUGS.forEach { slug ->
            val row = bySlug[slug]
            assertWithMessage(slug).that(row).isNotNull()
            assertWithMessage(slug).that(row!!.title).isNotEmpty()
            assertWithMessage(slug).that(row.titleEn).isNotEmpty()
            assertWithMessage(slug).that(row.content).isNotEmpty()
        }
        assertThat(OnboardingDefaults.CANDIDATE_SLUGS).containsAtLeastElementsIn(OnboardingDefaults.DEFAULT_PICK_SLUGS)
    }

    @Test fun secondRunChangesNothing() = runTest {
        seeder.seed()
        val first = db.prayerDao().getAllOnce()
        seeder.seed()
        assertThat(db.prayerDao().getAllOnce()).isEqualTo(first)
        assertThat(db.notificationDao().count()).isEqualTo(1)
    }

    @Test fun unsluggedRowWithStaticTitleGetsSlugAndKeepsUserData() = runTest {
        val target = PrayerCatalog.all.first { it.slug == "dolma" }
        val id = db.prayerDao().insert(
            PrayerEntity(slug = null, title = target.title + " ", count = 7, isSaved = true, savedAt = 5L),
        ).toInt()
        seeder.seed()
        val rows = db.prayerDao().getAllOnce()
        assertThat(rows).hasSize(24)
        val row = db.prayerDao().get(id)!!
        assertThat(row.slug).isEqualTo("dolma")
        assertThat(row.count).isEqualTo(7)
        assertThat(row.isSaved).isTrue()
        assertThat(row.titleEn).isEqualTo(target.titleEn)
    }

    @Test fun unknownTitleRowStaysOther() = runTest {
        val id = db.prayerDao().insert(PrayerEntity(slug = null, title = "unknown remote prayer")).toInt()
        seeder.seed()
        assertThat(db.prayerDao().getAllOnce()).hasSize(25)
        val row = db.prayerDao().get(id)!!
        assertThat(row.slug).isNull()
        assertThat(row.category).isEqualTo("other")
    }

    @Test fun validateAudioFilesClearsMissingFiles() = runTest {
        seeder.seed()
        val id = db.prayerDao().getAllOnce().first().id
        db.prayerDao().markDownloadedWithDuration(id, "/nonexistent/file.mp3", null)
        seeder.validateAudioFiles()
        val row = db.prayerDao().get(id)!!
        assertThat(row.isDownloaded).isFalse()
        assertThat(row.audioPath).isNull()
    }
}
