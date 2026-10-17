package org.linguafree.core.data.library

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.linguafree.core.data.migration.LegacyMigration
import java.io.File

data class DictEntry(
    val en: String,
    val ru: String,
    val level: String,
    val source: String,
    val exampleEn: String,
    val exampleRu: String,
)

data class TopicQuestion(
    val prompt: String,
    val choices: List<String>,
    val answer: Int,
    val speech: String?,
    val explanation: String?,
)

enum class TopicBook(val prefix: String, val title: String) {
    Beginner("beginner", "С нуля"),
    Grammar("grammar", "Грамматика"),
}

data class Topic(
    val id: String,
    val book: TopicBook,
    val title: String,
    val level: String,
    val group: String,
    val rule: String,
    val examples: List<String>,
    val questions: List<TopicQuestion>,
)

data class ReferenceEntry(val id: Long, val en: String, val ru: String, val credit: String)

/**
 * The offline library carried over from LinguaFree 0.9: the learner dictionary, grammar and
 * «с нуля» topics, and the bundled 150 000-pair reference (SQLite FTS, built in CI).
 * Learner state lives in the 0.9 preference keys, so backups and old installs keep working.
 */
class LibraryRepository(private val context: Context) {
    private val prefs get() = context.getSharedPreferences(LegacyMigration.LEGACY_PREFS, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var dictionaryCache: List<DictEntry>? = null
    private var topicsCache: List<Topic>? = null

    private val _favorites = MutableStateFlow(prefs.getStringSet(KEY_FAVORITES, emptySet())!!.toSet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _mastered = MutableStateFlow(readMastered())
    /** Ids of topics whose quiz was passed (both books). */
    val mastered: StateFlow<Set<String>> = _mastered.asStateFlow()

    private fun readMastered(): Set<String> =
        TopicBook.entries.flatMap { prefs.getStringSet("${it.prefix}Mastered", emptySet())!! }.toSet()

    /** Re-reads learner state after a backup import. */
    fun reload() {
        _favorites.value = prefs.getStringSet(KEY_FAVORITES, emptySet())!!.toSet()
        _mastered.value = readMastered()
    }

    suspend fun dictionary(): List<DictEntry> = mutex.withLock {
        dictionaryCache ?: withContext(Dispatchers.IO) {
            val arr = JSONArray(read("library/dictionary.json"))
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DictEntry(o.getString("en"), o.getString("ru"), o.optString("level"), o.optString("source"), o.optString("example_en"), o.optString("example_ru"))
            }.sortedBy { it.en.lowercase() }
        }.also { dictionaryCache = it }
    }

    suspend fun topics(): List<Topic> = mutex.withLock {
        topicsCache ?: withContext(Dispatchers.IO) {
            readTopics("library/beginner.json", TopicBook.Beginner) + readTopics("library/grammar.json", TopicBook.Grammar)
        }.also { topicsCache = it }
    }

    private fun readTopics(path: String, book: TopicBook): List<Topic> {
        val arr = JSONArray(read(path))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val qs = o.getJSONArray("questions")
            val ex = o.getJSONArray("examples")
            Topic(
                id = o.getString("id"), book = book, title = o.getString("title"), level = o.optString("level"),
                group = o.optString("group"), rule = o.getString("rule"),
                examples = (0 until ex.length()).map { ex.getString(it) },
                questions = (0 until qs.length()).map { j ->
                    val q = qs.getJSONObject(j)
                    val ch = q.getJSONArray("choices")
                    TopicQuestion(
                        prompt = q.getString("prompt"), choices = (0 until ch.length()).map { ch.getString(it) }, answer = q.getInt("answer"),
                        speech = q.optString("speech").ifBlank { null }, explanation = q.optString("explanation").ifBlank { null },
                    )
                },
            )
        }
    }

    fun toggleFavorite(en: String) {
        val next = _favorites.value.let { if (en in it) it - en else it + en }
        prefs.edit().putStringSet(KEY_FAVORITES, next).apply()
        _favorites.value = next
    }

    /** Records a quiz result; a topic counts as mastered at ≥ 80% correct. Returns true when mastered. */
    fun recordQuiz(topic: Topic, correct: Int): Boolean {
        val total = topic.questions.size.coerceAtLeast(1)
        val bestKey = "${topic.book.prefix}Best"
        val best = prefs.getString(bestKey, "").orEmpty().split(';').filter { ':' in it }
            .associate { it.substringBefore(':') to (it.substringAfter(':').toIntOrNull() ?: 0) }.toMutableMap()
        best[topic.id] = maxOf(best[topic.id] ?: 0, correct)
        val passed = correct * 100 >= 80 * total
        val key = "${topic.book.prefix}Mastered"
        val set = prefs.getStringSet(key, emptySet())!!.toMutableSet()
        if (passed) set += topic.id
        prefs.edit().putString(bestKey, best.entries.joinToString(";") { "${it.key}:${it.value}" }).putStringSet(key, set).apply()
        _mastered.value = readMastered()
        return passed
    }

    /** True when the CI-built reference database is bundled in this APK. */
    val referenceAvailable: Boolean by lazy { runCatching { context.assets.open(REFERENCE_ASSET).close() }.isSuccess }

    suspend fun searchReference(query: String, page: Int): List<ReferenceEntry> = withContext(Dispatchers.IO) {
        val words = Regex("[\\p{L}\\p{N}]+").findAll(query.take(200)).take(8).map { it.value + "*" }.toList()
        if (words.isEmpty() || !referenceAvailable) return@withContext emptyList()
        openReference().use { db ->
            db.rawQuery(
                "SELECT e.id,e.en,e.ru,e.credit FROM search JOIN entries e ON e.id=search.docid WHERE search MATCH ? ORDER BY e.kind, e.id LIMIT ? OFFSET ?",
                arrayOf(words.joinToString(" AND "), (PAGE + 1).toString(), (page.coerceIn(0, 3000) * PAGE).toString()),
            ).use { c -> buildList { while (c.moveToNext()) add(ReferenceEntry(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))) } }
        }
    }

    @Synchronized
    private fun openReference(): SQLiteDatabase {
        val file = context.getDatabasePath(REFERENCE_FILE)
        // Re-unpack after an app update so a newer bundled reference replaces the old copy.
        val installed = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime }.getOrDefault(0L)
        val marker = context.getSharedPreferences("linguafree_library", Context.MODE_PRIVATE)
        if (file.exists() && marker.getLong("referenceFor", -1L) != installed) file.delete()
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            val tmp = File(file.path + ".tmp")
            context.assets.open(REFERENCE_ASSET).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(tmp.renameTo(file)) { "Не удалось подготовить справочник" }
            marker.edit().putLong("referenceFor", installed).apply()
        }
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun read(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        const val PAGE = 50
        private const val KEY_FAVORITES = "dictionaryFavorites"
        private const val REFERENCE_ASSET = "offline-reference.db"
        /** Same file name as 0.9, so an upgraded install reuses the unpacked copy. */
        private const val REFERENCE_FILE = "offline-reference-v1.db"
    }
}
