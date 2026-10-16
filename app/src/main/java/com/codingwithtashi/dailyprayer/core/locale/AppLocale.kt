package com.codingwithtashi.dailyprayer.core.locale

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLocale {
    fun apply(lang: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
    }

    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!locales.isEmpty) locales[0]?.toLanguageTag() else Locale.getDefault().toLanguageTag()
        return AppLanguage.fromTag(tag)
    }

    /** For receivers/services where the per-app locale is not applied to the context (API < 33). */
    fun localizedContext(ctx: Context, lang: AppLanguage): Context {
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(Locale.forLanguageTag(lang.tag))
        return ctx.createConfigurationContext(config)
    }
}
