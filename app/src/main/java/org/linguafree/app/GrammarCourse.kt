package org.linguafree.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

/** Independent topic IDs preserve existing lesson and review indices. */
@Composable
fun GrammarCourse(
    context: Context,
    recommendedLevel: String,
    onSpeak: (String) -> Unit,
    onSessionFinished: () -> Unit,
    onBack: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("linguafree_progress", Context.MODE_PRIVATE) }
    val topics = remember {
        val data = JSONArray(context.assets.open("grammar.json").bufferedReader().use { it.readText() })
        (0 until data.length()).map { data.getJSONObject(it) }
    }
    var level by rememberSaveable { mutableStateOf(prefs.getString("grammarLevel", recommendedLevel) ?: recommendedLevel) }
    var group by rememberSaveable { mutableStateOf("all") }
    var topicId by rememberSaveable { mutableStateOf("") }
    var phase by rememberSaveable { mutableStateOf("intro") }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var correct by rememberSaveable { mutableIntStateOf(0) }
    var order by rememberSaveable { mutableStateOf(intArrayOf()) }
    var mistakes by rememberSaveable { mutableStateOf(intArrayOf()) }
    var retry by rememberSaveable { mutableStateOf(false) }
    var hint by rememberSaveable { mutableStateOf(false) }
    var mastered by remember { mutableStateOf(prefs.getStringSet("grammarMastered", emptySet())?.toSet() ?: emptySet()) }
    var best by remember { mutableStateOf(runCatching { JSONObject(prefs.getString("grammarBest", "{}") ?: "{}") }.getOrDefault(JSONObject())) }
    val topic = topics.firstOrNull { it.getString("id") == topicId }

    fun begin(indices: List<Int>, isRetry: Boolean) {
        order = indices.shuffled().toIntArray()
        index = 0; selected = -1; correct = 0; mistakes = intArrayOf()
        hint = false; retry = isRetry; phase = "practice"
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = {
            if (topic != null) { topicId = ""; phase = "intro" } else onBack()
        }) { Text(if (topic != null) "‹ К темам грамматики" else "‹ К урокам") }

        if (topic == null) {
            Text("🧩 Грамматика с лисичкой", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Местоимения, глаголы и времена: короткое правило → примеры → 5 заданий → работа над ошибками.")
            Text("Стартовая рекомендация: $recommendedLevel. Уровень можно выбрать самостоятельно; это не оценка CEFR.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("A1", "A2", "B1", "B2").forEach { item ->
                    FilterChip(selected = level == item, onClick = {
                        level = item
                        prefs.edit().putString("grammarLevel", item).apply()
                    }, label = { Text(item) }, modifier = Modifier.weight(1f))
                }
            }
            // A column keeps category labels readable on small screens and large font settings.
            listOf("all" to "Все темы", "pronouns" to "Местоимения", "verbs" to "Типы глаголов", "tenses" to "Времена").forEach { (id, title) ->
                FilterChip(selected = group == id, onClick = { group = id }, label = { Text(title) })
            }
            val available = topics.filter { it.getString("level") == level && (group == "all" || it.getString("group") == group) }
            val next = available.firstOrNull { it.getString("id") !in mastered }
            Text("Освоено на уровне $level: ${topics.count { it.getString("level") == level && it.getString("id") in mastered }} / ${topics.count { it.getString("level") == level }}")
            available.forEach { item ->
                val id = item.getString("id")
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text((if (id in mastered) "✓ " else "") + item.getString("title"), style = MaterialTheme.typography.titleMedium)
                        if (id == next?.getString("id")) Text("🦊 Следующая тема для тебя")
                        Text("${item.getJSONArray("questions").length()} заданий · лучший результат: ${best.optInt(id, 0)}/${item.getJSONArray("questions").length()}")
                        Button(onClick = { topicId = id; phase = "intro" }) { Text(if (id in mastered) "Повторить тему" else "Открыть тему") }
                    }
                }
            }
        } else {
            val questions = topic.getJSONArray("questions")
            Text("${topic.getString("level")} · ${topic.getString("title")}", style = MaterialTheme.typography.headlineSmall)
            if (phase == "intro") {
                Text(topic.getString("rule"), style = MaterialTheme.typography.bodyLarge)
                val examples = topic.getJSONArray("examples")
                for (i in 0 until examples.length()) {
                    Text(examples.getString(i), style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { onSpeak(examples.getString(i)) }) { Text("🔊 Послушать пример") }
                }
                Text("Для освоения темы нужно 4 из 5 верных ответов. Повторять и исправлять ошибки можно без ограничения.")
                Button(onClick = { begin((0 until questions.length()).toList(), false) }, modifier = Modifier.fillMaxWidth()) { Text("Начать практику 🚀") }
            } else if (phase == "practice") {
                val q = questions.getJSONObject(order[index])
                val choices = q.getJSONArray("choices")
                Text(if (retry) "Работа над ошибками" else "Практика")
                LinearProgressIndicator(progress = { index.toFloat() / order.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                Text("Задание ${index + 1} / ${order.size}", style = MaterialTheme.typography.labelLarge)
                Text(q.getString("prompt"), style = MaterialTheme.typography.titleLarge)
                for (i in 0 until choices.length()) {
                    OutlinedButton(onClick = { selected = i }, enabled = selected < 0,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text((if (selected >= 0 && i == q.getInt("answer")) "✓ " else "") + choices.getString(i))
                    }
                }
                if (selected < 0) {
                    TextButton(onClick = { hint = !hint }) { Text(if (hint) "Скрыть правило" else "💡 Вспомнить правило") }
                    if (hint) Text(topic.getString("rule"))
                } else {
                    Text(if (selected == q.getInt("answer")) "🦊 Верно!" else "🦊 Попробуем разобраться: ${choices.getString(q.getInt("answer"))}", fontWeight = FontWeight.Bold)
                    Text(q.getString("explanation"))
                    OutlinedButton(onClick = { onSpeak(q.getString("speech")) }) { Text("🔊 Послушать верный ответ") }
                    Button(onClick = {
                        val right = selected == q.getInt("answer")
                        if (right) correct++ else mistakes = mistakes + order[index]
                        selected = -1; hint = false
                        if (index < order.lastIndex) index++ else {
                            if (!retry) {
                                val id = topic.getString("id")
                                best = JSONObject(best.toString()).put(id, maxOf(best.optInt(id, 0), correct))
                                if (correct * 100 >= order.size * 80) mastered = mastered + id
                                prefs.edit().putString("grammarBest", best.toString()).putStringSet("grammarMastered", mastered).apply()
                                onSessionFinished()
                            }
                            phase = "result"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Продолжить") }
                }
            } else {
                Text("🎉 Практика завершена: $correct / ${order.size}", style = MaterialTheme.typography.titleLarge)
                Text(if (topic.getString("id") in mastered) "Тема освоена! Попробуй следующую или вернись позже." else "Ты продвинулся! Разбери ошибки и пройди полную практику ещё раз.")
                if (mistakes.isNotEmpty()) Button(onClick = { begin(mistakes.toList(), true) }, modifier = Modifier.fillMaxWidth()) { Text("Разобрать ошибки (${mistakes.size})") }
                OutlinedButton(onClick = { begin((0 until questions.length()).toList(), false) }, modifier = Modifier.fillMaxWidth()) { Text("Ещё одна полная попытка") }
                TextButton(onClick = { topicId = ""; phase = "intro" }) { Text("Выбрать следующую тему") }
            }
        }
    }
}
