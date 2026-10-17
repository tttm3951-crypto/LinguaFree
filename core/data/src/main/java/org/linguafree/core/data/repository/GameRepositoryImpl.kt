package org.linguafree.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.linguafree.core.data.db.AchievementEntity
import org.linguafree.core.data.db.CounterEntity
import org.linguafree.core.data.db.GameDao
import org.linguafree.core.data.db.InventoryEntity
import org.linguafree.core.data.db.LeagueEntryEntity
import org.linguafree.core.data.db.LeagueStateEntity
import org.linguafree.core.data.db.QuestEntity
import org.linguafree.core.data.db.StreakDayEntity
import org.linguafree.core.data.db.StreakEntity
import org.linguafree.core.data.db.WalletEntity
import org.linguafree.core.domain.model.Quest
import org.linguafree.core.domain.model.QuestPeriod
import org.linguafree.core.domain.model.QuestType
import org.linguafree.core.domain.model.StreakState
import org.linguafree.core.domain.model.Wallet
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.LeagueState
import org.linguafree.core.domain.usecase.NewWallet
import java.time.LocalDate

class GameRepositoryImpl(private val dao: GameDao) : GameRepository {

    private val walletLock = Mutex()

    override fun wallet(): Flow<Wallet> = dao.walletFlow().map { it?.toDomain() ?: NewWallet }

    override suspend fun walletNow(): Wallet = dao.wallet()?.toDomain() ?: NewWallet

    override suspend fun updateWallet(transform: (Wallet) -> Wallet): Wallet = walletLock.withLock {
        val next = transform(walletNow())
        dao.upsertWallet(next.toEntity())
        next
    }

    override fun streak(): Flow<StreakState> =
        combine(dao.streakDaysFlow(), dao.streakFlow()) { days, meta -> toStreak(days, meta) }

    override suspend fun streakNow(): StreakState = toStreak(dao.streakDays(), dao.streak())

    override suspend fun saveStreak(state: StreakState) {
        dao.upsertStreakDays(
            state.studyDays.map { StreakDayEntity(it.toString(), false) } +
                (state.frozenDays - state.studyDays).map { StreakDayEntity(it.toString(), true) }
        )
        dao.upsertStreak(StreakEntity(0, state.longest, state.checkedThrough?.toString()))
    }

    private fun toStreak(days: List<StreakDayEntity>, meta: StreakEntity?): StreakState {
        val study = days.filter { !it.frozen }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
        val frozen = days.filter { it.frozen }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
        return StreakState(study, frozen, meta?.longest ?: 0, meta?.checkedThrough?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
    }

    override fun quests(): Flow<List<Quest>> = dao.questsFlow().map { l -> l.mapNotNull { it.toDomain() } }
    override suspend fun questsNow(): List<Quest> = dao.quests().mapNotNull { it.toDomain() }
    override suspend fun saveQuests(quests: List<Quest>) = dao.replaceQuests(quests.map { it.toEntity() })

    override fun claimedAchievements(): Flow<Map<String, Int>> =
        dao.achievementsFlow().map { l -> l.associate { it.id to it.claimedLevel } }

    override suspend fun setClaimedAchievement(id: String, level: Int) = dao.upsertAchievement(AchievementEntity(id, level))

    override fun leagueTier(): Flow<Int> = dao.leagueStateFlow().map { it?.tier ?: 0 }

    override suspend fun leagueState(): LeagueState = dao.leagueState()?.let {
        LeagueState(it.tier, it.processedWeek, it.lastRank, it.lastTierChange, it.lastWeek, it.top3Count)
    } ?: LeagueState(0, "")

    override suspend fun saveLeagueState(state: LeagueState) {
        dao.upsertLeagueState(
            LeagueStateEntity(0, state.tier, state.processedWeek, state.lastResultRank, state.lastResultTierChange, state.lastResultWeek, state.top3Count)
        )
        if (state.lastResultWeek.isNotEmpty()) {
            dao.upsertLeagueEntry(LeagueEntryEntity(state.lastResultWeek, state.tier, state.lastResultRank, state.lastResultTierChange))
        }
    }

    fun leagueHistory() = dao.leagueHistory()

    override fun inventory(): Flow<Set<String>> = dao.inventoryFlow().map { it.toSet() }
    override suspend fun addToInventory(item: String) = dao.addInventory(InventoryEntity(item, System.currentTimeMillis()))

    override suspend fun counter(key: String): Int = dao.counter(key) ?: 0
    override suspend fun increment(key: String, by: Int) = dao.upsertCounter(CounterEntity(key, counter(key) + by))
    override fun counters(): Flow<Map<String, Int>> = dao.countersFlow().map { l -> l.associate { it.key to it.value } }
}

internal fun WalletEntity.toDomain() = Wallet(gems, hearts, heartsRefillAt, freezes, doubleXpUntil)
internal fun Wallet.toEntity() = WalletEntity(0, gems, hearts, heartsRefillAt, freezes, doubleXpUntil)

internal fun QuestEntity.toDomain(): Quest? = runCatching {
    Quest(id, QuestPeriod.valueOf(period), periodKey, QuestType.valueOf(type), target, progress, reward, claimed)
}.getOrNull()

internal fun Quest.toEntity() = QuestEntity(id, period.name, periodKey, type.name, target, progress, reward, claimed)
