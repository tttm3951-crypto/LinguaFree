package org.linguafree.core.domain

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.linguafree.core.domain.game.HeartsRules
import org.linguafree.core.domain.game.Rewards
import org.linguafree.core.domain.game.ShopItem
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.learning.LessonSession
import org.linguafree.core.domain.model.QuestPeriod
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.usecase.DailyMaintenanceUseCase
import org.linguafree.core.domain.usecase.FinishSessionUseCase
import org.linguafree.core.domain.usecase.NewWallet
import org.linguafree.core.domain.usecase.OpenChestUseCase
import org.linguafree.core.domain.usecase.SessionPlan
import org.linguafree.core.domain.usecase.ShopUseCase
import org.linguafree.core.domain.usecase.StartSessionUseCase

class UseCasesTest {
    private val time = FakeTime()
    private val content = FakeContent()
    private val progress = FakeProgress()
    private val game = FakeGame()
    private val settings = FakeSettings()
    private val start = StartSessionUseCase(content, progress, game, settings, time)
    private val finish = FinishSessionUseCase(content, progress, game, settings, time)
    private val daily = DailyMaintenanceUseCase(progress, game, settings, time)
    private val shop = ShopUseCase(game, settings, time)

    private suspend fun playLesson(nodeId: String, wrongFirst: Int = 0): LessonSession {
        val plan = start(SessionKind.Lesson, nodeId, "u1") as SessionPlan.Ready
        val s = LessonSession(plan.exercises, SessionKind.Lesson, time.now() - 60_000)
        var wrong = wrongFirst
        while (!s.isFinished) {
            s.submit(correct = wrong-- <= 0)
            s.next()
        }
        return s
    }

    @Test
    fun `a finished lesson gives XP, completes the node and starts the streak`() = runTest {
        daily()
        val session = playLesson("l1")
        val summary = finish(session, "l1", "u1")
        assertTrue(summary.passed)
        assertEquals(Rewards.LESSON_XP + Rewards.PERFECT_BONUS_XP + Rewards.SPEED_BONUS_XP, summary.xp)
        assertTrue(summary.streakExtended)
        assertEquals(1, summary.streak)
        assertTrue(progress.progress.value.getValue("l1").completed)
        assertTrue(game.walletNow().gems > NewWallet.gems)
    }

    @Test
    fun `mistakes in a lesson are saved for practice and cleared by practice`() = runTest {
        val session = playLesson("l1", wrongFirst = 1)
        finish(session, "l1", "u1")
        assertEquals(1, progress.mistakes.size)
        val practice = start(SessionKind.PracticeMistakes, null, null) as SessionPlan.Ready
        val s = LessonSession(practice.exercises, SessionKind.PracticeMistakes, time.now())
        while (!s.isFinished) { s.submit(true); s.next() }
        finish(s, null, null)
        assertTrue(progress.mistakes.isEmpty())
    }

    @Test
    fun `no hearts blocks lessons but not practice, and heart practice refills`() = runTest {
        game.updateWallet { it.copy(hearts = 0, heartsRefillAt = time.now() + HeartsRules.REFILL_MS) }
        assertTrue(start(SessionKind.Lesson, "l1", "u1") is SessionPlan.NoHearts)
        progress.updateMemory(listOf("hello" to true), time.now())
        val refill = start(SessionKind.HeartsRefill, null, null)
        assertTrue(refill is SessionPlan.Ready)
        val s = LessonSession((refill as SessionPlan.Ready).exercises, SessionKind.HeartsRefill, time.now())
        while (!s.isFinished) { s.submit(true); s.next() }
        assertTrue(finish(s, null, null).heartsRefilled)
        assertEquals(HeartsRules.MAX, game.walletNow().hearts)
    }

    @Test
    fun `no-hearts mode ignores an empty heart counter`() = runTest {
        game.updateWallet { it.copy(hearts = 0, heartsRefillAt = time.now() + HeartsRules.REFILL_MS) }
        settings.update { it.copy(heartsDisabled = true) }
        val plan = start(SessionKind.Lesson, "l1", "u1")
        assertTrue(plan is SessionPlan.Ready && !plan.heartsEnabled)
    }

    @Test
    fun `daily maintenance rolls three daily quests and the monthly badge`() = runTest {
        daily()
        val quests = game.questsNow()
        assertEquals(3, quests.count { it.period == QuestPeriod.Daily })
        assertEquals(1, quests.count { it.period == QuestPeriod.Monthly })
    }

    @Test
    fun `a missed day spends a freeze and keeps the streak`() = runTest {
        daily()
        finish(playLesson("l1"), "l1", "u1")
        game.updateWallet { it.copy(freezes = 1) }
        time.dateTime = time.dateTime.plusDays(2)
        val report = daily()
        assertEquals(1, report.freezesUsed)
        assertFalse(report.streakLost)
        assertEquals(0, game.walletNow().freezes)
        val day0 = time.today().minusDays(2)
        assertTrue(day0.plusDays(1) in game.streakNow().frozenDays)
    }

    @Test
    fun `shop sells freezes up to the limit and only for gems`() = runTest {
        game.updateWallet { it.copy(gems = 1000, freezes = 0) }
        assertEquals(ShopUseCase.Result.Ok, shop.buy(ShopItem.StreakFreeze))
        assertEquals(ShopUseCase.Result.Ok, shop.buy(ShopItem.StreakFreeze))
        assertTrue(shop.buy(ShopItem.StreakFreeze) is ShopUseCase.Result.NotAvailable)
        assertEquals(1000 - 2 * ShopItem.StreakFreeze.price, game.walletNow().gems)
        game.updateWallet { it.copy(gems = 10) }
        assertTrue(shop.buy(ShopItem.OutfitCrown) is ShopUseCase.Result.NotEnoughGems)
    }

    @Test
    fun `shop never sells hearts`() {
        assertTrue(ShopItem.entries.none { it.name.contains("Heart") })
    }

    @Test
    fun `buying a theme applies it and can't be bought twice`() = runTest {
        game.updateWallet { it.copy(gems = 1000) }
        settings.settings.test {
            assertEquals("Forest", awaitItem().accent)
            assertEquals(ShopUseCase.Result.Ok, shop.buy(ShopItem.ThemeOcean))
            assertEquals("Ocean", awaitItem().accent)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(shop.buy(ShopItem.ThemeOcean) is ShopUseCase.Result.NotAvailable)
        assertTrue("ThemeOcean" in game.inventoryFlow.value)
    }

    @Test
    fun `a chest gives gems once and completes its node`() = runTest {
        val before = game.walletNow().gems
        val gems = OpenChestUseCase(progress, game)("c1")
        assertTrue(gems in Rewards.CHEST_GEMS)
        assertEquals(before + gems, game.walletNow().gems)
        assertTrue(progress.progress.value.getValue("c1").completed)
    }
}
