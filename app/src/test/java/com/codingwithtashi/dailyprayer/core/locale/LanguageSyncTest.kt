package com.codingwithtashi.dailyprayer.core.locale

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LanguageSyncTest {
    @Test fun differentLocaleWins() {
        assertThat(AppLanguage.syncTarget("bo", AppLanguage.EN)).isEqualTo(AppLanguage.BO)
        assertThat(AppLanguage.syncTarget("en-US", AppLanguage.BO)).isEqualTo(AppLanguage.EN)
    }

    @Test fun sameOrMissingOrUnknownLocaleChangesNothing() {
        assertThat(AppLanguage.syncTarget("bo", AppLanguage.BO)).isNull()
        assertThat(AppLanguage.syncTarget(null, AppLanguage.BO)).isNull()
        assertThat(AppLanguage.syncTarget("fr", AppLanguage.BO)).isNull()
    }
}
