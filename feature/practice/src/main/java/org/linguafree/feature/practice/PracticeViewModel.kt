package org.linguafree.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.linguafree.core.data.library.DictEntry
import org.linguafree.core.data.library.LibraryRepository
import org.linguafree.core.data.library.ReferenceEntry
import org.linguafree.core.data.library.Topic
import org.linguafree.core.data.platform.SpeechOutput
import org.linguafree.core.domain.model.LexemeState
import org.linguafree.core.domain.model.UserSettings
import org.linguafree.core.domain.repository.ProgressRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.TimeProvider
import javax.inject.Inject

data class PracticeUiState(
    val mistakes: Int = 0,
    /** Words seen at least once, weakest first. */
    val words: List<LexemeState> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val now: Long = 0,
) {
    val weakCount: Int get() = words.count { it.strength < 0.6 }
    val speakingOn: Boolean get() = settings.speakingEnabled(now)
    val listeningOn: Boolean get() = settings.listeningEnabled(now)
}

@HiltViewModel
class PracticeViewModel @Inject constructor(
    progress: ProgressRepository,
    private val settingsRepo: SettingsRepository,
    private val library: LibraryRepository,
    private val speech: SpeechOutput,
    private val time: TimeProvider,
) : ViewModel() {

    val state: StateFlow<PracticeUiState> = combine(
        progress.mistakesCount(), progress.lexemeStates(time.now()), settingsRepo.settings,
    ) { m, lex, s ->
        PracticeUiState(m, lex.filter { it.lastSeen > 0 }.sortedBy { it.strength }, s, time.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PracticeUiState())

    val favorites = library.favorites
    val mastered = library.mastered
    val topics = MutableStateFlow<List<Topic>>(emptyList())
    val dictionary = MutableStateFlow<List<DictEntry>>(emptyList())
    val referenceAvailable get() = library.referenceAvailable

    init {
        library.reload()
        viewModelScope.launch {
            topics.value = runCatching { library.topics() }.getOrDefault(emptyList())
            dictionary.value = runCatching { library.dictionary() }.getOrDefault(emptyList())
        }
    }

    fun speak(text: String, slow: Boolean = false) = speech.speak(text, slow)
    fun toggleFavorite(en: String) = library.toggleFavorite(en)
    fun recordQuiz(topic: Topic, correct: Int) = library.recordQuiz(topic, correct)
    fun resumeSpeaking() { viewModelScope.launch { settingsRepo.update { it.copy(speakingDisabledUntil = 0, listeningDisabledUntil = 0) } } }

    // Offline reference search (debounced, paged).
    data class ReferenceState(val query: String = "", val page: Int = 0, val rows: List<ReferenceEntry> = emptyList(), val busy: Boolean = false, val error: String? = null) {
        val hasNext: Boolean get() = rows.size > LibraryRepository.PAGE
    }
    val reference = MutableStateFlow(ReferenceState())
    private var searchJob: Job? = null

    fun search(query: String, page: Int = 0) {
        reference.value = reference.value.copy(query = query.take(200), page = page, busy = query.isNotBlank(), error = null, rows = if (page == reference.value.page) reference.value.rows else emptyList())
        searchJob?.cancel()
        if (query.isBlank()) { reference.value = ReferenceState(); return }
        searchJob = viewModelScope.launch {
            delay(300)
            val r = runCatching { library.searchReference(query, page) }
            reference.value = reference.value.copy(
                rows = r.getOrDefault(emptyList()), busy = false,
                error = r.exceptionOrNull()?.let { "Справочник недоступен. Переустановите приложение." },
            )
        }
    }
}
