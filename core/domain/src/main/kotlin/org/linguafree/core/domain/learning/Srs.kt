package org.linguafree.core.domain.learning

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Spaced repetition with a half-life regression (HLR) forgetting model
 * (Settles & Meeder, 2016): recall probability p = 2^(−Δ/h), where Δ is time since the
 * last practice and h is the lexeme's half-life. The half-life is predicted from practice
 * history: log2 h = θ0 + θ1·√(1+right) + θ2·√(1+wrong).
 *
 * The weights are fixed (fitted by hand to sensible intervals) because LinguaFree
 * learns nothing off-device: a new word lives ≈1 day, after 3 correct ≈2.5 days,
 * after 8 ≈6.5 days, after 15 ≈17 days; each mistake shortens it.
 */
object Srs {
    const val BIAS = -0.5
    const val RIGHT_WEIGHT = 1.4
    const val WRONG_WEIGHT = -1.0
    const val MIN_HALF_LIFE_DAYS = 0.25
    const val MAX_HALF_LIFE_DAYS = 365.0
    private const val DAY_MS = 86_400_000.0

    data class Memory(val right: Int, val wrong: Int, val halfLifeDays: Double, val lastSeen: Long)

    fun halfLife(right: Int, wrong: Int): Double {
        val log2h = BIAS + RIGHT_WEIGHT * sqrt(1.0 + right) + WRONG_WEIGHT * sqrt(1.0 + wrong)
        return 2.0.pow(log2h).coerceIn(MIN_HALF_LIFE_DAYS, MAX_HALF_LIFE_DAYS)
    }

    /** Predicted recall probability now (the lexeme's "strength", 0..1). */
    fun strength(memory: Memory?, now: Long): Double {
        if (memory == null || memory.lastSeen <= 0) return 0.0
        val elapsedDays = ((now - memory.lastSeen).coerceAtLeast(0)) / DAY_MS
        return 2.0.pow(-elapsedDays / memory.halfLifeDays).coerceIn(0.0, 1.0)
    }

    /** New memory after one practice of the lexeme. */
    fun update(memory: Memory?, correct: Boolean, now: Long): Memory {
        val right = (memory?.right ?: 0) + if (correct) 1 else 0
        val wrong = (memory?.wrong ?: 0) + if (correct) 0 else 1
        return Memory(right, wrong, halfLife(right, wrong), now)
    }

    /** When recall is predicted to drop to [threshold] (default 50%). */
    fun dueAt(memory: Memory, threshold: Double = 0.5): Long {
        val days = -memory.halfLifeDays * ln(threshold) / ln(2.0)
        return memory.lastSeen + (days * DAY_MS).toLong()
    }

    /**
     * Picks the weakest lexemes first. Never-seen words are excluded (they belong to new lessons).
     * Ties are broken by more mistakes, then by older practice.
     */
    fun <T> weakest(items: List<T>, memory: (T) -> Memory?, now: Long, limit: Int): List<T> =
        items.filter { (memory(it)?.lastSeen ?: 0L) > 0L }
            .sortedWith(
                compareBy<T> { strength(memory(it), now) }
                    .thenByDescending { memory(it)?.wrong ?: 0 }
                    .thenBy { memory(it)?.lastSeen ?: 0L }
            )
            .take(limit)

    /** Share of known lexemes with predicted recall ≥ 50% — shown as "words learned". */
    fun isLearned(memory: Memory?, now: Long): Boolean = memory != null && memory.right >= 2 && strength(memory, now) >= 0.5
}
