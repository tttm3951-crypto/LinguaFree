package org.linguafree.core.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.linguafree.core.domain.game.HeartsRules
import org.linguafree.core.domain.game.LeagueRules
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.learning.LessonSession
import org.linguafree.core.domain.learning.Srs
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.model.StreakState
import org.linguafree.core.domain.model.Wallet
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SrsTest {
    @Test
    fun `half-life grows with correct answers and shrinks with mistakes`() {
        assertTrue(Srs.halfLife(3, 0) > Srs.halfLife(1, 0))
        assertTrue(Srs.halfLife(3, 2) < Srs.halfLife(3, 0))
    }

    @Test
    fun `recall is one half after one half-life`() {
        val day = 86_400_000L
        val m = Srs.Memory(2, 0, 2.0, lastSeen = 1)
        assertEquals(0.5, Srs.strength(m, 1 + 2 * day), 1e-9)
        assertEquals(1.0, Srs.strength(m.copy(lastSeen = 5), 5), 1e-9)
    }

    @Test
    fun `weakest picks the least remembered seen lexemes`() {
        val mem = mapOf(
            "a" to Srs.Memory(5, 0, 10.0, 1),
            "b" to Srs.Memory(1, 2, 0.5, 1),
            "c" to Srs.Memory(0, 0, 1.0, 0), // never seen
        )
        assertEquals(listOf("b", "a"), Srs.weakest(listOf("a", "b", "c"), { mem[it] }, 86_400_000L * 3, 5))
    }
}

class StreakTest {
    private val d = LocalDate.of(2026, 10, 18)

    @Test
    fun `streak counts consecutive study days including today`() {
        assertEquals(3, StreakRules.current(setOf(d, d.minusDays(1), d.minusDays(2)), emptySet(), d))
        assertEquals(2, StreakRules.current(setOf(d.minusDays(1), d.minusDays(2)), emptySet(), d))
        assertEquals(0, StreakRules.current(setOf(d.minusDays(2)), emptySet(), d))
    }

    @Test
    fun `freezes save missed days up to the number held`() {
        val st = StreakState(setOf(d.minusDays(3), d.minusDays(4)), emptySet(), 2, d.minusDays(3))
        val r = StreakRules.applyMissedDays(st, 2, d)
        assertEquals(2, r.freezesUsed.size)
        assertFalse(r.broken)
        assertEquals(2, StreakRules.current(st.studyDays, r.state.frozenDays, d))
    }

    @Test
    fun `streak burns when freezes run out and no freezes are spent afterwards`() {
        val st = StreakState(setOf(d.minusDays(3)), emptySet(), 1, d.minusDays(3))
        val r = StreakRules.applyMissedDays(st, 1, d)
        assertTrue(r.broken)
        val later = StreakRules.applyMissedDays(r.state, r.freezesLeft + 2, d.plusDays(3))
        assertTrue(later.freezesUsed.isEmpty())
    }
}

class HeartsTest {
    private val t0 = 1_000_000L

    @Test
    fun `hearts are lost and regenerate every 30 minutes`() {
        var w = Wallet(0, 5, null, 0, 0)
        w = HeartsRules.loseHeart(w, t0)
        w = HeartsRules.loseHeart(w, t0 + 1)
        assertEquals(3, w.hearts)
        w = HeartsRules.regenerate(w, t0 + HeartsRules.REFILL_MS)
        assertEquals(4, w.hearts)
        w = HeartsRules.regenerate(w, t0 + 10 * HeartsRules.REFILL_MS)
        assertEquals(5, w.hearts)
        assertNull(w.heartsRefillAt)
    }

    @Test
    fun `hearts never go below zero`() {
        var w = Wallet(0, 1, null, 0, 0)
        repeat(3) { w = HeartsRules.loseHeart(w, t0) }
        assertEquals(0, w.hearts)
    }
}

class LessonSessionTest {
    private fun ex(n: Int) = (1..n).map { Exercise("e$it", "n", it, ExerciseType.SelectTranslation, "t$it", answers = listOf("a")) }

    @Test
    fun `wrong exercise returns at the end and costs a heart`() {
        val s = LessonSession(ex(3), SessionKind.Lesson, 0)
        val o = s.submit(false)
        assertTrue(o.heartLost && o.willRetry)
        s.next()
        assertEquals("e2", s.current!!.exercise.id)
        s.submit(true); s.next()
        s.submit(true); s.next()
        assertEquals("e1", s.current!!.exercise.id)
        s.submit(true); s.next()
        assertTrue(s.isFinished && s.passed)
        assertEquals(66, s.accuracy)
    }

    @Test
    fun `combo milestone every five correct answers`() {
        val s = LessonSession(ex(10), SessionKind.Lesson, 0)
        val milestones = (1..10).mapNotNull { s.submit(true).comboMilestone.also { s.next() } }
        assertEquals(listOf(5, 10), milestones)
    }

    @Test
    fun `unit test fails after too many mistakes and never retries`() {
        val s = LessonSession(ex(10), SessionKind.UnitTest, 0)
        repeat(4) { val o = s.submit(false); assertFalse(o.willRetry); assertFalse(o.heartLost); s.next() }
        assertTrue(s.failed)
        assertFalse(s.passed)
    }
}

class LeagueTest {
    private val zone = ZoneId.of("Asia/Almaty")

    @Test
    fun `thirty participants with the learner`() {
        val st = LeagueRules.standing(2, "Я", 100, ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, zone))
        assertEquals(30, st.entries.size)
        assertEquals(1, st.entries.count { it.isUser })
    }

    @Test
    fun `bots are deterministic for a week`() {
        val now = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, zone)
        assertEquals(LeagueRules.standing(4, "Я", 50, now).entries, LeagueRules.standing(4, "Я", 50, now).entries)
    }

    @Test
    fun `zones promote top seven and relegate bottom five`() {
        assertEquals(LeagueRules.Zone.Promotion, LeagueRules.zone(7, 3))
        assertEquals(LeagueRules.Zone.Safe, LeagueRules.zone(8, 3))
        assertEquals(LeagueRules.Zone.Demotion, LeagueRules.zone(26, 3))
        assertEquals(LeagueRules.Zone.Safe, LeagueRules.zone(26, 0))
        assertEquals(LeagueRules.Zone.Safe, LeagueRules.zone(1, LeagueRules.TIERS - 1))
    }

    @Test
    fun `a huge week wins first place and promotion`() {
        val r = LeagueRules.finish(LocalDate.of(2026, 10, 14), 0, "Я", 100_000, zone)!!
        assertEquals(1, r.rank)
        assertEquals(1, r.newTier)
        assertTrue(r.gems > 0)
    }
}
