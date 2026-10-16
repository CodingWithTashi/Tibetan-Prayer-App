package com.codingwithtashi.dailyprayer.data.seed

import com.codingwithtashi.dailyprayer.DataSourceTitle
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Modifier
import org.junit.Test

class PrayerCatalogTest {
    private val all = PrayerCatalog.all

    @Test fun hasTwentyFourEntries() = assertThat(all).hasSize(24)

    @Test fun slugsAreUnique() = assertThat(all.map { it.slug }.toSet()).hasSize(24)

    @Test fun titlesAreTrimmed() {
        all.forEach { assertThat(it.title).isEqualTo(it.title.trim()) }
    }

    @Test fun exactlyThirteenHaveAudio() = assertThat(all.count { it.audioUrl.isNotBlank() }).isEqualTo(13)

    @Test fun everyDataSourceTitleIsReferenced() {
        val titles = DataSourceTitle::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { f -> f.isAccessible = true; (f.get(null) as String).trim() }
            .toSet()
        assertThat(titles).isNotEmpty()
        assertThat(all.map { it.title }.toSet()).isEqualTo(titles)
    }
}
