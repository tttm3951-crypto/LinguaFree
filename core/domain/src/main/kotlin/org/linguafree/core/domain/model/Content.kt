package org.linguafree.core.domain.model

/** Course → sections (A1, A2…) → units → path nodes (lessons, chests, stories, reviews, tests). */
data class Course(
    val id: String,
    val title: String,
    val contentVersion: Int,
    val sections: List<Section>,
)

data class Section(
    val id: String,
    val order: Int,
    val title: String,
    val cefr: String,
    val description: String,
    val units: List<CourseUnit>,
)

data class CourseUnit(
    val id: String,
    val sectionId: String,
    val order: Int,
    val title: String,
    val description: String,
    /** ARGB color of the unit banner. */
    val color: Long,
    val guidebook: Guidebook,
    val nodes: List<PathNode>,
)

data class Guidebook(val sections: List<GuideSection>) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

data class GuideSection(val title: String, val text: String, val examples: List<GuideExample>)
data class GuideExample(val en: String, val ru: String)

enum class NodeType(val key: String) {
    Lesson("lesson"), Chest("chest"), Story("story"), Review("review"), Test("test");

    companion object {
        fun of(key: String): NodeType = entries.firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Unknown node type: $key")
    }
}

/** A circle on the learning path. */
data class PathNode(
    val id: String,
    val unitId: String,
    val order: Int,
    val type: NodeType,
    val title: String,
    /** New lexeme ids introduced in this lesson (≤ 5). */
    val newWords: List<String> = emptyList(),
)

/** One lexeme (word or fixed phrase) tracked by spaced repetition. */
data class Lexeme(
    val id: String,
    val word: String,
    val translation: String,
    val image: String? = null,
)

enum class ExerciseType(val key: String, val title: String) {
    /** 1. Translate a sentence by tapping/dragging word tiles (RU→EN or EN→RU). */
    Tiles("tiles", "Переведите предложение"),
    /** 2. Translate by typing; typos tolerated (Levenshtein), contractions normalised. */
    TypeTranslation("type", "Напишите перевод"),
    /** 3. Pick the picture for a word (4 cards). */
    SelectImage("select_image", "Выберите правильную картинку"),
    /** 4. Match five pairs. */
    MatchPairs("match", "Найдите пары"),
    /** 5a. Listen and assemble with tiles. */
    ListenTiles("listen_tiles", "Соберите то, что услышали"),
    /** 5b. Listen and type. */
    ListenType("listen_type", "Напишите то, что услышали"),
    /** 6. Fill the gap by choosing an option. */
    FillGap("fill_gap", "Заполните пропуск"),
    /** 7. Read the phrase aloud. */
    Speak("speak", "Произнесите фразу"),
    /** 8. Multiple choice translation. */
    SelectTranslation("select_translation", "Выберите перевод"),
    /** 9. Mini dialogue/story with comprehension questions. */
    Dialogue("dialogue", "Прочитайте диалог и ответьте");

    val isListening: Boolean get() = this == ListenTiles || this == ListenType
    val isSpeaking: Boolean get() = this == Speak

    companion object {
        fun of(key: String): ExerciseType = entries.firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Unknown exercise type: $key")
    }
}

enum class Direction { RuToEn, EnToRu }

data class ExerciseOption(val text: String, val image: String? = null)
data class WordPair(val en: String, val ru: String)
data class DialogueLine(val speaker: String, val en: String, val ru: String)
data class ComprehensionQuestion(val prompt: String, val options: List<String>, val answer: Int)

data class Exercise(
    val id: String,
    val nodeId: String,
    val order: Int,
    val type: ExerciseType,
    /** Main text: sentence to translate, word for pictures, phrase to say, gap sentence (`___`). */
    val text: String,
    /** Optional override for the default instruction of the type. */
    val instruction: String? = null,
    /** Optional line under the text, e.g. the translation of a fill-the-gap sentence. */
    val subtitle: String? = null,
    val options: List<ExerciseOption> = emptyList(),
    /** All accepted answers. For choice types — the text of the right option. */
    val answers: List<String> = emptyList(),
    val pairs: List<WordPair> = emptyList(),
    val lines: List<DialogueLine> = emptyList(),
    val questions: List<ComprehensionQuestion> = emptyList(),
    val direction: Direction = Direction.RuToEn,
    /** Text for TTS or `asset:path` for a recorded file. */
    val audio: String? = null,
    val image: String? = null,
    val lexemes: List<String> = emptyList(),
    /** Word → translation hints shown on tap. */
    val hints: Map<String, String> = emptyMap(),
) {
    val displayInstruction: String get() = instruction ?: type.title
    /** Language of what the learner produces. */
    val answerIsEnglish: Boolean
        get() = when (type) {
            ExerciseType.Tiles, ExerciseType.TypeTranslation -> direction == Direction.RuToEn
            ExerciseType.ListenTiles, ExerciseType.ListenType, ExerciseType.Speak, ExerciseType.FillGap -> true
            else -> direction == Direction.RuToEn
        }
}

/** A story shown as a path node. */
data class Story(
    val id: String,
    val title: String,
    val level: String,
    val lines: List<DialogueLine>,
    val questions: List<ComprehensionQuestion>,
)
