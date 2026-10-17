package org.linguafree.core.domain.usecase

import kotlinx.coroutines.flow.first
import org.linguafree.core.domain.game.Achievements
import org.linguafree.core.domain.game.HeartsRules
import org.linguafree.core.domain.game.LeagueRules
import org.linguafree.core.domain.game.QuestRules
import org.linguafree.core.domain.game.Rewards
import org.linguafree.core.domain.game.ShopItem
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.learning.LessonSession
import org.linguafree.core.domain.learning.SessionComposer
import org.linguafree.core.domain.learning.Srs
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.LessonResult
import org.linguafree.core.domain.model.NodeType
import org.linguafree.core.domain.model.QuestPeriod
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.model.Wallet
import org.linguafree.core.domain.path.PathPlanner
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.ProgressRepository
import org.linguafree.core.domain.repository.SettingsRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/** Injectable clock so rules can be tested with fixed time. */
interface TimeProvider {
    fun now(): Long
    fun zone(): ZoneId
    fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(zone()).toLocalDate()
    fun zoned(): ZonedDateTime = Instant.ofEpochMilli(now()).atZone(zone())
    fun dayStart(day: LocalDate = today()): Long = day.atStartOfDay(zone()).toInstant().toEpochMilli()
}

object SystemTime : TimeProvider {
    override fun now() = System.currentTimeMillis()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Counter keys stored by [GameRepository]. */
object Counters {
    const val STORIES = "stories"
    const val QUESTS_DONE = "quests_done"
    const val MONTHLY_BADGES = "monthly_badges"
}

/** What a session needs to start, or why it can't. */
sealed interface SessionPlan {
    data class Ready(val kind: SessionKind, val exercises: List<Exercise>, val heartsEnabled: Boolean) : SessionPlan
    /** No hearts left: offer free practice or waiting. */
    data class NoHearts(val msToNextHeart: Long?) : SessionPlan
    data object Empty : SessionPlan
}

class StartSessionUseCase(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val game: GameRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(kind: SessionKind, nodeId: String?, unitId: String?): SessionPlan {
        val now = time.now()
        val s = settings.current()
        val heartsEnabled = !s.heartsDisabled && kind.spendsHearts
        if (heartsEnabled) {
            val w = game.updateWallet { HeartsRules.regenerate(it, now) }
            if (w.hearts <= 0) return SessionPlan.NoHearts(HeartsRules.msToNext(w, now))
        }
        val course = content.course()
        val nodeProgress = progress.nodeProgressNow()
        val order = course.sections.flatMap { sec -> sec.units.flatMap { it.nodes } }
        val prevLesson = nodeId?.let { id ->
            val i = order.indexOfFirst { it.id == id }
            order.take(maxOf(i, 0)).lastOrNull { it.type == NodeType.Lesson }
        }
        val lastAccuracy = prevLesson?.let { nodeProgress[it.id]?.lastAccuracy }
        val lexemes = content.lexemes().associate { it.id to (it.word to it.translation) }
        val composer = SessionComposer(
            course = course,
            exercisesByNode = content.exercisesByNode(),
            memory = progress.memory(),
            completedNodes = nodeProgress.filterValues { it.completed }.keys,
            lexemeTranslations = lexemes,
            now = now,
            speakingEnabled = s.speakingEnabled(now),
            listeningEnabled = s.listeningEnabled(now),
        )
        val list = composer.compose(kind, nodeId, unitId, lastAccuracy, progress.mistakeIds(30))
        return if (list.isEmpty()) SessionPlan.Empty else SessionPlan.Ready(kind, list, heartsEnabled)
    }
}

/** Everything shown on the lesson summary screen. */
data class SessionSummary(
    val passed: Boolean,
    val xp: Int,
    val bonusXp: Int,
    val gems: Int,
    val accuracy: Int,
    val durationMs: Long,
    val mistakes: Int,
    val streak: Int,
    val streakExtended: Boolean,
    val dailyGoalXp: Int,
    val todayXp: Int,
    val questsCompleted: List<String>,
    val achievementsUnlocked: List<String>,
    val heartsRefilled: Boolean,
    val legendary: Boolean,
    val unitSkipped: Boolean,
    val placementUnits: Int,
)

class FinishSessionUseCase(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
    private val game: GameRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    suspend operator fun invoke(
        session: LessonSession,
        nodeId: String?,
        unitId: String?,
        /** Placement: unit ids per exercise, to compute the start point. */
        placementUnits: List<String?> = emptyList(),
    ): SessionSummary {
        val now = time.now()
        val today = time.today()
        val kind = session.kind
        val s = settings.current()
        val passed = if (kind == SessionKind.Placement) true else session.passed
        val reward = Rewards.forSession(kind, session.mistakes, session.durationMs(now), passed, session.total)
        val result = LessonResult(
            nodeId = nodeId, kind = kind, xp = reward.totalXp, accuracy = session.accuracy,
            durationMs = session.durationMs(now), mistakes = session.mistakes, correct = session.correctFirstTry,
            total = session.total, bestCombo = session.bestCombo, listeningDone = session.listeningDone,
            speakingDone = session.speakingDone, finishedAt = now, passed = passed,
        )
        progress.addResult(result)
        progress.updateMemory(session.lexemeResults, now)
        progress.addMistakes(session.wrongExercises, now)
        if (kind == SessionKind.PracticeMistakes || kind.isPractice) {
            // Exercises solved in practice leave the mistakes list.
            progress.removeMistakes(session.answeredIds - session.wrongExercises)
        }

        var unitSkipped = false
        var placementSkip = 0
        val course = content.course()
        if (passed) when (kind) {
            SessionKind.Lesson, SessionKind.Story -> progress.completeNodes(listOfNotNull(nodeId), session.accuracy)
            SessionKind.Legendary -> progress.completeNodes(listOfNotNull(nodeId), session.accuracy, legendary = true)
            SessionKind.UnitReview, SessionKind.UnitTest -> progress.completeNodes(listOfNotNull(nodeId), session.accuracy)
            SessionKind.SkipTest -> {
                progress.completeNodes(PathPlanner.nodesCompletedBySkip(course, unitId!!), null)
                unitSkipped = true
            }
            SessionKind.Placement -> {
                placementSkip = placementStart(session, placementUnits, course)
                if (placementSkip > 0) {
                    val target = course.sections.flatMap { it.units }.getOrNull(placementSkip - 1)
                    if (target != null) progress.completeNodes(PathPlanner.nodesCompletedBySkip(course, target.id), null)
                }
                settings.update { it.copy(placementDone = true) }
            }
            else -> Unit
        }
        val heartsRefilled = passed && kind == SessionKind.HeartsRefill
        var wallet = game.updateWallet { w ->
            val base = if (heartsRefilled) HeartsRules.refillAll(w) else w
            base.copy(gems = base.gems + reward.gems)
        }

        // Streak: any finished session that earned XP counts as a study day.
        var streakExtended = false
        if (passed && reward.totalXp > 0) {
            val (st, grew) = StreakRules.recordStudy(game.streakNow(), today)
            game.saveStreak(st)
            streakExtended = grew
        }
        val streakNow = game.streakNow().let { StreakRules.current(it.studyDays, it.frozenDays, today) }

        // Quests.
        val quests = game.questsNow()
        val updated = quests.map { if (it.period == QuestPeriod.Daily && it.periodKey == today.toString()) QuestRules.apply(it, result) else it }
        val newlyDone = updated.filter { q -> q.done && quests.first { it.id == q.id }.let { !it.done } }
        var questGems = 0
        var finalQuests = updated.map { if (it in newlyDone) it.copy(claimed = true) else it }
        newlyDone.forEach { questGems += it.reward }
        if (newlyDone.isNotEmpty()) {
            game.increment(Counters.QUESTS_DONE, newlyDone.size)
            val month = YearMonth.from(today).toString()
            finalQuests = finalQuests.map { q ->
                if (q.period == QuestPeriod.Monthly && q.periodKey == month && !q.done) {
                    val p = (q.progress + newlyDone.size).coerceAtMost(q.target)
                    if (p >= q.target) { questGems += q.reward; game.increment(Counters.MONTHLY_BADGES) }
                    q.copy(progress = p, claimed = p >= q.target)
                } else q
            }
        }
        game.saveQuests(finalQuests)
        if (kind == SessionKind.Story && passed) game.increment(Counters.STORIES)

        // Achievements (gems are credited automatically for every reached tier).
        val unlocked = claimAchievements(today)
        val achGems = unlocked.sumOf { it.second }
        if (questGems + achGems > 0) wallet = game.updateWallet { it.copy(gems = it.gems + questGems + achGems) }

        val todayXp = progress.resultsBetween(time.dayStart(today), time.dayStart(today.plusDays(1))).sumOf { it.xp }
        return SessionSummary(
            passed = passed, xp = reward.totalXp, bonusXp = reward.bonusXp,
            gems = reward.gems + questGems + achGems, accuracy = session.accuracy, durationMs = session.durationMs(now),
            mistakes = session.mistakes, streak = streakNow, streakExtended = streakExtended,
            dailyGoalXp = s.dailyGoalXp, todayXp = todayXp,
            questsCompleted = newlyDone.map { it.title }, achievementsUnlocked = unlocked.map { it.first },
            heartsRefilled = heartsRefilled, legendary = kind == SessionKind.Legendary && passed,
            unitSkipped = unitSkipped, placementUnits = placementSkip,
        )
    }

    /**
     * Number of course units to skip after placement: the longest prefix of answers that is
     * ≥ 75% correct and ends with a correct answer marks the last mastered unit.
     */
    private fun placementStart(session: LessonSession, units: List<String?>, course: org.linguafree.core.domain.model.Course): Int {
        val answers = session.firstTryResultsInOrder
        if (answers.size < 2 || units.isEmpty()) return 0
        var right = 0
        var best = 0
        answers.forEachIndexed { i, ok ->
            if (ok) right++
            if (ok && i >= 1 && right * 100 >= 75 * (i + 1)) best = i + 1
        }
        if (best == 0) return 0
        val lastMastered = units.getOrNull(best - 1) ?: return 0
        val index = course.sections.flatMap { it.units }.indexOfFirst { it.id == lastMastered }
        return if (index < 0) 0 else index + 1
    }

    private suspend fun claimAchievements(today: LocalDate): List<Pair<String, Int>> {
        val stats = AchievementStats.collect(progress, game, time, today)
        val claimed = game.claimedAchievements().first()
        val out = mutableListOf<Pair<String, Int>>()
        for (def in Achievements.all) {
            val level = Achievements.levelFor(def, stats[def.id] ?: 0)
            val had = claimed[def.id] ?: 0
            if (level > had) {
                game.setClaimedAchievement(def.id, level)
                out += "${def.title} · уровень $level" to def.gemsPerTier * (level - had)
            }
        }
        return out
    }
}

/** Values that drive the achievements. */
object AchievementStats {
    suspend fun collect(progress: ProgressRepository, game: GameRepository, time: TimeProvider, today: LocalDate): Map<String, Int> {
        val results = progress.results().first()
        val streak = game.streakNow()
        val nodes = progress.nodeProgressNow()
        val memory = progress.memory()
        val now = time.now()
        return mapOf(
            "fire" to maxOf(streak.longest, StreakRules.current(streak.studyDays, streak.frozenDays, today)),
            "sage" to results.sumOf { it.xp },
            "perfect" to results.count { it.passed && it.perfect && it.kind.spendsHearts },
            "words" to memory.values.count { Srs.isLearned(it, now) },
            "scholar" to nodes.values.count { it.completed },
            "champion" to game.leagueState().top3Count,
            "storyteller" to game.counter(Counters.STORIES),
            "speaker" to results.sumOf { it.speakingDone },
            "quests" to game.counter(Counters.QUESTS_DONE),
        )
    }
}

/** Runs on app start and resume: hearts regenerate, missed days spend freezes, quests roll, leagues close. */
class DailyMaintenanceUseCase(
    private val progress: ProgressRepository,
    private val game: GameRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    data class Report(val freezesUsed: Int, val streakLost: Boolean, val league: LeagueRules.WeekResult?)

    suspend operator fun invoke(): Report {
        val now = time.now()
        val today = time.today()
        val s = settings.current()
        var wallet = game.updateWallet { HeartsRules.regenerate(it, now) }

        // Streak freezes.
        val fr = StreakRules.applyMissedDays(game.streakNow(), wallet.freezes, today)
        game.saveStreak(fr.state)
        if (fr.freezesUsed.isNotEmpty()) wallet = game.updateWallet { it.copy(freezes = fr.freezesLeft) }

        // Quests: today's three + this month's badge quest.
        val quests = game.questsNow()
        val todayKey = today.toString()
        val month = YearMonth.from(today)
        val keep = quests.filter { (it.period == QuestPeriod.Daily && it.periodKey == todayKey) || (it.period == QuestPeriod.Monthly && it.periodKey == month.toString()) }
        var fresh = keep
        if (keep.none { it.period == QuestPeriod.Daily }) fresh = fresh + QuestRules.dailyFor(today, s.dailyGoalXp, s.speakingEnabled(now))
        if (keep.none { it.period == QuestPeriod.Monthly }) fresh = fresh + QuestRules.monthlyFor(month)
        if (fresh != quests) game.saveQuests(fresh)

        // League: apply the result of the previous week once.
        val league = game.leagueState()
        val week = LeagueRules.weekKey(today)
        var weekResult: LeagueRules.WeekResult? = null
        if (league.processedWeek.isEmpty()) {
            game.saveLeagueState(league.copy(processedWeek = week))
        } else if (league.processedWeek != week) {
            val prevDay = today.minusDays(7)
            val prevKey = LeagueRules.weekKey(prevDay)
            if (league.processedWeek == prevKey) {
                val from = LeagueRules.weekStartMillis(prevDay, time.zone())
                val to = LeagueRules.weekEndMillis(prevDay, time.zone())
                val xp = progress.resultsBetween(from, to + 1).sumOf { it.xp }
                weekResult = LeagueRules.finish(prevDay, league.tier, s.name, xp, time.zone())
            }
            val r = weekResult
            game.saveLeagueState(
                league.copy(
                    tier = r?.newTier ?: league.tier,
                    processedWeek = week,
                    lastResultRank = r?.rank ?: 0,
                    lastResultTierChange = r?.let { it.newTier - it.oldTier } ?: 0,
                    lastResultWeek = r?.weekKey ?: "",
                    top3Count = league.top3Count + if (r != null && r.rank <= 3) 1 else 0,
                )
            )
            if (r != null && r.gems > 0) game.updateWallet { it.copy(gems = it.gems + r.gems) }
        }
        return Report(fr.freezesUsed.size, fr.broken, weekResult)
    }
}

class ShopUseCase(private val game: GameRepository, private val settings: SettingsRepository, private val time: TimeProvider) {
    sealed interface Result {
        data object Ok : Result
        data class NotEnoughGems(val missing: Int) : Result
        data class NotAvailable(val reason: String) : Result
    }

