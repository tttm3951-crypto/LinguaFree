package org.linguafree.core.domain.learning

import java.util.Locale

/** Result of checking a learner's answer. */
sealed interface Verdict {
    val correct: Boolean
    /** Best matching accepted answer — shown in the feedback panel. */
    val expected: String

    data class Correct(override val expected: String) : Verdict { override val correct = true }
    /** Accepted, but with a small typo worth pointing out. */
    data class Typo(override val expected: String, val typed: String) : Verdict { override val correct = true }
    data class Wrong(override val expected: String) : Verdict { override val correct = false }
}

/**
 * Normalises and compares answers.
 * - case, punctuation, extra spaces and ё/е are ignored;
 * - English contractions are expanded: don't = do not, I'm = I am, can't = cannot…;
 * - typed answers tolerate small typos per word (Levenshtein), but short words must be exact
 *   so that "he" ≠ "she" and "a" ≠ "the".
 */
object AnswerChecker {

    private val contractions: Map<String, String> = mapOf(
        "can't" to "can not", "cannot" to "can not", "won't" to "will not", "shan't" to "shall not",
        "ain't" to "am not", "let's" to "let us",
        "i'm" to "i am", "you're" to "you are", "we're" to "we are", "they're" to "they are",
        "he's" to "he is", "she's" to "she is", "it's" to "it is", "that's" to "that is",
        "what's" to "what is", "where's" to "where is", "who's" to "who is", "there's" to "there is",
        "here's" to "here is", "how's" to "how is",
        "i've" to "i have", "you've" to "you have", "we've" to "we have", "they've" to "they have",
        "i'll" to "i will", "you'll" to "you will", "he'll" to "he will", "she'll" to "she will",
        "it'll" to "it will", "we'll" to "we will", "they'll" to "they will",
        "i'd" to "i would", "you'd" to "you would", "he'd" to "he would", "she'd" to "she would",
        "we'd" to "we would", "they'd" to "they would",
        "isn't" to "is not", "aren't" to "are not", "wasn't" to "was not", "weren't" to "were not",
        "don't" to "do not", "doesn't" to "does not", "didn't" to "did not",
        "haven't" to "have not", "hasn't" to "has not", "hadn't" to "had not",
        "couldn't" to "could not", "wouldn't" to "would not", "shouldn't" to "should not",
        "mustn't" to "must not", "needn't" to "need not",
        // Forms typed without an apostrophe that are not real words themselves.
        "dont" to "do not", "doesnt" to "does not", "didnt" to "did not", "isnt" to "is not",
        "arent" to "are not", "wasnt" to "was not", "werent" to "were not", "cant" to "can not",
        "wont" to "will not", "im" to "i am", "ive" to "i have", "youre" to "you are",
        "theyre" to "they are", "havent" to "have not", "hasnt" to "has not", "hadnt" to "had not",
        "couldnt" to "could not", "wouldnt" to "would not", "shouldnt" to "should not",
        "youve" to "you have", "theyve" to "they have", "weve" to "we have", "whats" to "what is",
        "thats" to "that is", "theres" to "there is",
    )

    /** Lowercase, unify apostrophes and ё, drop punctuation, expand contractions. */
    fun normalize(text: String): String {
        val base = text
            .lowercase(Locale.ROOT)
            .replace('’', '\'').replace('‘', '\'').replace('`', '\'').replace('ʼ', '\'')
            .replace('ё', 'е')
            .replace(Regex("[^\\p{L}\\p{N}' ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (base.isEmpty()) return base
        return base.split(' ')
            .map { w -> contractions[w] ?: w.trim('\'') }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    fun words(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }

    /** Exact comparison after normalisation (used for tiles and choices). */
    fun checkExact(input: String, accepted: List<String>): Verdict {
        require(accepted.isNotEmpty()) { "No accepted answers" }
        val n = normalize(input)
        val hit = accepted.firstOrNull { normalize(it) == n }
        return if (hit != null) Verdict.Correct(hit) else Verdict.Wrong(closest(n, accepted))
    }

    /** Comparison for typed answers with typo tolerance. */
    fun checkTyped(input: String, accepted: List<String>): Verdict {
        require(accepted.isNotEmpty()) { "No accepted answers" }
        val n = normalize(input)
        if (n.isEmpty()) return Verdict.Wrong(accepted.first())
        accepted.firstOrNull { normalize(it) == n }?.let { return Verdict.Correct(it) }
        val typed = n.split(' ')
        for (answer in accepted) {
            val target = normalize(answer).split(' ')
            if (target.size != typed.size) continue
            val ok = target.indices.all { i -> wordWithinTolerance(typed[i], target[i]) }
            if (ok) return Verdict.Typo(answer, input.trim())
        }
        return Verdict.Wrong(closest(n, accepted))
    }

    /** How many typos a word of this length may contain. */
    fun allowedTypos(wordLength: Int): Int = when {
        wordLength <= 3 -> 0
        wordLength <= 7 -> 1
        else -> 2
    }

    private fun wordWithinTolerance(typed: String, target: String): Boolean {
        if (typed == target) return true
        val allowed = allowedTypos(target.length)
        if (allowed == 0) return false
        // Russian/English letters only differ by typos; a digit or different length by > allowed fails.
        return levenshtein(typed, target, allowed) <= allowed
    }

    private fun closest(normalized: String, accepted: List<String>): String =
        accepted.minByOrNull { levenshtein(normalized, normalize(it), Int.MAX_VALUE) } ?: accepted.first()

    /** Levenshtein distance with early exit when it exceeds [limit]. */
    fun levenshtein(a: String, b: String, limit: Int = Int.MAX_VALUE): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                rowMin = minOf(rowMin, cur[j])
            }
            if (rowMin > limit) return limit + 1
            val t = prev; prev = cur; cur = t
        }
        return prev[b.length]
    }

    /**
     * Word-level similarity of recognised speech to the target phrase, 0..100.
     * This measures recognised words, not accent quality.
     */
    fun speechSimilarity(expected: String, heard: String): Int {
        val a = words(expected)
        val b = words(heard)
        if (a.isEmpty() || b.isEmpty()) return 0
        var prev = IntArray(b.size + 1) { it }
        var cur = IntArray(b.size + 1)
        for (i in 1..a.size) {
            cur[0] = i
            for (j in 1..b.size) {
                // A near-miss of a longer word (recogniser spelling, e.g. "colour"/"color") is not an error.
                val cost = when {
                    a[i - 1] == b[j - 1] -> 0
                    a[i - 1].length > 3 && levenshtein(a[i - 1], b[j - 1], 1) <= 1 -> 0
                    else -> 1
                }
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            val t = prev; prev = cur; cur = t
        }
        val distance = prev[b.size]
        return ((1.0 - distance.toDouble() / maxOf(a.size, b.size)) * 100).toInt().coerceIn(0, 100)
    }

    /** Threshold for counting a spoken answer as correct. */
    const val SPEECH_PASS = 70
}
