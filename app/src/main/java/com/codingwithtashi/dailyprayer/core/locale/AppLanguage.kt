package com.codingwithtashi.dailyprayer.core.locale

/**
 * The two UI languages the app ships. [tag] is the BCP-47 tag used for per-app locales
 * and resource qualifiers (`values/` = en, `values-bo/` = bo).
 */
enum class AppLanguage(val tag: String) {
    EN("en"),
    BO("bo");

    /** True when numbers that represent counts should be rendered with Tibetan digits (༠-༩). */
    val usesTibetanDigits: Boolean get() = this == BO

    companion object {
        /**
         * Language to persist so notifications match the UI: the per-app locale [appLocaleTag] when it
         * is a shipped language different from [stored], else null (nothing to change).
         */
        fun syncTarget(appLocaleTag: String?, stored: AppLanguage): AppLanguage? {
            val fromLocale = entries.firstOrNull { appLocaleTag != null && appLocaleTag.startsWith(it.tag, ignoreCase = true) }
            return fromLocale?.takeIf { it != stored }
        }

        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { tag != null && tag.startsWith(it.tag, ignoreCase = true) } ?: EN
    }
}
