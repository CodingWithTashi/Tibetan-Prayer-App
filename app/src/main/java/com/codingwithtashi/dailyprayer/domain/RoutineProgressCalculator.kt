package com.codingwithtashi.dailyprayer.domain

data class RoutineProgress(
    val doneIds: Set<Int>,
    val doneCount: Int,
    val total: Int,
    val isComplete: Boolean,
    val nextPrayerId: Int?,
    val fraction: Float,
)

enum class StartLabel { START_READING, CONTINUE, READ_AGAIN }

object RoutineProgressCalculator {
    fun compute(prayerIdsInOrder: List<Int>, completedPrayerIds: Set<Int>): RoutineProgress {
        val total = prayerIdsInOrder.size
        val doneIds = prayerIdsInOrder.filterTo(LinkedHashSet()) { it in completedPrayerIds }
        val doneCount = doneIds.size
        val complete = total > 0 && doneCount == total
        val next = when {
            total == 0 -> null
            complete -> prayerIdsInOrder.first()
            else -> prayerIdsInOrder.first { it !in doneIds }
        }
        return RoutineProgress(
            doneIds = doneIds,
            doneCount = doneCount,
            total = total,
            isComplete = complete,
            nextPrayerId = next,
            fraction = if (total == 0) 0f else doneCount.toFloat() / total,
        )
    }

    fun startLabel(progress: RoutineProgress): StartLabel = when {
        progress.isComplete -> StartLabel.READ_AGAIN
        progress.doneCount > 0 -> StartLabel.CONTINUE
        else -> StartLabel.START_READING
    }
}
