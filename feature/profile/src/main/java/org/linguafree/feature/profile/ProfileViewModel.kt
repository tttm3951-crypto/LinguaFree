package org.linguafree.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.linguafree.core.data.backup.BackupManager
import org.linguafree.core.data.backup.TeacherTools
import org.linguafree.core.data.platform.Feedback
import org.linguafree.core.data.platform.SpeechOutput
import org.linguafree.core.domain.game.Achievements
import org.linguafree.core.domain.game.LeagueRules
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.model.AchievementState
import org.linguafree.core.domain.model.StreakState
import org.linguafree.core.domain.model.UserSettings
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.ProgressRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.Counters
import org.linguafree.core.domain.usecase.TimeProvider
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class ProfileUiState(
    val settings: UserSettings = UserSettings(),
    val streak: Int = 0,
    val longest: Int = 0,
    val streakState: StreakState = StreakState(emptySet(), emptySet(), 0, null),
    val totalXp: Int = 0,
    val lessonsDone: Int = 0,
    val wordsLearned: Int = 0,
    val wordsSeen: Int = 0,
    val leagueTier: Int = 0,
    val achievements: List<AchievementState> = emptyList(),
    val since: LocalDate? = null,
    val inventory: Set<String> = emptySet(),
    val loaded: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progress: ProgressRepository,
    private val game: GameRepository,
    private val settingsRepo: SettingsRepository,
    private val backup: BackupManager,
    private val teacher: TeacherTools,
    val speech: SpeechOutput,
    private val feedback: Feedback,
    private val time: TimeProvider,
) : ViewModel() {

    val message = MutableStateFlow<String?>(null)
    val students = MutableStateFlow(teacher.students())
    private val top3 = MutableStateFlow(0)

    val state: StateFlow<ProfileUiState> = combine(
        combine(settingsRepo.settings, progress.results(), game.streak()) { s, r, st -> Triple(s, r, st) },
        progress.nodeProgress(),
        progress.lexemeStates(time.now()),
        combine(game.leagueTier(), game.claimedAchievements(), game.counters(), game.inventory()) { t, a, c, i -> listOf(t, a, c, i) },
        top3,
    ) { (s, results, st), nodes, lexemes, misc, champion ->
        val today = time.today()
        @Suppress("UNCHECKED_CAST") val claimed = misc[1] as Map<String, Int>
        @Suppress("UNCHECKED_CAST") val counters = misc[2] as Map<String, Int>
        @Suppress("UNCHECKED_CAST") val inventory = misc[3] as Set<String>
        val current = StreakRules.current(st.studyDays, st.frozenDays, today)
        val learned = lexemes.count { it.right >= 2 && it.strength >= 0.5 }
        val stats = mapOf(
            "fire" to maxOf(st.longest, current),
            "sage" to results.sumOf { it.xp },
            "perfect" to results.count { it.passed && it.perfect && it.kind.spendsHearts },
            "words" to learned,
            "scholar" to nodes.values.count { it.completed },
            "champion" to champion,
            "storyteller" to (counters[Counters.STORIES] ?: 0),
            "speaker" to results.sumOf { it.speakingDone },
            "quests" to (counters[Counters.QUESTS_DONE] ?: 0),
        )
        ProfileUiState(
            settings = s, streak = current, longest = maxOf(st.longest, current), streakState = st,
            totalXp = results.sumOf { it.xp }, lessonsDone = nodes.values.count { it.completed },
            wordsLearned = learned, wordsSeen = lexemes.size, leagueTier = misc[0] as Int,
            achievements = Achievements.all.map { Achievements.state(it, stats[it.id] ?: 0, claimed[it.id] ?: 0) },
            since = s.createdAt.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(time.zone()).toLocalDate() },
            inventory = inventory, loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init {
        viewModelScope.launch { top3.value = game.leagueState().top3Count }
    }

    val leagueTitle: String get() = LeagueRules.tiers[state.value.leagueTier.coerceIn(0, 9)].title

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
            val s = settingsRepo.current()
            feedback.soundEnabled = s.soundEnabled
            feedback.hapticsEnabled = s.hapticsEnabled
            speech.speed = s.voiceSpeed
        }
    }

    fun exportTo(write: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
                write(backup.export(version))
            }.onSuccess { message.value = "Резервная копия сохранена. Храни файл в надёжном месте." }
                .onFailure { message.value = "Не удалось сохранить: ${it.message}" }
        }
    }

    fun importFrom(text: String?) {
        viewModelScope.launch {
            message.value = when {
                text == null -> "Не удалось прочитать файл"
                else -> when (val r = backup.import(text)) {
                    BackupManager.ImportResult.Ok -> "Прогресс восстановлен!"
                    is BackupManager.ImportResult.Error -> r.message
                }
            }
        }
    }

    fun studyReport(write: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { write(teacher.studyReport()) }
                .onSuccess { message.value = "Отчёт сохранён. Отправь файл учителю удобным способом." }
                .onFailure { message.value = "Не удалось сохранить отчёт: ${it.message}" }
        }
    }

    fun importReport(text: String?) {
        message.value = runCatching { teacher.importReport(text ?: error("Пустой файл")) }.getOrElse { "Ошибка импорта: ${it.message}" }
        students.value = teacher.students()
    }

    fun deleteStudent(key: String) {
        teacher.deleteStudent(key)
        students.value = teacher.students()
    }

    fun resetSpeaking() = update { it.copy(speakingDisabledUntil = 0, listeningDisabledUntil = 0) }
    fun dismissMessage() { message.value = null }
    fun testVoice() = speech.speak("Hello! Let's learn English together!")
}
