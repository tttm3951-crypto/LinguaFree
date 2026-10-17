package org.linguafree.core.domain.learning

import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.domain.model.SessionKind

/**
 * Pure state machine of one lesson (or practice/test session).
 * - A wrongly answered exercise returns at the end of the queue (up to [MAX_RETRIES] times).
 * - Counts the current run of correct answers for "5 подряд!".
 * - Test-like sessions don't retry and fail after [maxMistakes].
 */
class LessonSession(
    exercises: List<Exercise>,
    val kind: SessionKind,
    val startedAt: Long,
) {
    data class Item(val exercise: Exercise, val attempt: Int)

    /** Feedback for one submitted answer. */
    data class Outcome(
        val correct: Boolean,
        /** A heart should be taken (wrong answer in a session that spends hearts). */
        val heartLost: Boolean,
        /** Non-null on 5, 10, 15… correct in a row. */
        val comboMilestone: Int?,
        val willRetry: Boolean,
    )

    val maxMistakes: Int? = when (kind) {
        SessionKind.UnitTest, SessionKind.SkipTest -> 3
        SessionKind.Legendary -> 2
        else -> null
    }

    private val queue = ArrayDeque(exercises.map { Item(it, 0) })
    private val firstTryResults = LinkedHashMap<String, Boolean>()

    val total: Int = exercises.size
    var current: Item? = queue.firstOrNull(); private set
    var mistakes = 0; private set
    var combo = 0; private set
    var bestCombo = 0; private set
    var answeredCurrent = false; private set
    var failed = false; private set
    var listeningDone = 0; private set
    var speakingDone = 0; private set
    private var finishedUnique = 0
    private var lastCorrect = true

    /** Mistakes per lexeme, for SRS updates and the mistakes review. */
    val lexemeResults = mutableListOf<Pair<String, Boolean>>()
    val wrongExercises = LinkedHashSet<String>()

    val progress: Float get() = if (total == 0) 1f else finishedUnique.toFloat() / total
    val isFinished: Boolean get() = failed || (current == null)
    val correctFirstTry: Int get() = firstTryResults.values.count { it }
    /** First answers in the order exercises were shown (used by the placement test). */
    val firstTryResultsInOrder: List<Boolean> get() = firstTryResults.values.toList()
    /** Every exercise that got an answer in this session. */
    val answeredIds: Set<String> get() = firstTryResults.keys
    val accuracy: Int get() = if (firstTryResults.isEmpty()) 100 else correctFirstTry * 100 / firstTryResults.size

    fun submit(correct: Boolean, spendsHearts: Boolean = kind.spendsHearts): Outcome {
        val item = current ?: error("Session finished")
        check(!answeredCurrent) { "Already answered" }
        answeredCurrent = true
        lastCorrect = correct
        val ex = item.exercise
        if (item.attempt == 0) firstTryResults[ex.id] = correct
        ex.lexemes.forEach { lexemeResults += it to correct }
        if (correct) {
            combo++
            bestCombo = maxOf(bestCombo, combo)
            if (ex.type.isListening) listeningDone++
            if (ex.type.isSpeaking) speakingDone++
        } else {
            combo = 0
            mistakes++
            wrongExercises += ex.id
            maxMistakes?.let { if (mistakes > it) failed = true }
        }
        val willRetry = !correct && !kind.isTest && item.attempt < MAX_RETRIES
        val milestone = if (correct && combo >= 5 && combo % 5 == 0) combo else null
        return Outcome(correct, heartLost = !correct && spendsHearts, comboMilestone = milestone, willRetry = willRetry)
    }

    /** Moves to the next exercise. Wrong answers are re-queued at the end. */
    fun next() {
        val item = current ?: return
        check(answeredCurrent) { "Answer first" }
        queue.removeFirst()
        if (!lastCorrect && !kind.isTest && item.attempt < MAX_RETRIES) {
            queue.addLast(item.copy(attempt = item.attempt + 1))
        } else {
            finishedUnique++
        }
        answeredCurrent = false
        current = if (failed) null else queue.firstOrNull()
    }

    /** Skip an exercise that can't be done now (e.g. "не могу говорить"): counts as neither right nor wrong. */
    fun skipCurrent() {
        val item = current ?: return
        queue.removeFirst()
        firstTryResults.remove(item.exercise.id)
        finishedUnique++
        answeredCurrent = false
        current = queue.firstOrNull()
    }

    fun durationMs(now: Long) = (now - startedAt).coerceAtLeast(0)

    /** For a placement test: share of correct first answers. */
    fun score(): Int = accuracy

    val passed: Boolean get() = !failed && current == null

    val remainingTypes: List<ExerciseType> get() = queue.map { it.exercise.type }

    companion object {
        const val MAX_RETRIES = 2
    }
}
