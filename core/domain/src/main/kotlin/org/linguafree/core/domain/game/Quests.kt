package org.linguafree.core.domain.game

import org.linguafree.core.domain.model.AchievementState
import org.linguafree.core.domain.model.LessonResult
import org.linguafree.core.domain.model.Quest
import org.linguafree.core.domain.model.QuestPeriod
import org.linguafree.core.domain.model.QuestType
import org.linguafree.core.domain.model.SessionKind
import java.time.LocalDate
import java.time.YearMonth

/** Three daily quests (deterministic per day) and one monthly badge quest. */
object QuestRules {
    const val DAILY_COUNT = 3
    const val MONTHLY_TARGET = 20
    const val MONTHLY_REWARD = 100

    private data class Template(val type: QuestType, val targets: List<Int>, val reward: Int)

    private val pool = listOf(
        Template(QuestType.EarnXp, listOf(20, 30, 40, 50), 10),
        Template(QuestType.CompleteLessons, listOf(1, 2, 3), 10),
        Template(QuestType.PerfectLessons, listOf(1, 2), 15),
        Template(QuestType.Combo, listOf(5, 10, 15), 10),
        Template(QuestType.Listening, listOf(3, 5, 8), 10),
        Template(QuestType.Speaking, listOf(2, 3, 5), 10),
        Template(QuestType.Practice, listOf(1, 2), 10),
    )

    /** The quests for [day]. Earn-XP is always first (it matches the daily goal), two others rotate. */
    fun dailyFor(day: LocalDate, dailyGoalXp: Int, speakingEnabled: Boolean): List<Quest> {
        val seed = day.toEpochDay()
        val rnd = java.util.Random(seed * 31 + 7)
        val xpTarget = (dailyGoalXp + 10).coerceIn(20, 60)
        val others = pool.drop(1).filter { speakingEnabled || it.type != QuestType.Speaking }.shuffled(rnd).take(DAILY_COUNT - 1)
        val key = day.toString()
        return listOf(Quest("d-$key-xp", QuestPeriod.Daily, key, QuestType.EarnXp, xpTarget, 0, 10, false)) +
            others.mapIndexed { i, t ->
                Quest("d-$key-$i-${t.type.name}", QuestPeriod.Daily, key, t.type, t.targets[rnd.nextInt(t.targets.size)], 0, t.reward, false)
            }
    }

    fun monthlyFor(month: YearMonth): Quest =
        Quest("m-$month", QuestPeriod.Monthly, month.toString(), QuestType.DailyQuests, MONTHLY_TARGET, 0, MONTHLY_REWARD, false)

    /** Progress gained from one finished session. */
    fun progressFrom(type: QuestType, r: LessonResult): Int = when (type) {
        QuestType.EarnXp -> r.xp
        QuestType.CompleteLessons -> if (r.passed && (r.kind == SessionKind.Lesson || r.kind == SessionKind.Story)) 1 else 0
        QuestType.PerfectLessons -> if (r.passed && r.perfect && r.kind.spendsHearts) 1 else 0
        QuestType.Combo -> r.bestCombo
        QuestType.Listening -> r.listeningDone
        QuestType.Speaking -> r.speakingDone
        QuestType.Practice -> if (r.kind.isPractice) 1 else 0
        QuestType.DailyQuests -> 0
    }

    /** Combo is a max, everything else accumulates. */
    fun apply(quest: Quest, r: LessonResult): Quest {
        if (quest.done) return quest
        val gained = progressFrom(quest.type, r)
        val p = if (quest.type == QuestType.Combo) maxOf(quest.progress, gained) else quest.progress + gained
        return quest.copy(progress = p.coerceAtMost(quest.target))
    }
}

/** Tiered achievements, e.g. «Огонь»: streak of 3/7/14/30… days. */
object Achievements {
    data class Def(val id: String, val title: String, val description: (Int) -> String, val tiers: List<Int>, val gemsPerTier: Int)

    val all = listOf(
        Def("fire", "Огонь", { "Серия $it дн." }, listOf(3, 7, 14, 30, 60, 100, 200, 365), 10),
        Def("sage", "Мудрец", { "Заработать $it XP" }, listOf(100, 500, 1000, 2500, 5000, 10000, 25000), 10),
        Def("perfect", "Отличник", { "Уроков без ошибок: $it" }, listOf(1, 5, 15, 40, 100, 250), 10),
        Def("words", "Знаток слов", { "Выучено слов: $it" }, listOf(10, 50, 100, 250, 500, 1000), 15),
        Def("scholar", "Учёный", { "Пройдено уроков: $it" }, listOf(5, 25, 50, 100, 200, 315), 10),
        Def("champion", "Чемпион", { "Финиш в тройке лиги: $it" }, listOf(1, 3, 10, 25), 20),
        Def("storyteller", "Рассказчик", { "Историй прочитано: $it" }, listOf(1, 5, 15, 40, 70), 10),
        Def("speaker", "Голос", { "Фраз сказано вслух: $it" }, listOf(5, 25, 100, 300), 10),
        Def("quests", "Искатель", { "Выполнено заданий: $it" }, listOf(5, 30, 100, 300), 15),
    )

    fun levelFor(def: Def, value: Int) = def.tiers.count { value >= it }

    fun state(def: Def, value: Int, claimedLevel: Int): AchievementState {
        val level = levelFor(def, value)
        val target = def.tiers.getOrNull(level) ?: def.tiers.last()
        return AchievementState(def.id, def.title, def.description(target), def.tiers, value, level, claimedLevel)
    }
}
