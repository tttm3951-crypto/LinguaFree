package org.linguafree.feature.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.linguafree.core.data.platform.Feedback
import org.linguafree.core.data.platform.SpeechOutput
import org.linguafree.core.domain.game.HeartsRules
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.model.Course
import org.linguafree.core.domain.model.CourseUnit
import org.linguafree.core.domain.model.NodeState
import org.linguafree.core.domain.model.NodeType
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.path.PathPlanner
import org.linguafree.core.domain.path.PlannedNode
import org.linguafree.core.domain.path.PlannedUnit
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.ProgressRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.OpenChestUseCase
import org.linguafree.core.domain.usecase.ShopUseCase
import org.linguafree.core.domain.game.ShopItem
import org.linguafree.core.domain.model.Quest
import org.linguafree.core.domain.model.QuestPeriod
import org.linguafree.core.domain.model.StreakState
import org.linguafree.core.domain.usecase.TimeProvider
import javax.inject.Inject

data class TopStats(
    val streak: Int = 0,
    val studiedToday: Boolean = false,
    val gems: Int = 0,
    val hearts: Int = HeartsRules.MAX,
    val unlimitedHearts: Boolean = false,
)

data class PathUiState(
    val loading: Boolean = true,
    val quests: List<Quest> = emptyList(),
    val monthly: Quest? = null,
    val streakState: StreakState = StreakState(emptySet(), emptySet(), 0, null),
    val freezes: Int = 0,
    val heartsRefillAt: Long? = null,
    val heartsDisabled: Boolean = false,
    val units: List<PlannedUnit> = emptyList(),
    val currentNodeId: String? = null,
    val stats: TopStats = TopStats(),
    val dailyGoalXp: Int = 20,
    val todayXp: Int = 0,
    val doubleXpUntil: Long = 0,
)

/** What tapping a node leads to. */
sealed interface PathEvent {
    data class Start(val kind: SessionKind, val nodeId: String?, val unitId: String?) : PathEvent
    data class ChestOpened(val gems: Int) : PathEvent
}

@HiltViewModel
class PathViewModel @Inject constructor(
    private val content: ContentRepository,
    progress: ProgressRepository,
    private val game: GameRepository,
    private val settingsRepo: SettingsRepository,
    private val openChestUseCase: OpenChestUseCase,
    private val shop: ShopUseCase,
    private val time: TimeProvider,
    val speech: SpeechOutput,
    private val feedback: Feedback,
) : ViewModel() {

    private val course = MutableStateFlow<Course?>(null)
    val events = MutableStateFlow<PathEvent?>(null)
    val selected = MutableStateFlow<String?>(null)
    val guidebook = MutableStateFlow<CourseUnit?>(null)

    val state: StateFlow<PathUiState> = combine(
        course, progress.nodeProgress(), game.wallet(), game.streak(),
        combine(settingsRepo.settings, progress.results(), game.quests()) { s, r, q -> Triple(s, r, q) },
    ) { c, p, w, st, (s, results, quests) ->
        val today = time.today()
        val units = if (c == null) emptyList() else PathPlanner.plan(c, p)
        val dayStart = time.dayStart(today)
        PathUiState(
            loading = c == null,
            units = units,
            currentNodeId = PathPlanner.currentNode(units)?.node?.id,
            stats = TopStats(
                streak = StreakRules.current(st.studyDays, st.frozenDays, today),
                studiedToday = today in st.studyDays,
                gems = w.gems,
                hearts = HeartsRules.regenerate(w, time.now()).hearts,
                unlimitedHearts = s.heartsDisabled,
            ),
            quests = quests.filter { it.period == QuestPeriod.Daily && it.periodKey == today.toString() },
            monthly = quests.firstOrNull { it.period == QuestPeriod.Monthly },
            streakState = st,
            freezes = w.freezes,
            heartsRefillAt = HeartsRules.regenerate(w, time.now()).heartsRefillAt,
            heartsDisabled = s.heartsDisabled,
            dailyGoalXp = s.dailyGoalXp,
            todayXp = results.filter { it.finishedAt >= dayStart }.sumOf { it.xp },
            doubleXpUntil = w.doubleXpUntil,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PathUiState())

    init {
        viewModelScope.launch {
            course.value = content.course()
            feedback.soundEnabled = settingsRepo.current().soundEnabled
        }
    }

    fun select(nodeId: String?) { selected.update { if (it == nodeId) null else nodeId } }

    /** Main action of a node popover. */
    fun open(node: PlannedNode, unit: PlannedUnit) {
        selected.value = null
        val n = node.node
        when (n.type) {
            NodeType.Chest -> if (node.state == NodeState.Current) viewModelScope.launch {
                val gems = openChestUseCase(n.id)
                feedback.chest()
                events.value = PathEvent.ChestOpened(gems)
            }
            NodeType.Lesson -> events.value = PathEvent.Start(SessionKind.Lesson, n.id, unit.unit.id)
            NodeType.Story -> events.value = PathEvent.Start(SessionKind.Story, n.id, unit.unit.id)
            NodeType.Review -> events.value = PathEvent.Start(SessionKind.UnitReview, n.id, unit.unit.id)
            NodeType.Test -> events.value = PathEvent.Start(SessionKind.UnitTest, n.id, unit.unit.id)
        }
    }

    fun legendary(node: PlannedNode, unit: PlannedUnit) {
        selected.value = null
        events.value = PathEvent.Start(SessionKind.Legendary, node.node.id, unit.unit.id)
    }

    /** «Перейти сюда»: a free test of a locked unit; passing it opens everything up to that unit. */
    fun skipTo(unit: PlannedUnit) {
        events.value = PathEvent.Start(SessionKind.SkipTest, null, unit.unit.id)
    }

    fun consumeEvent() { events.value = null }

    fun setHeartsDisabled(value: Boolean) { viewModelScope.launch { settingsRepo.update { it.copy(heartsDisabled = value) } } }

    /** Buys a full refill; returns a message for the user. */
    fun buyHearts(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(
                when (val r = shop.buy(ShopItem.HeartsRefill)) {
                    ShopUseCase.Result.Ok -> "Все сердца восстановлены!"
                    is ShopUseCase.Result.NotEnoughGems -> "Не хватает ${r.missing} 💎 — пройди бесплатную тренировку."
                    is ShopUseCase.Result.NotAvailable -> r.reason
                }
            )
        }
    }

    fun startHeartsPractice() { events.value = PathEvent.Start(SessionKind.HeartsRefill, null, null) }
    fun showGuidebook(unit: CourseUnit?) { guidebook.value = unit }
    fun speak(text: String) = speech.speak(text)
}
