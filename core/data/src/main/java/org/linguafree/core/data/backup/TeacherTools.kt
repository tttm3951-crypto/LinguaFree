package org.linguafree.core.data.backup

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.linguafree.core.data.db.LinguaFreeDatabase
import org.linguafree.core.data.migration.LegacyMigration
import org.linguafree.core.data.settings.SettingsRepositoryImpl
import org.linguafree.core.domain.model.SessionKind
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Offline teacher/parent tools carried over from LinguaFree 0.9: the learner saves a study
 * report file and sends it however they like; a teacher imports reports on their phone.
 * Nothing is uploaded anywhere. Report format is compatible with 0.9 (LinguaFreeStudyReport v1).
 */
class TeacherTools(
    private val context: Context,
    private val db: LinguaFreeDatabase,
    private val settings: SettingsRepositoryImpl,
) {
    data class StudentSummary(
        val key: String, val name: String, val lessons: Int, val totalLessons: Int, val xp: Int,
        val days7: Int, val days30: Int, val lastDate: String, val mistakes: Int, val reports: Int,
        val xpDelta: Int?, val lessonsDelta: Int?, val updated: String, val legacyId: Boolean,
    )

    private val legacy get() = context.getSharedPreferences(LegacyMigration.LEGACY_PREFS, Context.MODE_PRIVATE)
    private val teacherPrefs get() = context.getSharedPreferences("linguafree_teacher", Context.MODE_PRIVATE)

    private fun learnerId(): String {
        val existing = legacy.getString("learnerId", null)
        if (existing != null && existing.matches(UUID_RE)) return existing
        return UUID.randomUUID().toString().also { legacy.edit().putString("learnerId", it).apply() }
    }

    suspend fun studyReport(): String {
        val s = settings.current()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val days = db.game().streakDays().filter { !it.frozen }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.sorted()
        val lessons = db.content().lessons().filter { it.type == "lesson" }
        val done = db.progress().progress().filter { it.completed }.map { it.lessonId }.toSet()
        val results = db.progress().results()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        fun count(kinds: Set<String>, field: (org.linguafree.core.data.db.LessonResultEntity) -> Int) = results.filter { it.kind in kinds || kinds.isEmpty() }.sumOf(field)
        val report = JSONObject()
            .put("format", "LinguaFreeStudyReport").put("version", 1)
            .put("learnerId", learnerId())
            .put("generatedAt", OffsetDateTime.now().toString())
            .put("student", s.name.ifBlank { "Ученик" })
            .put("completedLessons", lessons.count { it.id in done })
            .put("totalLessons", lessons.size)
            .put("xp", results.sumOf { it.xp })
            .put("studyDaysTotal", days.size)
            .put("studyDaysLast7", days.count { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) })
            .put("studyDaysLast30", days.count { !it.isBefore(today.minusDays(29)) && !it.isAfter(today) })
            .put("lastStudyDate", days.lastOrNull()?.toString() ?: "")
            .put("dailyGoal", s.dailyGoalXp)
            .put("todayCompleted", results.filter { it.finishedAt >= todayStart }.sumOf { it.xp })
            .put("mistakesPending", db.progress().mistakes().size)
            .put("grammarTopicsCompleted", (legacy.getStringSet("grammarMastered", emptySet())?.size ?: 0) + (legacy.getStringSet("beginnerMastered", emptySet())?.size ?: 0))
            .put("skills", JSONArray(listOf(
                JSONObject().put("name", "Уроки и истории").put("attempts", count(setOf(SessionKind.Lesson.key, SessionKind.Story.key)) { it.total }).put("correct", count(setOf(SessionKind.Lesson.key, SessionKind.Story.key)) { it.correct }),
                JSONObject().put("name", "Аудирование").put("attempts", count(emptySet()) { it.listeningDone }).put("correct", count(emptySet()) { it.listeningDone }),
                JSONObject().put("name", "Говорение").put("attempts", count(emptySet()) { it.speakingDone }).put("correct", count(emptySet()) { it.speakingDone }),
                JSONObject().put("name", "Практика").put("attempts", results.count { SessionKind.of(it.kind).isPractice }).put("correct", results.count { SessionKind.of(it.kind).isPractice && it.passed }),
            )))
            .put("studyDatesLast30", JSONArray(days.filter { !it.isBefore(today.minusDays(29)) }.map { it.toString() }))
        return report.toString(2)
    }

    fun importReport(text: String): String {
        require(text.length <= 100_000) { "Слишком большой файл" }
        val report = JSONObject(text)
        require(report.optString("format") == "LinguaFreeStudyReport" && report.optInt("version") == 1) { "Это не отчёт ученика LinguaFree" }
        val name = report.optString("student").trim()
        require(name.isNotBlank() && name.length <= 80) { "Некорректное имя ученика" }
        require(report.optInt("completedLessons", -1) >= 0 && report.optInt("xp", -1) in 0..10_000_000) { "Некорректные показатели" }
        val generated = OffsetDateTime.parse(report.getString("generatedAt"))
        require(!generated.isAfter(OffsetDateTime.now().plusDays(1))) { "Некорректная дата отчёта" }
        val all = reports().toMutableList()
        val key = keyOf(report)
        if (all.any { keyOf(it) == key && it.optString("generatedAt") == report.getString("generatedAt") }) return "Этот отчёт уже загружен."
        all += report
        save(all.takeLast(250))
        return "Отчёт ученика «$name» добавлен. Данные хранятся только на этом телефоне."
    }

    fun students(): List<StudentSummary> = reports().groupBy(::keyOf).map { (key, list) ->
        val history = list.sortedBy { it.optString("generatedAt") }
        val last = history.last()
        val prev = history.dropLast(1).lastOrNull()
        StudentSummary(
            key = key, name = last.optString("student"), lessons = last.optInt("completedLessons"),
            totalLessons = last.optInt("totalLessons", 300), xp = last.optInt("xp"),
            days7 = last.optInt("studyDaysLast7"), days30 = last.optInt("studyDaysLast30"),
            lastDate = last.optString("lastStudyDate"), mistakes = last.optInt("mistakesPending"), reports = history.size,
            xpDelta = prev?.let { last.optInt("xp") - it.optInt("xp") },
            lessonsDelta = prev?.let { last.optInt("completedLessons") - it.optInt("completedLessons") },
            updated = last.optString("generatedAt").take(16).replace('T', ' '),
            legacyId = key.startsWith("legacy:"),
        )
    }.sortedBy { it.name }

    fun deleteStudent(key: String) = save(reports().filterNot { keyOf(it) == key })

    private fun reports(): List<JSONObject> = runCatching {
        val arr = JSONArray(teacherPrefs.getString("reports", "[]"))
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }.getOrDefault(emptyList())

    private fun save(list: List<JSONObject>) = teacherPrefs.edit().putString("reports", JSONArray(list).toString()).apply()

    private fun keyOf(r: JSONObject): String {
        val id = r.optString("learnerId")
        return if (id.matches(UUID_RE)) "id:" + id.lowercase() else "legacy:" + r.optString("student").trim()
    }

    companion object {
        private val UUID_RE = Regex("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")
    }
}
