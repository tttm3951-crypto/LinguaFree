package org.linguafree.core.domain.repository

import kotlinx.coroutines.flow.Flow
import org.linguafree.core.domain.learning.Srs
import org.linguafree.core.domain.model.Course
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.LessonResult
import org.linguafree.core.domain.model.Lexeme
import org.linguafree.core.domain.model.LexemeState
import org.linguafree.core.domain.model.NodeProgress
import org.linguafree.core.domain.model.Quest
import org.linguafree.core.domain.model.StreakState
import org.linguafree.core.domain.model.UserSettings
import org.linguafree.core.domain.model.Wallet

/** Course content loaded from the bundled JSON files. */
interface ContentRepository {
    /** Loads, validates and imports content when its version changed. */
    suspend fun ensureContent(): ContentStatus
    suspend fun course(): Course
    suspend fun exercises(nodeId: String): List<Exercise>
    suspend fun exercisesByNode(): Map<String, List<Exercise>>
    suspend fun lexemes(): List<Lexeme>
    /** Tap-to-translate hints, lowercase word → translation. */
    suspend fun hints(): Map<String, String>
}

sealed interface ContentStatus {
    data class Ready(val contentVersion: Int, val imported: Boolean) : ContentStatus
    data class Invalid(val errors: List<String>) : ContentStatus
}

interface ProgressRepository {
    fun nodeProgress(): Flow<Map<String, NodeProgress>>
    suspend fun nodeProgressNow(): Map<String, NodeProgress>
    suspend fun completeNodes(nodeIds: List<String>, accuracy: Int?, legendary: Boolean = false)
    suspend fun addResult(result: LessonResult)
    fun results(): Flow<List<LessonResult>>
    suspend fun resultsBetween(fromMs: Long, toMs: Long): List<LessonResult>
    suspend fun memory(): Map<String, Srs.Memory>
    suspend fun updateMemory(results: List<Pair<String, Boolean>>, now: Long)
    fun lexemeStates(now: Long): Flow<List<LexemeState>>
    suspend fun addMistakes(exerciseIds: Collection<String>, now: Long)
    suspend fun removeMistakes(exerciseIds: Collection<String>)
    suspend fun mistakeIds(limit: Int): List<String>
    fun mistakesCount(): Flow<Int>
}

interface GameRepository {
    fun wallet(): Flow<Wallet>
    suspend fun walletNow(): Wallet
    suspend fun updateWallet(transform: (Wallet) -> Wallet): Wallet

    fun streak(): Flow<StreakState>
    suspend fun streakNow(): StreakState
    suspend fun saveStreak(state: StreakState)

    fun quests(): Flow<List<Quest>>
    suspend fun questsNow(): List<Quest>
    suspend fun saveQuests(quests: List<Quest>)

    fun claimedAchievements(): Flow<Map<String, Int>>
    suspend fun setClaimedAchievement(id: String, level: Int)

    /** Current league tier (0..9) and the last processed week key. */
    fun leagueTier(): Flow<Int>
    suspend fun leagueState(): LeagueState
    suspend fun saveLeagueState(state: LeagueState)

    fun inventory(): Flow<Set<String>>
    suspend fun addToInventory(item: String)

    /** Counters for achievements that aren't derivable from results. */
    suspend fun counter(key: String): Int
    suspend fun increment(key: String, by: Int = 1)
    fun counters(): Flow<Map<String, Int>>
}

data class LeagueState(
    val tier: Int,
    /** Last week whose result was applied (yyyy-Www) or empty. */
    val processedWeek: String,
    val lastResultRank: Int = 0,
    val lastResultTierChange: Int = 0,
    val lastResultWeek: String = "",
    val top3Count: Int = 0,
)

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun current(): UserSettings
    suspend fun update(transform: (UserSettings) -> UserSettings)
}
