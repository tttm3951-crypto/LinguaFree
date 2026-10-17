package org.linguafree.core.domain.game

import org.linguafree.core.domain.model.LeagueEntry
import org.linguafree.core.domain.model.LeagueStanding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import kotlin.math.exp
import kotlin.random.Random

/**
 * Weekly leagues: 30 participants (the learner + 29 local bots), 10 tiers.
 * The week runs Monday 00:00 → Sunday 23:59 local time; results are fixed on Sunday night.
 * Top 7 are promoted, the bottom 5 are relegated. Everything is computed on-device
 * and deterministically from the week key, so bots behave the same on every app start.
 */
object LeagueRules {
    const val SIZE = 30
    const val PROMOTE = 7
    const val DEMOTE = 5
    const val TIERS = 10

    data class Tier(val title: String, val emoji: String, val color: Long)

    val tiers = listOf(
        Tier("Моховая", "🌿", 0xFF7FB069),
        Tier("Папоротник", "🍃", 0xFF4CC417),
        Tier("Шишка", "🌰", 0xFFB8743A),
        Tier("Жёлудь", "🥜", 0xFFD99A00),
        Tier("Клён", "🍁", 0xFFFF7A45),
        Tier("Берёза", "🌳", 0xFF34D1A2),
        Tier("Сосна", "🌲", 0xFF2E8B57),
        Tier("Кедр", "🏔️", 0xFF2EB8F2),
        Tier("Секвойя", "🌟", 0xFFA56BF7),
        Tier("Северное сияние", "🌌", 0xFFFF4B6E),
    )

    /** Gems for finishing places 1–3; promotion adds a bonus. */
    fun placeReward(rank: Int): Int = when (rank) { 1 -> 50; 2 -> 30; 3 -> 20; else -> 0 }
    const val PROMOTION_BONUS = 15

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekKey(date: LocalDate): String {
        val start = weekStart(date)
        return "%d-W%02d".format(start.get(IsoFields.WEEK_BASED_YEAR), start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
    }

    fun weekEndMillis(date: LocalDate, zone: ZoneId): Long =
        weekStart(date).plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun weekStartMillis(date: LocalDate, zone: ZoneId): Long =
        weekStart(date).atStartOfDay(zone).toInstant().toEpochMilli()

    private val names = listOf(
        "Алина", "Тимур", "Даша", "Арсений", "Мадина", "Егор", "Соня", "Айдар", "Ксюша", "Максим",
        "Полина", "Руслан", "Вика", "Илья", "Лейла", "Никита", "Аня", "Ержан", "Варя", "Глеб",
        "Камила", "Денис", "Ева", "Артём", "Диана", "Олжас", "Маша", "Кирилл", "Аружан", "Лёва",
        "Настя", "Санжар", "Юля", "Миша", "Амина", "Петя", "Зарина", "Ваня", "Лиза", "Данияр",
        "Катя", "Рома", "Алия", "Федя", "Милана", "Саша", "Томирис", "Гоша", "Злата", "Нурлан",
        "Женя", "Ася", "Марат", "Оля", "Ян", "Ника", "Давид", "Света", "Богдан", "Асель",
        "kotik_88", "englishfan", "Звёздочка", "Lucky", "Совушка_нет", "ПоливалКактус", "Ёжик", "Moon",
        "Пельмешек", "Бублик", "Енот", "Капибара", "skywalker", "Тучка", "Lingvo", "Кексик",
    )

    /** Simulated learner. Weekly volume and daily habits come from a deterministic seed. */
    private data class Bot(
        val id: String, val name: String, val avatar: Int,
        val dailyXp: IntArray, val activeHours: IntRange,
    )

    private fun bots(weekKey: String, tier: Int): List<Bot> {
        val rnd = Random((weekKey.hashCode().toLong() shl 8) xor tier.toLong() * 7919L)
        val shuffled = names.shuffled(rnd)
        return (0 until SIZE - 1).map { i ->
            // Log-normal-ish activity: many casual learners, a few very active ones; higher tiers are busier.
            val scale = 60 + tier * 35
            val weekly = (scale * exp(rnd.nextDouble(-1.6, 1.4))).toInt().coerceAtLeast(0)
            val activeDays = rnd.nextInt(1, 8)
            val days = (0 until 7).shuffled(rnd).take(activeDays).toSet()
            val per = IntArray(7) { d -> if (d in days) (weekly / activeDays) + rnd.nextInt(-5, 10) else 0 }.map { it.coerceAtLeast(0) - it.coerceAtLeast(0) % 5 }.toIntArray()
            val startHour = rnd.nextInt(6, 18)
            Bot("bot-$weekKey-$tier-$i", shuffled[i % shuffled.size], rnd.nextInt(0, 6), per, startHour..(startHour + rnd.nextInt(2, 6)).coerceAtMost(23))
        }
    }

    /** XP a bot has earned by [now] (counting finished days fully and today by the hour). */
    private fun botXpAt(bot: Bot, weekStart: LocalDate, now: ZonedDateTime): Int {
        var xp = 0
        for (d in 0 until 7) {
            val day = weekStart.plusDays(d.toLong())
            val today = now.toLocalDate()
            xp += when {
                day.isBefore(today) -> bot.dailyXp[d]
                day == today -> {
                    val h = now.hour
                    when {
                        h > bot.activeHours.last -> bot.dailyXp[d]
                        h < bot.activeHours.first -> 0
                        else -> {
                            val span = (bot.activeHours.last - bot.activeHours.first + 1).coerceAtLeast(1)
                            bot.dailyXp[d] * (h - bot.activeHours.first + 1) / span / 5 * 5
                        }
                    }
                }
                else -> 0
            }
        }
        return xp
    }

    fun standing(
        tier: Int, userName: String, userXp: Int, now: ZonedDateTime,
    ): LeagueStanding {
        val date = now.toLocalDate()
        val key = weekKey(date)
        val start = weekStart(date)
        val entries = bots(key, tier).map { b -> LeagueEntry(b.id, b.name, b.avatar, botXpAt(b, start, now), false) } +
            LeagueEntry("user", userName.ifBlank { "Ты" }, -1, userXp, true)
        val sorted = entries.sortedWith(compareByDescending<LeagueEntry> { it.xp }.thenBy { if (it.isUser) 0 else 1 }.thenBy { it.name })
        return LeagueStanding(tier, key, sorted, weekEndMillis(date, now.zone), joined = userXp > 0)
    }

    enum class Zone { Promotion, Safe, Demotion }

    fun zone(rank: Int, tier: Int): Zone = when {
        rank <= PROMOTE && tier < TIERS - 1 -> Zone.Promotion
        rank > SIZE - DEMOTE && tier > 0 -> Zone.Demotion
        else -> Zone.Safe
    }

    data class WeekResult(val weekKey: String, val rank: Int, val oldTier: Int, val newTier: Int, val gems: Int)

    /** Final result of a finished week. Learners who didn't join (0 XP) keep their tier. */
    fun finish(weekDay: LocalDate, tier: Int, userName: String, userXp: Int, zone: ZoneId): WeekResult? {
        if (userXp <= 0) return null
        val end = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(weekEndMillis(weekDay, zone)), zone)
        val st = standing(tier, userName, userXp, end)
        val rank = st.entries.indexOfFirst { it.isUser } + 1
        val newTier = when (zone(rank, tier)) {
            Zone.Promotion -> tier + 1
            Zone.Demotion -> tier - 1
            Zone.Safe -> tier
        }
        val gems = placeReward(rank) + if (newTier > tier) PROMOTION_BONUS else 0
        return WeekResult(st.weekKey, rank, tier, newTier, gems)
    }
}
