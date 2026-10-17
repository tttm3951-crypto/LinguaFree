package org.linguafree.core.domain.learning

import org.linguafree.core.domain.model.Course
import org.linguafree.core.domain.model.Direction
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseOption
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.domain.model.NodeType
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.model.WordPair
import kotlin.random.Random

/**
 * Builds the exercise list for any session. Pure: all inputs are passed in.
 *
 * Methodology:
 * - lessons introduce ≤ 5 new words (checked by the content validator);
 * - adaptivity: if the previous lesson's accuracy was < 70%, the next one gets more review;
 * - practice always targets the weakest lexemes (lowest predicted recall, HLR).
 */
class SessionComposer(
    private val course: Course,
    private val exercisesByNode: Map<String, List<Exercise>>,
    private val memory: Map<String, Srs.Memory>,
    private val completedNodes: Set<String>,
    private val lexemeTranslations: Map<String, Pair<String, String>>,
    private val now: Long,
    private val speakingEnabled: Boolean,
    private val listeningEnabled: Boolean = true,
    private val random: Random = Random(now / 60_000),
) {
    private val nodeOrder: List<String> = course.sections.flatMap { s -> s.units.flatMap { u -> u.nodes.map { it.id } } }
    private val unitOfNode: Map<String, String> = course.sections.flatMap { s -> s.units.flatMap { u -> u.nodes.map { it.id to u.id } } }.toMap()

    /** Exercises the learner has already met (from completed nodes). */
    private val learnedExercises: List<Exercise> by lazy {
        nodeOrder.filter { it in completedNodes }.flatMap { exercisesByNode[it].orEmpty() }.filter(::allowed)
    }

    private fun allowed(e: Exercise): Boolean =
        (speakingEnabled || !e.type.isSpeaking) && (listeningEnabled || !e.type.isListening)

    fun compose(kind: SessionKind, nodeId: String?, unitId: String?, lastAccuracy: Int?, mistakeIds: List<String> = emptyList()): List<Exercise> {
        return when (kind) {
            SessionKind.Lesson, SessionKind.Story -> lesson(nodeId!!, lastAccuracy)
            SessionKind.Legendary -> exercisesByNode[nodeId!!].orEmpty().filter(::allowed).shuffled(random)
            SessionKind.UnitReview -> unitSample(unitId!!, 12, preferWeak = true)
            SessionKind.UnitTest, SessionKind.SkipTest -> unitSample(unitId!!, 14, preferWeak = false)
            SessionKind.Placement -> placement()
            SessionKind.PracticeMistakes -> mistakes(mistakeIds)
            SessionKind.PracticeWeakWords, SessionKind.HeartsRefill -> weakWords(if (kind == SessionKind.HeartsRefill) 8 else 12)
            SessionKind.PracticeListening -> listening(10)
            SessionKind.PracticeSpeaking -> speaking(8)
            SessionKind.PracticeMatch -> matchRounds(3)
            SessionKind.Imported -> emptyList()
        }
    }

    private fun lesson(nodeId: String, lastAccuracy: Int?): List<Exercise> {
        val own = exercisesByNode[nodeId].orEmpty().filter(::allowed)
        val node = course.sections.flatMap { s -> s.units.flatMap { it.nodes } }.firstOrNull { it.id == nodeId }
        if (node?.type == NodeType.Story) return own
        val reviewCount = when {
            lastAccuracy == null -> 0
            lastAccuracy < 70 -> 4
            else -> 1
        }
        if (reviewCount == 0) return own
        val ownIds = own.map { it.id }.toSet()
        val review = weakestExercises(learnedExercises.filter { it.id !in ownIds && it.type != ExerciseType.Dialogue }, reviewCount)
        if (review.isEmpty()) return own
        // Review goes in the middle and near the end, never first.
        val out = own.toMutableList()
        review.forEachIndexed { i, ex ->
            val pos = ((i + 1) * out.size / (review.size + 1)).coerceIn(1, out.size)
            out.add(pos, ex)
        }
        return out
    }

    private fun weakestExercises(pool: List<Exercise>, n: Int): List<Exercise> {
        if (pool.isEmpty()) return emptyList()
        fun score(e: Exercise): Double = if (e.lexemes.isEmpty()) 0.6 else e.lexemes.minOf { Srs.strength(memory[it], now) }
        return pool.distinctBy { it.id }
            .map { it to score(it) + random.nextDouble(0.0, 0.08) }
            .sortedBy { it.second }
            .map { it.first }
            .distinctBy { it.lexemes.firstOrNull() ?: it.id }
            .take(n)
    }

    private fun unitSample(unitId: String, n: Int, preferWeak: Boolean): List<Exercise> {
        val unit = course.sections.flatMap { it.units }.first { it.id == unitId }
        val pool = unit.nodes.filter { it.type == NodeType.Lesson }
            .flatMap { exercisesByNode[it.id].orEmpty() }
            .filter { allowed(it) && it.type != ExerciseType.Dialogue }
        if (pool.isEmpty()) return emptyList()
        val chosen = if (preferWeak) weakestExercises(pool, n) else
            pool.groupBy { it.type }.values.flatMap { it.shuffled(random).take(3) }.shuffled(random).take(n)
        return (if (chosen.size < n) chosen + pool.filter { it !in chosen }.shuffled(random).take(n - chosen.size) else chosen)
    }

    /** One exercise from evenly spaced units, easiest first; ≤ 15 items. */
    private fun placement(): List<Exercise> {
        val units = course.sections.flatMap { it.units }
        if (units.isEmpty()) return emptyList()
        val step = maxOf(1, units.size / 15)
        return units.filterIndexed { i, _ -> i % step == 0 }.take(15).mapNotNull { u ->
            u.nodes.filter { it.type == NodeType.Lesson }
                .flatMap { exercisesByNode[it.id].orEmpty() }
                .filter { it.type == ExerciseType.SelectTranslation || it.type == ExerciseType.Tiles || it.type == ExerciseType.FillGap }
                .shuffled(random).firstOrNull()
        }
    }

    /** Unit ids sampled by [placement] in order — to translate a placement score into a start point. */
    fun placementUnitIds(exercises: List<Exercise>): List<String?> = exercises.map { unitOfNode[it.nodeId] }

    private fun mistakes(ids: List<String>): List<Exercise> {
        val all = exercisesByNode.values.flatten().associateBy { it.id }
        val list = ids.mapNotNull { all[it] }.filter(::allowed).distinctBy { it.id }.take(12)
        return if (list.size >= 6) list else list + weakWords(12 - list.size).filter { it.id !in list.map { e -> e.id } }
    }

    private fun weakWords(n: Int): List<Exercise> {
        val pool = learnedExercises.filter { it.type != ExerciseType.Dialogue }
        val picked = weakestExercises(pool, n)
        if (picked.size >= n || pool.isEmpty()) return picked.ifEmpty { firstLessonFallback(n) }
        return picked + pool.filter { it !in picked }.shuffled(random).take(n - picked.size)
    }

    /** Before anything is learned, practice uses the first lesson so the screen is never empty. */
    private fun firstLessonFallback(n: Int): List<Exercise> =
        nodeOrder.firstOrNull()?.let { exercisesByNode[it] }.orEmpty().filter(::allowed).take(n)

    /** Real listening exercises plus English sentences turned into "listen and type". */
    private fun listening(n: Int): List<Exercise> {
        val pool = learnedExercises.ifEmpty { firstLessonFallback(20) }
        val native = pool.filter { it.type.isListening }
        val converted = pool.filter { it.type in setOf(ExerciseType.Tiles, ExerciseType.TypeTranslation) && it.direction == Direction.RuToEn }
            .map { asListenType(it) }
        return weakestExercises(native + converted, n)
    }

    private fun speaking(n: Int): List<Exercise> {
        val pool = learnedExercises.ifEmpty { firstLessonFallback(20) }
        val native = pool.filter { it.type.isSpeaking }
        val converted = pool.filter { it.type in setOf(ExerciseType.Tiles, ExerciseType.TypeTranslation, ExerciseType.ListenType, ExerciseType.ListenTiles) && it.answerIsEnglish }
            .map { asSpeak(it) }
        return weakestExercises((native + converted).filter { AnswerChecker.words(it.text).size in 1..9 }, n)
    }

    private fun asListenType(e: Exercise) = e.copy(
        id = e.id + "~listen", type = ExerciseType.ListenType, text = e.answers.first(),
        audio = e.answers.first(), options = emptyList(), instruction = null,
    )

    private fun asSpeak(e: Exercise): Exercise {
        val phrase = if (e.type.isListening) e.answers.first() else e.answers.first()
        return e.copy(id = e.id + "~speak", type = ExerciseType.Speak, text = phrase, audio = phrase, answers = listOf(phrase), options = emptyList(), instruction = null)
    }

    /** Timed pair matching from the weakest known lexemes: [rounds] × 5 pairs. */
    private fun matchRounds(rounds: Int): List<Exercise> {
        val known = memory.keys.filter { it in lexemeTranslations }
        val ids = Srs.weakest(known, { memory[it] }, now, rounds * 5 + 5).ifEmpty { lexemeTranslations.keys.take(rounds * 5) }
        return ids.chunked(5).filter { it.size == 5 }.take(rounds).mapIndexed { i, chunk ->
            Exercise(
                id = "match-$now-$i", nodeId = "practice", order = i, type = ExerciseType.MatchPairs, text = "",
                pairs = chunk.map { val (en, ru) = lexemeTranslations.getValue(it); WordPair(en, ru) },
                lexemes = chunk,
            )
        }
    }

    companion object {
        /** Distractor helper for generated choice exercises. */
        fun options(correct: String, pool: List<String>, count: Int, random: Random): List<ExerciseOption> =
            (pool.filter { !it.equals(correct, true) }.distinct().shuffled(random).take(count - 1) + correct)
                .shuffled(random).map { ExerciseOption(it) }
    }
}
