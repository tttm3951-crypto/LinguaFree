package org.linguafree.feature.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.linguafree.core.domain.game.HeartsRules
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.model.Course
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.ProgressRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.TimeProvider
import javax.inject.Inject

data class TopStats(
    val streak: Int = 0,
    val studiedToday: Boolean = false,
    val gems: Int = 0,
    val hearts: Int = HeartsRules.MAX,
    val unlimitedHearts: Boolean = false,
)

data class LearnUiState(
    val course: Course? = null,
    val completed: Set<String> = emptySet(),
    val stats: TopStats = TopStats(),
)

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val content: ContentRepository,
    progress: ProgressRepository,
    game: GameRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val course = MutableStateFlow<Course?>(null)

    val state: StateFlow<LearnUiState> = combine(
        course, progress.nodeProgress(), game.wallet(), game.streak(), settings.settings,
    ) { c, p, w, st, s ->
        val today = time.today()
        LearnUiState(
            course = c,
            completed = p.filterValues { it.completed }.keys,
            stats = TopStats(
                streak = StreakRules.current(st.studyDays, st.frozenDays, today),
                studiedToday = today in st.studyDays,
                gems = w.gems,
                hearts = HeartsRules.regenerate(w, time.now()).hearts,
                unlimitedHearts = s.heartsDisabled,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnUiState())

    init {
        viewModelScope.launch { course.value = content.course() }
    }
}