    suspend fun buy(item: ShopItem): Result {
        val now = time.now()
        val w = HeartsRules.regenerate(game.walletNow(), now)
        val owned = game.inventory().first()
        when {
            item == ShopItem.StreakFreeze && w.freezes >= StreakRules.MAX_FREEZES ->
                return Result.NotAvailable("У тебя уже ${StreakRules.MAX_FREEZES} заморозки — это максимум.")
            item.isCosmetic && item.name in owned -> return Result.NotAvailable("Уже куплено.")
        }
        if (w.gems < item.price) return Result.NotEnoughGems(item.price - w.gems)
        game.updateWallet { cur ->
            val base = HeartsRules.regenerate(cur, now).copy(gems = cur.gems - item.price)
            when (item) {
                ShopItem.StreakFreeze -> base.copy(freezes = base.freezes + 1)
                else -> base
            }
        }
        if (item.isCosmetic) {
            game.addToInventory(item.name)
            when (item) {
                ShopItem.ThemeOcean -> settings.update { it.copy(accent = "Ocean") }
                ShopItem.ThemeSunset -> settings.update { it.copy(accent = "Sunset") }
                ShopItem.ThemeLilac -> settings.update { it.copy(accent = "Lilac") }
                else -> settings.update { it.copy(outfit = item.name) }
            }
        }
        return Result.Ok
    }
}

class OpenChestUseCase(private val progress: ProgressRepository, private val game: GameRepository) {
    suspend operator fun invoke(nodeId: String): Int {
        val gems = Rewards.chestGems(nodeId)
        progress.completeNodes(listOf(nodeId), null)
        game.updateWallet { it.copy(gems = it.gems + gems) }
        return gems
    }
}

/** Default wallet for a new learner. */
val NewWallet = Wallet(gems = 50, hearts = HeartsRules.MAX, heartsRefillAt = null, freezes = 0, doubleXpUntil = 0)
