package org.linguafree.core.domain.game

import org.linguafree.core.domain.model.StreakState
import java.time.LocalDate

/**
 * Day streak: +1 for every local calendar day with a finished lesson.
 * A missed day automatically spends a streak freeze (hold up to [MAX_FREEZES]);
 * frozen days keep the streak alive but don't add to its length.
 * The streak burns at local midnight if a day is missed without a freeze.
 */
object StreakRules {
    const val MAX_FREEZES = 2

    /** Current streak length as of [today]. Today counts once studied; until then yesterday's streak is shown. */
    fun current(study: Set<LocalDate>, frozen: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in study) today else today.minusDays(1)
        var count = 0
        while (day in study || day in frozen) {
            if (day in study) count++
            day = day.minusDays(1)
        }
        return count
    }

    fun studiedToday(study: Set<LocalDate>, today: LocalDate) = today in study

    /** The streak is "at risk" when it is alive but today has no lesson yet. */
    fun atRisk(study: Set<LocalDate>, frozen: Set<LocalDate>, today: LocalDate) =
        today !in study && current(study, frozen, today) > 0

    data class FreezeResult(val state: StreakState, val freezesLeft: Int, val freezesUsed: List<LocalDate>, val broken: Boolean)

    /**
     * Processes days missed since the last check (up to yesterday): spends freezes on each missed day
     * while the streak is alive. Called on app start and after midnight.
     */
    fun applyMissedDays(state: StreakState, freezes: Int, today: LocalDate): FreezeResult {
        val yesterday = today.minusDays(1)
        val lastActive = (state.studyDays + state.frozenDays).filter { !it.isAfter(yesterday) }.maxOrNull()
        val from = listOfNotNull(state.checkedThrough?.plusDays(1), lastActive?.plusDays(1)).maxOrNull()
        if (lastActive == null || from == null || from.isAfter(yesterday)) {
            return FreezeResult(state.copy(checkedThrough = yesterday), freezes, emptyList(), broken = false)
        }
        // Freezes are spent only while the streak is alive: the day before the gap must be active.
        val beforeGap = from.minusDays(1)
        if (beforeGap !in state.studyDays && beforeGap !in state.frozenDays) {
            return FreezeResult(state.copy(checkedThrough = yesterday), freezes, emptyList(), broken = false)
        }
        var left = freezes
        val used = mutableListOf<LocalDate>()
        var broken = false
        var day: LocalDate = from
        while (!day.isAfter(yesterday)) {
            if (day !in state.studyDays && day !in state.frozenDays) {
                if (left > 0) {
                    used += day; left--
                } else {
                    broken = true
                    break
                }
            }
            day = day.plusDays(1)
        }
        val newFrozen = state.frozenDays + used
        val length = current(state.studyDays, newFrozen, today)
        return FreezeResult(
            state.copy(frozenDays = newFrozen, checkedThrough = yesterday, longest = maxOf(state.longest, length)),
            left, used, broken,
        )
    }

    /** Records a finished lesson today. Returns the new state and whether the streak grew. */
    fun recordStudy(state: StreakState, today: LocalDate): Pair<StreakState, Boolean> {
        if (today in state.studyDays) return state to false
        val study = state.studyDays + today
        val length = current(study, state.frozenDays, today)
        return state.copy(studyDays = study, longest = maxOf(state.longest, length)) to true
    }
}
