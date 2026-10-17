package org.linguafree.feature.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.linguafree.core.data.platform.Feedback
import org.linguafree.core.data.platform.SpeechInput
import org.linguafree.core.data.platform.SpeechOutput
import org.linguafree.core.domain.game.HeartsRules
import org.linguafree.core.domain.learning.ExerciseGrader
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.domain.learning.LessonSession
import org.linguafree.core.domain.learning.Verdict
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.FinishSessionUseCase
import org.linguafree.core.domain.usecase.SessionPlan
import org.linguafree.core.domain.usecase.SessionSummary
import org.linguafree.core.domain.usecase.StartSessionUseCase
import org.linguafree.core.domain.usecase.TimeProvider
import org.linguafree.core.ui.mascot.FoxEmotion
import javax.inject.Inject

enum class LessonPhase { Loading, Playing, OutOfHearts, Empty, Summary, TestFailed }

/** Feedback panel after "Проверить". */
data class FeedbackUi(
    val correct: Boolean,
    val title: String,
    val answer: String?,
    val note: String?,
)

data class LessonUiState(
    val phase: LessonPhase = LessonPhase.Loading,
    val kind: SessionKind = SessionKind.Lesson,
    val exercise: Exercise? = null,
    /** Changes for every shown exercise (also for retries) to reset the exercise UI state. */
    val exerciseKey: Int = 0,
    val isRetry: Boolean = false,
    val progress: Float = 0f,
    val hearts: Int = HeartsRules.MAX,
    val heartsEnabled: Boolean = true,
    val gems: Int = 0,
    val answer: LearnerAnswer? = null,
    val feedback: FeedbackUi? = null,
    val combo: Int = 0,
    val comboBanner: Int? = null,
    val fox: FoxEmotion = FoxEmotion.Waiting,
    val summary: SessionSummary? = null,
    val msToNextHeart: Long? = null,
    val exitConfirm: Boolean = false,
    val hints: Map<String, String> = emptyMap(),
    val mistakesLeft: Int? = null,
    val message: String? = null,
)

