package org.linguafree.core.data.content

import android.content.Context
import android.util.Log
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.linguafree.core.data.db.ContentDao
import org.linguafree.core.data.db.CourseEntity
import org.linguafree.core.data.db.ExerciseEntity
import org.linguafree.core.data.db.LessonEntity
import org.linguafree.core.data.db.LexemeEntity
import org.linguafree.core.data.db.SectionEntity
import org.linguafree.core.data.db.UnitEntity
import org.linguafree.core.domain.repository.ContentStatus

/** Reads bundled JSON content, validates it and imports it into Room. */
class ContentLoader(private val context: Context, private val dao: ContentDao) {

    private val json = ContentSchema.json

    private fun read(path: String): String = context.assets.open("content/$path").bufferedReader().use { it.readText() }

    fun readCourse(): CourseDto = json.decodeFromString(CourseDto.serializer(), read("course.json"))

    fun readLexicon(): Map<String, String> = runCatching {
        json.decodeFromString(LexiconDto.serializer(), read("lexicon.json")).hints
    }.getOrDefault(emptyMap())

    fun imageExists(name: String): Boolean = imageRes(context, name) != 0

    suspend fun ensure(): ContentStatus {
        val course = runCatching { readCourse() }.getOrElse { return invalidOrOld("course.json: ${it.message}") }
        val stored = dao.course()
        if (stored != null && stored.contentVersion == course.contentVersion && stored.schemaVersion == course.schemaVersion) {
            return ContentStatus.Ready(course.contentVersion, imported = false)
        }
        val units = HashMap<String, UnitDto>()
        val lexemes: LexemesDto
        try {
            course.sections.flatMap { it.units }.forEach { id ->
                units[id] = json.decodeFromString(UnitDto.serializer(), read("units/$id.json"))
            }
            lexemes = json.decodeFromString(LexemesDto.serializer(), read("lexemes.json"))
        } catch (e: Exception) {
            return invalidOrOld("Ошибка чтения контента: ${e.message}")
        }
        val errors = ContentValidator.validate(course, units, lexemes, ::imageExists)
        if (errors.isNotEmpty()) {
            errors.take(20).forEach { Log.e(TAG, it) }
            return invalidOrOld(*errors.toTypedArray())
        }
        import(course, units, lexemes)
        return ContentStatus.Ready(course.contentVersion, imported = true)
    }

    private suspend fun invalidOrOld(vararg errors: String): ContentStatus {
        val old = dao.course()
        return if (old != null) ContentStatus.Ready(old.contentVersion, imported = false) else ContentStatus.Invalid(errors.toList())
    }

    private suspend fun import(course: CourseDto, units: Map<String, UnitDto>, lexemes: LexemesDto) {
        val sections = mutableListOf<SectionEntity>()
        val unitEntities = mutableListOf<UnitEntity>()
        val lessons = mutableListOf<LessonEntity>()
        val exercises = mutableListOf<ExerciseEntity>()
        var unitOrd = 0
        var nodeOrd = 0
        course.sections.forEachIndexed { si, s ->
            sections += SectionEntity(s.id, si, s.title, s.cefr, s.description)
            for (uid in s.units) {
                val u = units.getValue(uid)
                unitEntities += UnitEntity(
                    id = u.id, sectionId = s.id, ord = unitOrd++, title = u.title, description = u.description,
                    color = parseColor(u.color),
                    guidebookJson = json.encodeToString(ListSerializer(GuideSectionDto.serializer()), u.guidebook),
                )
                for (n in u.nodes) {
                    lessons += LessonEntity(n.id, u.id, nodeOrd++, n.type, n.title, n.newWords.joinToString(","))
                    n.exercises.forEachIndexed { i, e -> exercises += e.toEntity(n.id, i) }
                }
            }
        }
        dao.replaceContent(
            course = CourseEntity(course.id, course.title, course.schemaVersion, course.contentVersion),
            sections = sections, units = unitEntities, lessons = lessons, exercises = exercises,
            lexemes = lexemes.lexemes.map { LexemeEntity(it.id, it.word, it.translation, it.image) },
        )
    }

    private fun ExerciseDto.toEntity(lessonId: String, index: Int): ExerciseEntity {
        val extra: JsonObject? = if (pairs.isEmpty() && lines.isEmpty() && questions.isEmpty()) null else buildJsonObject {
            put("pairs", json.encodeToJsonElement(ListSerializer(PairDto.serializer()), pairs))
            put("lines", json.encodeToJsonElement(ListSerializer(LineDto.serializer()), lines))
            put("questions", json.encodeToJsonElement(ListSerializer(QuestionDto.serializer()), questions))
        }
        return ExerciseEntity(
            id = id, lessonId = lessonId, ord = if (order != 0) order else index, type = type, prompt = text,
            instruction = instruction, subtitle = subtitle, direction = direction,
            optionsJson = json.encodeToString(ListSerializer(OptionDto.serializer()), options.map { it.toOption() }),
            correctAnswersJson = json.encodeToString(ListSerializer(String.serializer()), answers),
            audioRef = audio, imageRef = image, extraJson = extra?.toString(),
            lexemesCsv = lexemes.joinToString(","),
            hintsJson = if (hints.isEmpty()) null else json.encodeToString(MapSerializer(String.serializer(), String.serializer()), hints),
        )
    }

    companion object {
        private const val TAG = "LinguaFreeContent"

        fun parseColor(hex: String): Long = runCatching { 0xFF000000L or hex.removePrefix("#").toLong(16) }.getOrDefault(0xFF4CC417L)

        @Suppress("DiscouragedApi")
        fun imageRes(context: Context, name: String): Int = context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}
