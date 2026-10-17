package org.linguafree.core.data.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** JSON model of the content files (schema v1). See docs/content-format.md. */
object ContentSchema {
    const val SUPPORTED_VERSION = 1
    val json = Json { ignoreUnknownKeys = true }
}

@Serializable
data class CourseDto(
    val schemaVersion: Int,
    val contentVersion: Int,
    val id: String,
    val title: String,
    val sections: List<SectionDto>,
)

@Serializable
data class SectionDto(
    val id: String,
    val title: String,
    val cefr: String = "",
    val description: String = "",
    val units: List<String>,
)

@Serializable
data class UnitDto(
    val schemaVersion: Int,
    val id: String,
    val title: String,
    val description: String = "",
    val color: String = "#4CC417",
    val guidebook: List<GuideSectionDto> = emptyList(),
    val nodes: List<NodeDto>,
)

@Serializable
data class GuideSectionDto(val title: String, val text: String, val examples: List<GuideExampleDto> = emptyList())

@Serializable
data class GuideExampleDto(val en: String, val ru: String = "")

@Serializable
data class NodeDto(
    val id: String,
    val type: String,
    val title: String = "",
    val newWords: List<String> = emptyList(),
    val exercises: List<ExerciseDto> = emptyList(),
)

@Serializable
data class ExerciseDto(
    val id: String,
    val type: String,
    val order: Int = 0,
    val text: String = "",
    val instruction: String? = null,
    val subtitle: String? = null,
    val direction: String? = null,
    /** Strings or {"text", "image"} objects. */
    val options: List<JsonElement> = emptyList(),
    val answers: List<String> = emptyList(),
    val pairs: List<PairDto> = emptyList(),
    val lines: List<LineDto> = emptyList(),
    val questions: List<QuestionDto> = emptyList(),
    val audio: String? = null,
    val image: String? = null,
    val lexemes: List<String> = emptyList(),
    val hints: Map<String, String> = emptyMap(),
)

@Serializable
data class PairDto(val en: String, val ru: String)

@Serializable
data class LineDto(val speaker: String = "", val en: String, val ru: String = "")

@Serializable
data class QuestionDto(val prompt: String, val options: List<String>, val answer: Int)

@Serializable
data class LexemesDto(val schemaVersion: Int, val lexemes: List<LexemeDto>)

@Serializable
data class LexemeDto(val id: String, val word: String, val translation: String, val image: String? = null)

@Serializable
data class LexiconDto(val schemaVersion: Int, val hints: Map<String, String>)

/** Option as stored in Room (normalised form of the string-or-object JSON options). */
@Serializable
data class OptionDto(val text: String, val image: String? = null)

fun JsonElement.toOption(): OptionDto = when (this) {
    is JsonPrimitive -> OptionDto(content)
    is JsonObject -> OptionDto(
        text = this["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        image = this["image"]?.jsonPrimitive?.contentOrNull,
    )
    else -> OptionDto(toString())
}
