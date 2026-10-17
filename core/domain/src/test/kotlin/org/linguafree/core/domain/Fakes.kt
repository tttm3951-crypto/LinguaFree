package org.linguafree.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.linguafree.core.domain.learning.Srs
import org.linguafree.core.domain.model.Course
import org.linguafree.core.domain.model.CourseUnit
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.domain.model.Guidebook
import org.linguafree.core.domain.model.LessonResult
import org.linguafree.core.domain.model.Lexeme
import org.linguafree.core.domain.model.LexemeState
import org.linguafree.core.domain.model.NodeProgress
import org.linguafree.core.domain.model.NodeType
import org.linguafree.core.domain.model.PathNode
import org.linguafree.core.domain.model.Quest
import org.linguafree.core.domain.model.Section
import org.linguafree.core.domain.model.StreakState
import org.linguafree.core.domain.model.UserSettings
import org.linguafree.core.domain.model.Wallet
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.ContentStatus
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.LeagueState
import org.linguafree.core.domain.repository.ProgressRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.NewWallet
import org.linguafree.core.domain.usecase.TimeProvider
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** In-memory test doubles for the repositories: one unit, two lessons, a chest and a test. */
class FakeTime(var dateTime: LocalDateTime = LocalDateTime.of(2026, 10, 14, 12, 0)) : TimeProvider {
    override fun now(): Long = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
    override fun zone(): ZoneId = ZoneOffset.UTC
}

fun sampleExercises(nodeId: String, n: Int = 12) = (1..n).map {
    Exercise("$nodeId-e$it", nodeId, it, ExerciseType.SelectTranslation, "привет $it", answers = listOf("hello"), lexemes = listOf("hello"))
}

class FakeContent : ContentRepository {
    val nodes = listOf(
        PathNode("l1", "u1", 1, NodeType.Lesson, "Урок 1", listOf("hello")),
        PathNode("l2", "u1", 2, NodeType.Lesson, "Урок 2"),
        PathNode("c1", "u1", 3, NodeType.Chest, "Сундук"),
        PathNode("t1", "u1", 4, NodeType.Test, "Проверка"),
    )
    val course = Course(
        "en-ru", "Английский", 1,
        listOf(Section("a1", 1, "A1", "A1", "", listOf(CourseUnit("u1", "a1", 1, "Приветствия", "", 0xFF4CC417, Guidebook(emptyList()), nodes)))),
    )
    private val byNode = nodes.filter { it.type != NodeType.Chest }.associate { it.id to sampleExercises(it.id) }
    override suspend fun ensureContent() = ContentStatus.Ready(1, false)
    override suspend fun course() = course
    override suspend fun exercises(nodeId: String) = byNode[nodeId].orEmpty()
    override suspend fun exercisesByNode() = byNode
    override suspend fun lexemes() = listOf(Lexeme("hello", "hello", "привет"))
    override suspend fun hints() = mapOf("привет" to "hello")
}

class FakeProgress : ProgressRepository {
    val progress = MutableStateFlow<Map<String, NodeProgress>>(emptyMap())
    val resultsFlow = MutableStateFlow<List<LessonResult>>(emptyList())
    val memoryMap = mutableMapOf<String, Srs.Memory>()
    val mistakes = linkedMapOf<String, Long>()

    override fun nodeProgress(): Flow<Map<String, NodeProgress>> = progress
    override suspend fun nodeProgressNow() = progress.value
    override suspend fun completeNodes(nodeIds: List<String>, accuracy: Int?, legendary: Boolean) {
        progress.value = progress.value + nodeIds.associateWith { id ->
            val old = progress.value[id]
            NodeProgress(id, true, legendary || old?.legendary == true, (old?.timesCompleted ?: 0) + 1, maxOf(old?.bestAccuracy ?: 0, accuracy ?: 0), accuracy ?: 100)
        }
    }
    override suspend fun addResult(result: LessonResult) { resultsFlow.value = resultsFlow.value + result }
    override fun results(): Flow<List<LessonResult>> = resultsFlow
    override suspend fun resultsBetween(fromMs: Long, toMs: Long) = resultsFlow.value.filter { it.finishedAt in fromMs until toMs }
    override suspend fun memory() = memoryMap.toMap()
    override suspend fun updateMemory(results: List<Pair<String, Boolean>>, now: Long) {
        results.forEach { (id, ok) -> memoryMap[id] = Srs.update(memoryMap[id], ok, now) }
    }
    override fun lexemeStates(now: Long): Flow<List<LexemeState>> = MutableStateFlow(memoryMap.toMap()).map { m ->
        m.map { (id, mem) -> LexemeState(id, id, id, mem.right, mem.wrong, mem.halfLifeDays, mem.lastSeen, Srs.strength(mem, now)) }
    }
    override suspend fun addMistakes(exerciseIds: Collection<String>, now: Long) { exerciseIds.forEach { mistakes[it] = now } }
    override suspend fun removeMistakes(exerciseIds: Collection<String>) { exerciseIds.forEach { mistakes.remove(it) } }
    override suspend fun mistakeIds(limit: Int) = mistakes.keys.toList().takeLast(limit)
    override fun mistakesCount(): Flow<Int> = MutableStateFlow(mistakes.size)
}

class FakeGame(wallet: Wallet = NewWallet) : GameRepository {
    val walletFlow = MutableStateFlow(wallet)
    val streakFlow = MutableStateFlow(StreakState(emptySet(), emptySet(), 0, null))
    val questsFlow = MutableStateFlow<List<Quest>>(emptyList())
    val achievements = MutableStateFlow<Map<String, Int>>(emptyMap())
    var league = LeagueState(0, "")
    val inventoryFlow = MutableStateFlow<Set<String>>(emptySet())
    val countersFlow = MutableStateFlow<Map<String, Int>>(emptyMap())

    override fun wallet(): Flow<Wallet> = walletFlow
    override suspend fun walletNow() = walletFlow.value
    override suspend fun updateWallet(transform: (Wallet) -> Wallet) = transform(walletFlow.value).also { walletFlow.value = it }
    override fun streak(): Flow<StreakState> = streakFlow
    override suspend fun streakNow() = streakFlow.value
    override suspend fun saveStreak(state: StreakState) { streakFlow.value = state }
    override fun quests(): Flow<List<Quest>> = questsFlow
    override suspend fun questsNow() = questsFlow.value
    override suspend fun saveQuests(quests: List<Quest>) { questsFlow.value = quests }
    override fun claimedAchievements(): Flow<Map<String, Int>> = achievements
    override suspend fun setClaimedAchievement(id: String, level: Int) { achievements.value = achievements.value + (id to level) }
    override fun leagueTier(): Flow<Int> = MutableStateFlow(league.tier)
    override suspend fun leagueState() = league
    override suspend fun saveLeagueState(state: LeagueState) { league = state }
    override fun inventory(): Flow<Set<String>> = inventoryFlow
    override suspend fun addToInventory(item: String) { inventoryFlow.value = inventoryFlow.value + item }
    override suspend fun counter(key: String) = countersFlow.value[key] ?: 0
    override suspend fun increment(key: String, by: Int) { countersFlow.value = countersFlow.value + (key to counter(key) + by) }
    override fun counters(): Flow<Map<String, Int>> = countersFlow
}

class FakeSettings(initial: UserSettings = UserSettings(onboarded = true)) : SettingsRepository {
    val flow = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = flow
    override suspend fun current() = flow.value
    override suspend fun update(transform: (UserSettings) -> UserSettings) { flow.value = transform(flow.value) }
}
