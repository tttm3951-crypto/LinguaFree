package org.linguafree.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.linguafree.core.data.content.ContentLoader
import org.linguafree.core.data.content.ContentSchema
import org.linguafree.core.data.content.GuideSectionDto
import org.linguafree.core.data.content.LineDto
import org.linguafree.core.data.content.OptionDto
import org.linguafree.core.data.content.PairDto
import org.linguafree.core.data.content.QuestionDto
import org.linguafree.core.data.db.ContentDao
import org.linguafree.core.data.db.ExerciseEntity
import org.linguafree.core.domain.model.ComprehensionQuestion
import org.linguafree.core.domain.model.Course
import org.linguafree.core.domain.model.CourseUnit
import org.linguafree.core.domain.model.DialogueLine
import org.linguafree.core.domain.model.Direction
import org.linguafree.core.domain.model.Exercise
import org.linguafree.core.domain.model.ExerciseOption
import org.linguafree.core.domain.model.ExerciseType
import org.linguafree.core.domain.model.GuideExample
import org.linguafree.core.domain.model.GuideSection
import org.linguafree.core.domain.model.Guidebook
import org.linguafree.core.domain.model.Lexeme
import org.linguafree.core.domain.model.NodeType
import org.linguafree.core.domain.model.PathNode
import org.linguafree.core.domain.model.Section
import org.linguafree.core.domain.model.WordPair
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.ContentStatus

class ContentRepositoryImpl(
    private val loader: ContentLoader,
    private val dao: ContentDao,
) : ContentRepository {

    private val mutex = Mutex()
    private var courseCache: Course? = null
    private var exercisesCache: Map<String, List<Exercise>>? = null
    private var lexemesCache: List<Lexeme>? = null
    private var hintsCache: Map<String, String>? = null
    private var status: ContentStatus? = null
    private val json = ContentSchema.json

    override suspend fun ensureContent(): ContentStatus = mutex.withLock {
        status?.let { if (it is ContentStatus.Ready) return it }
        withContext(Dispatchers.IO) { loader.ensure() }.also {
            status = it
            if (it is ContentStatus.Ready && it.imported) { courseCache = null; exercisesCache = null; lexemesCache = null }
        }
    }

    override suspend fun course(): Course {
        courseCache?.let { return it }
        ensureContent()
        return withContext(Dispatchers.IO) {
            val c = dao.course() ?: error("Контент не загружен")
            val lessons = dao.lessons().groupBy { it.unitId }
            val units = dao.units().groupBy { it.sectionId }
            val sections = dao.sections().map { s ->
                Section(
                    id = s.id, order = s.ord, title = s.title, cefr = s.cefr, description = s.description,
                    units = units[s.id].orEmpty().sortedBy { it.ord }.map { u ->
                        CourseUnit(
                            id = u.id, sectionId = s.id, order = u.ord, title = u.title, description = u.description,
                            color = u.color, guidebook = guidebook(u.guidebookJson),
                            nodes = lessons[u.id].orEmpty().sortedBy { it.ord }.map { n ->
                                PathNode(n.id, u.id, n.ord, NodeType.of(n.type), n.title, n.newWordsCsv.split(',').filter { it.isNotBlank() })
                            },
                        )
                    },
                )
            }
            Course(c.id, c.title, c.contentVersion, sections)
        }.also { courseCache = it }
    }

    private fun guidebook(raw: String): Guidebook = runCatching {
        Guidebook(json.decodeFromString(ListSerializer(GuideSectionDto.serializer()), raw).map { g ->
            GuideSection(g.title, g.text, g.examples.map { GuideExample(it.en, it.ru) })
        })
    }.getOrDefault(Guidebook(emptyList()))

    override suspend fun exercises(nodeId: String): List<Exercise> =
        exercisesCache?.get(nodeId) ?: withContext(Dispatchers.IO) { dao.exercises(nodeId).map { it.toDomain() } }

    override suspend fun exercisesByNode(): Map<String, List<Exercise>> {
        exercisesCache?.let { return it }
        ensureContent()
        return withContext(Dispatchers.IO) {
            dao.allExercises().map { it.toDomain() }.groupBy { it.nodeId }.mapValues { (_, v) -> v.sortedBy { it.order } }
        }.also { exercisesCache = it }
    }

    override suspend fun lexemes(): List<Lexeme> {
        lexemesCache?.let { return it }
        ensureContent()
        return withContext(Dispatchers.IO) { dao.lexemes().map { Lexeme(it.id, it.word, it.translation, it.image) } }
            .also { lexemesCache = it }
    }

    override suspend fun hints(): Map<String, String> {
        hintsCache?.let { return it }
        return withContext(Dispatchers.IO) {
            val base = loader.readLexicon().toMutableMap()
            lexemes().forEach { l ->
                base.putIfAbsent(l.word.lowercase(), l.translation)
                base.putIfAbsent(l.translation.substringBefore(',').trim().lowercase(), l.word)
            }
            base
        }.also { hintsCache = it }
    }

    private fun ExerciseEntity.toDomain(): Exercise {
        val options = runCatching { json.decodeFromString(ListSerializer(OptionDto.serializer()), optionsJson) }.getOrDefault(emptyList())
        val answers = runCatching { json.decodeFromString(ListSerializer(String.serializer()), correctAnswersJson) }.getOrDefault(emptyList())
        var pairs = emptyList<WordPair>()
        var lines = emptyList<DialogueLine>()
        var questions = emptyList<ComprehensionQuestion>()
        extraJson?.let { raw ->
            val obj = json.parseToJsonElement(raw).jsonObject
            obj["pairs"]?.let { p -> pairs = json.decodeFromJsonElement(ListSerializer(PairDto.serializer()), p.jsonArray).map { WordPair(it.en, it.ru) } }
            obj["lines"]?.let { p -> lines = json.decodeFromJsonElement(ListSerializer(LineDto.serializer()), p.jsonArray).map { DialogueLine(it.speaker, it.en, it.ru) } }
            obj["questions"]?.let { p -> questions = json.decodeFromJsonElement(ListSerializer(QuestionDto.serializer()), p.jsonArray).map { ComprehensionQuestion(it.prompt, it.options, it.answer) } }
        }
        return Exercise(
            id = id, nodeId = lessonId, order = ord, type = ExerciseType.of(type), text = prompt,
            instruction = instruction, subtitle = subtitle,
            options = options.map { ExerciseOption(it.text, it.image) }, answers = answers,
            pairs = pairs, lines = lines, questions = questions,
            direction = if (direction == "en_ru") Direction.EnToRu else Direction.RuToEn,
            audio = audioRef, image = imageRef,
            lexemes = lexemesCsv.split(',').filter { it.isNotBlank() },
            hints = hintsJson?.let { runCatching { json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), it) }.getOrNull() } ?: emptyMap(),
        )
    }
}
