package org.linguafree.core.data.content

import org.linguafree.core.domain.learning.AnswerChecker

/**
 * Checks content before it is imported. Same rules as scripts/content/validate_content.py,
 * so a broken file never reaches the learner (the previous content stays in use).
 */
object ContentValidator {
    private val types = setOf("tiles", "type", "select_image", "match", "listen_tiles", "listen_type", "fill_gap", "speak", "select_translation", "dialogue")
    private val nodeTypes = setOf("lesson", "chest", "story", "review", "test")

    fun validate(course: CourseDto, units: Map<String, UnitDto>, lexemes: LexemesDto, imageExists: (String) -> Boolean): List<String> {
        val errors = mutableListOf<String>()
        fun err(where: String, msg: String) { if (errors.size < 200) errors += "$where: $msg" }

        if (course.schemaVersion > ContentSchema.SUPPORTED_VERSION) err("course.json", "схема ${course.schemaVersion} новее поддерживаемой ${ContentSchema.SUPPORTED_VERSION}")
        if (lexemes.schemaVersion > ContentSchema.SUPPORTED_VERSION) err("lexemes.json", "неподдерживаемая схема")
        val lexIds = lexemes.lexemes.map { it.id }.toSet()
        if (lexIds.size != lexemes.lexemes.size) err("lexemes.json", "повторяющиеся id")
        val ids = HashSet<String>()
        for (section in course.sections) for (unitId in section.units) {
            val unit = units[unitId] ?: run { err(unitId, "нет файла юнита"); null } ?: continue
            if (unit.schemaVersion > ContentSchema.SUPPORTED_VERSION) err(unitId, "неподдерживаемая схема")
            if (!Regex("#[0-9A-Fa-f]{6}").matches(unit.color)) err(unitId, "цвет должен быть #RRGGBB")
            if (unit.nodes.count { it.type == "test" } != 1) err(unitId, "нужна ровно одна проверка юнита")
            for (node in unit.nodes) {
                val w = "$unitId/${node.id}"
                if (node.type !in nodeTypes) err(w, "неизвестный тип узла ${node.type}")
                if (!ids.add(node.id)) err(w, "повторяющийся id")
                if (node.type == "lesson") {
                    if (node.exercises.size < 12) err(w, "в уроке меньше 12 упражнений")
                    if (node.newWords.size > 5) err(w, "больше 5 новых слов")
                }
                if (node.type == "story" && node.exercises.none { it.type == "dialogue" }) err(w, "в истории нет диалога")
                for (e in node.exercises) {
                    val ew = "$w/${e.id}"
                    if (!ids.add(e.id)) err(ew, "повторяющийся id")
                    validateExercise(e, imageExists) { err(ew, it) }
                    e.lexemes.filter { it !in lexIds }.forEach { err(ew, "неизвестная лексема $it") }
                }
            }
        }
        return errors
    }

    private fun validateExercise(e: ExerciseDto, imageExists: (String) -> Boolean, err: (String) -> Unit) {
        if (e.type !in types) return err("неизвестный тип ${e.type}")
        val options = e.options.map { it.toOption() }
        val needsAnswer = e.type !in setOf("match", "dialogue")
        if (needsAnswer && e.answers.isEmpty()) err("нет ответа")
        if (e.type in setOf("select_image", "fill_gap", "select_translation")) {
            if (options.size < 2) err("нужно ≥ 2 варианта")
            val n = options.map { AnswerChecker.normalize(it.text) }
            if (e.answers.none { a -> AnswerChecker.normalize(a) in n }) err("ответа нет среди вариантов")
        }
        if (e.type == "select_image") {
            if (options.size != 4) err("нужно 4 карточки")
            options.forEach { o -> if (o.image == null || !imageExists(o.image)) err("нет картинки ${o.image}") }
        }
        if (e.type == "tiles" || e.type == "listen_tiles") {
            val bank = options.flatMap { AnswerChecker.words(it.text) }.groupingBy { it }.eachCount()
            val need = AnswerChecker.words(e.answers.firstOrNull().orEmpty()).groupingBy { it }.eachCount()
            if (need.any { (w, c) -> (bank[w] ?: 0) < c }) err("из плиток нельзя собрать ответ")
        }
        if (e.type in setOf("listen_tiles", "listen_type", "speak") && e.audio.isNullOrBlank()) err("нет аудио")
        if (e.type == "fill_gap" && "___" !in e.text) err("нет пропуска ___")
        if (e.type == "match" && e.pairs.size != 5) err("нужно 5 пар")
        if (e.type == "dialogue") {
            if (e.lines.size < 2) err("в диалоге меньше 2 реплик")
            e.questions.forEach { q -> if (q.answer !in q.options.indices) err("неверный индекс ответа") }
        }
    }
}
