package com.codingwithtashi.dailyprayer.data.seed

object OnboardingDefaults {
    val CANDIDATE_SLUGS: List<String> = listOf(
        "gyun_chak_sum",
        "kyab_do_sam_kay",
        "sherab_nyingpo",
        "dolma",
        "kha_nyam_ma",
        "yanlak_dunpa",
        "ngowa_monlam_one",
    )
    val DEFAULT_PICK_SLUGS: List<String> = CANDIDATE_SLUGS.take(3)

    /** 06:30 */
    const val DEFAULT_TIME_MINUTES = 390
}