@HiltViewModel
class LessonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val startSession: StartSessionUseCase,
    private val finishSession: FinishSessionUseCase,
    private val content: ContentRepository,
    private val game: GameRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
    val speechOut: SpeechOutput,
    val speechIn: SpeechInput,
    val feedback: Feedback,
) : ViewModel() {

    val kind: SessionKind = SessionKind.of(savedState.get<String>(LessonRoute.ARG_KIND) ?: "lesson")
    val nodeId: String? = savedState.get<String>(LessonRoute.ARG_NODE)?.takeIf { it.isNotBlank() }
    val unitId: String? = savedState.get<String>(LessonRoute.ARG_UNIT)?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(LessonUiState(kind = kind))
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    private var session: LessonSession? = null
    private var placementUnits: List<String?> = emptyList()
    private var key = 0
    private var bannerJob: Job? = null

    init {
        viewModelScope.launch {
            val s = settings.current()
            feedback.soundEnabled = s.soundEnabled
            feedback.hapticsEnabled = s.hapticsEnabled
            speechOut.speed = s.voiceSpeed
            start()
        }
    }

    private suspend fun start() {
        _state.update { it.copy(phase = LessonPhase.Loading) }
        val hints = runCatching { content.hints() }.getOrDefault(emptyMap())
        when (val plan = startSession(kind, nodeId, unitId)) {
            is SessionPlan.NoHearts -> _state.update {
                it.copy(phase = LessonPhase.OutOfHearts, msToNextHeart = plan.msToNextHeart, gems = game.walletNow().gems, hints = hints)
            }
            SessionPlan.Empty -> _state.update { it.copy(phase = LessonPhase.Empty, hints = hints) }
            is SessionPlan.Ready -> {
                val sess = LessonSession(plan.exercises, plan.kind, time.now())
                session = sess
                if (kind == SessionKind.Placement) {
                    val unitOf = content.course().sections.flatMap { s -> s.units.flatMap { u -> u.nodes.map { it.id to u.id } } }.toMap()
                    placementUnits = plan.exercises.map { unitOf[it.nodeId] }
                }
                val w = game.updateWallet { HeartsRules.regenerate(it, time.now()) }
                _state.update {
                    it.copy(
                        phase = LessonPhase.Playing, heartsEnabled = plan.heartsEnabled, hearts = w.hearts, gems = w.gems,
                        hints = hints, mistakesLeft = sess.maxMistakes,
                    )
                }
                showCurrent()
            }
        }
    }

    private fun showCurrent() {
        val sess = session ?: return
        val item = sess.current ?: return
        key++
        _state.update {
            it.copy(
                exercise = item.exercise, exerciseKey = key, isRetry = item.attempt > 0, progress = sess.progress,
                answer = null, feedback = null, fox = idleFox(item.exercise.type), message = null,
            )
        }
        // Listening and some prompts start with audio, like a teacher reading aloud.
        val ex = item.exercise
        if (ex.type.isListening || ex.type == ExerciseType.SelectImage) ex.audio?.let { speechOut.speak(it) }
    }

    private fun idleFox(type: ExerciseType) = when (type) {
        ExerciseType.ListenTiles, ExerciseType.ListenType -> FoxEmotion.Listening
        ExerciseType.Speak -> FoxEmotion.Speaking
        ExerciseType.MatchPairs, ExerciseType.FillGap, ExerciseType.TypeTranslation -> FoxEmotion.Thinking
        else -> FoxEmotion.Waiting
    }

    // ---------- Events from the UI ----------

    fun onAnswerChanged(answer: LearnerAnswer?) {
        if (_state.value.feedback != null) return
        _state.update { it.copy(answer = answer, fox = if (answer != null) FoxEmotion.Thinking else it.fox) }
    }

    fun check() {
        val answer = _state.value.answer ?: return
        submit(answer)
    }

    /** Self-checking exercises (pairs, dialogue, speech) submit directly. */
    fun submit(answer: LearnerAnswer) {
        val sess = session ?: return
        val ex = sess.current?.exercise ?: return
        if (_state.value.feedback != null) return
        val verdict = ExerciseGrader.grade(ex, answer)
        val outcome = sess.submit(verdict.correct, spendsHearts = _state.value.heartsEnabled)
        if (verdict.correct) feedback.correct() else feedback.wrong()
        if (outcome.comboMilestone != null) feedback.combo()
        // Speak the right answer after a translation into English, like a tutor.
        if (ex.answerIsEnglish && ex.type != ExerciseType.Speak && ex.type != ExerciseType.MatchPairs && ex.type != ExerciseType.Dialogue) {
            speechOut.speak(verdict.expected)
        }
        viewModelScope.launch {
            var hearts = _state.value.hearts
            if (outcome.heartLost) hearts = game.updateWallet { HeartsRules.loseHeart(it, time.now()) }.hearts
            _state.update {
                it.copy(
                    answer = answer,
                    feedback = feedbackFor(ex, answer, verdict, outcome.willRetry),
                    hearts = hearts,
                    combo = sess.combo,
                    fox = when {
                        outcome.comboMilestone != null -> FoxEmotion.Streak
                        verdict.correct -> FoxEmotion.Correct
                        else -> FoxEmotion.TryAgain
                    },
                    mistakesLeft = sess.maxMistakes?.let { m -> (m - sess.mistakes).coerceAtLeast(0) },
                )
            }
            outcome.comboMilestone?.let { showBanner(it) }
        }
    }

    private fun feedbackFor(ex: Exercise, answer: LearnerAnswer, v: Verdict, retry: Boolean): FeedbackUi = when (v) {
        is Verdict.Correct -> FeedbackUi(true, praise(), null, when {
            answer is LearnerAnswer.Spoken -> ExerciseGrader.bestSpoken(ex, answer.alternatives).let { (t, s) -> "Распознано: «$t» · совпадение слов $s%" }
            ex.type == ExerciseType.MatchPairs && answer is LearnerAnswer.Completed && answer.mistakes > 0 -> "Пары найдены, ошибок: ${answer.mistakes}"
            else -> null
        })
        is Verdict.Typo -> FeedbackUi(true, "Верно, но есть опечатка", v.expected, "Обрати внимание на написание.")
        is Verdict.Wrong -> FeedbackUi(
            false, "Неправильно", v.expected.takeIf { ex.type != ExerciseType.Dialogue && ex.type != ExerciseType.MatchPairs },
            when {
                answer is LearnerAnswer.Spoken -> ExerciseGrader.bestSpoken(ex, answer.alternatives).let { (t, s) -> "Распознано: «$t» · совпадение слов $s%. Это проверка слов, а не акцента." }
                retry -> "Это задание вернётся в конце урока."
                else -> null
            },
        )
    }

    private val praises = listOf("Отлично!", "Верно!", "Супер!", "Так держать!", "Великолепно!", "Здорово!", "Умница!")
    private fun praise() = praises[(key * 7) % praises.size]

    private fun showBanner(combo: Int) {
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            _state.update { it.copy(comboBanner = combo) }
            delay(1800)
            _state.update { it.copy(comboBanner = null) }
        }
    }

    fun continueNext() {
        val sess = session ?: return
        if (_state.value.feedback == null) return
        speechOut.stop()
        speechIn.cancel()
        sess.next()
        val heartsEnabled = _state.value.heartsEnabled
        when {
            heartsEnabled && _state.value.hearts <= 0 -> outOfHearts()
            sess.isFinished -> finish()
            else -> showCurrent()
        }
    }

    /** «Не могу говорить/слушать сейчас»: hides those exercises for an hour and skips this one. */
    fun cantDoNow(speaking: Boolean) {
        val sess = session ?: return
        val until = time.now() + 60 * 60 * 1000L
        viewModelScope.launch {
            settings.update { if (speaking) it.copy(speakingDisabledUntil = until) else it.copy(listeningDisabledUntil = until) }
        }
        speechIn.cancel()
        speechOut.stop()
        // Remove the current and any remaining exercises of that kind.
        sess.skipCurrent()
        while (sess.current?.exercise?.type?.let { if (speaking) it.isSpeaking else it.isListening } == true) sess.skipCurrent()
        _state.update { it.copy(message = if (speaking) "Задания на говорение скрыты на 1 час" else "Задания на аудирование скрыты на 1 час") }
        if (sess.isFinished) finish() else showCurrent()
    }

    private fun outOfHearts() {
        viewModelScope.launch {
            val w = game.walletNow()
            _state.update { it.copy(phase = LessonPhase.OutOfHearts, msToNextHeart = HeartsRules.msToNext(w, time.now()), gems = w.gems, feedback = null) }
        }
    }

    private fun finish() {
        val sess = session ?: return
        if (sess.failed) {
            _state.update { it.copy(phase = LessonPhase.TestFailed, feedback = null, fox = FoxEmotion.TryAgain) }
            return
        }
        viewModelScope.launch {
            val summary = finishSession(sess, nodeId, unitId, placementUnits)
            feedback.complete()
            _state.update { it.copy(phase = LessonPhase.Summary, summary = summary, feedback = null, progress = 1f, fox = FoxEmotion.Complete) }
        }
    }

    fun speak(text: String, slow: Boolean = false) = speechOut.speak(text, slow)

    fun requestExit() { _state.update { it.copy(exitConfirm = true) } }
    fun dismissExit() { _state.update { it.copy(exitConfirm = false) } }

    fun retry() {
        session = null
        viewModelScope.launch { start() }
    }

    fun dismissMessage() { _state.update { it.copy(message = null) } }

    override fun onCleared() {
        speechOut.stop()
        speechIn.cancel()
        super.onCleared()
    }
}
