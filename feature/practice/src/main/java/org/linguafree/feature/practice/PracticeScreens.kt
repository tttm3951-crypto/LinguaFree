package org.linguafree.feature.practice

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.linguafree.core.data.library.Topic
import org.linguafree.core.data.library.TopicBook
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.ChoiceCard
import org.linguafree.core.ui.components.ChoiceState
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme

/** Practice tab: everything is free and never costs hearts. */
@Composable
fun PracticeScreen(
    onStart: (SessionKind) -> Unit,
    onDictionary: () -> Unit,
    onTopics: (TopicBook) -> Unit,
    onWords: () -> Unit,
    viewModel: PracticeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LfTheme.colors
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp).testTag("practice"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FoxMascot(FoxEmotion.Thinking, Modifier.width(96.dp))
            Column(Modifier.weight(1f)) {
                Text("Практика", style = MaterialTheme.typography.headlineMedium, color = c.text)
                Text("Тренировки бесплатны и не тратят сердца.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
        }
        PracticeCard(
            R.drawable.ic_target, "Работа над ошибками",
            if (state.mistakes > 0) "Заданий к повторению: ${state.mistakes}" else "Ошибок нет — отлично! Они появятся здесь после уроков.",
            enabled = state.mistakes > 0, tag = "practice-mistakes",
        ) { onStart(SessionKind.PracticeMistakes) }
        PracticeCard(
            R.drawable.ic_dumbbell, "Слабые слова",
            if (state.words.isEmpty()) "Пройди первый урок — и здесь появятся слова для повторения."
            else "Слов, которые пора повторить: ${state.weakCount}. Подбор по интервальному повторению.",
            enabled = state.words.isNotEmpty(), tag = "practice-weak",
        ) { onStart(SessionKind.PracticeWeakWords) }
        PracticeCard(
            R.drawable.ic_speaker, "Аудирование",
            if (state.listeningOn) "Послушай и собери фразу. Есть медленная скорость." else "Задания на слух отключены на час.",
            enabled = state.listeningOn && state.words.isNotEmpty(), tag = "practice-listening",
        ) { onStart(SessionKind.PracticeListening) }
        PracticeCard(
            R.drawable.ic_mic, "Говорение",
            if (state.speakingOn) "Прочитай фразу вслух — приложение оценит произношение офлайн." else "Задания на говорение отключены на час.",
            enabled = state.speakingOn && state.words.isNotEmpty(), tag = "practice-speaking",
        ) { onStart(SessionKind.PracticeSpeaking) }
        if (!state.speakingOn || !state.listeningOn) {
            LfButton("Вернуть говорение и аудирование", onClick = viewModel::resumeSpeaking, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        }
        PracticeCard(
            R.drawable.ic_star, "Пары на время",
            "Сопоставь слова и переводы за минуту. Три раунда.",
            enabled = state.words.isNotEmpty(), tag = "practice-match",
        ) { onStart(SessionKind.PracticeMatch) }

        Text("Библиотека", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.padding(top = 8.dp))
        PracticeCard(R.drawable.ic_book, "Мои слова", "Все изученные слова и сила знания каждого.", enabled = true, tag = "practice-words", onClick = onWords)
        PracticeCard(R.drawable.ic_flag_en, "Словарь", if (viewModel.referenceAvailable) "Учебный словарь, избранное и офлайн-справочник на 150 000 фраз." else "Учебный словарь и избранное.", enabled = true, tag = "practice-dictionary", onClick = onDictionary)
        PracticeCard(R.drawable.ic_story, "Английский с нуля", "Буквы, звуки и первые фразы — короткие темы с проверкой.", enabled = true, tag = "practice-beginner") { onTopics(TopicBook.Beginner) }
        PracticeCard(R.drawable.ic_crown, "Грамматика", "Правила A1–B2 на русском с примерами и тестом.", enabled = true, tag = "practice-grammar") { onTopics(TopicBook.Grammar) }
    }
}

@Composable
private fun PracticeCard(@DrawableRes icon: Int, title: String, subtitle: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val c = LfTheme.colors
    ChoiceCard(onClick = { if (enabled) onClick() }, state = if (enabled) ChoiceState.Idle else ChoiceState.Disabled, modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LfIcon(icon, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (enabled) c.text else c.textMuted)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
    }
}

@Composable
internal fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val c = LfTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onBack).padding(10.dp)) {
                LfIcon(R.drawable.ic_back_mono, size = 24.dp, tint = c.textMuted, contentDescription = "Назад")
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, maxLines = 1)
        }
        content()
    }
}

@Composable
internal fun SearchField(value: String, label: String, tag: String, onChange: (String) -> Unit) {
    val c = LfTheme.colors
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.gems, unfocusedBorderColor = c.border, focusedTextColor = c.text, unfocusedTextColor = c.text),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag(tag),
    )
}

@Composable
internal fun SpeakChip(onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(6.dp)) {
        LfIcon(R.drawable.ic_speaker, size = 28.dp, contentDescription = "Послушать")
    }
}

/** Words the learner has met, weakest first, with recall strength. */
@Composable
fun WordsScreen(onBack: () -> Unit, viewModel: PracticeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LfTheme.colors
    var query by rememberSaveable { mutableStateOf("") }
    val shown = state.words.filter { query.isBlank() || it.word.contains(query, true) || it.translation.contains(query, true) }
    SubScreen("Мои слова · ${state.words.size}", onBack) {
        SearchField(query, "Поиск слова", "words-search") { query = it.take(60) }
        if (state.words.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FoxMascot(FoxEmotion.Waiting, Modifier.width(160.dp))
                Text("Здесь появятся слова из пройденных уроков.", color = c.textMuted, textAlign = TextAlign.Center)
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.id }) { w ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, c.border, RoundedCornerShape(14.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpeakChip { viewModel.speak(w.word) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(w.word, style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text(w.translation, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        val color = when { w.strength >= 0.75 -> c.primary; w.strength >= 0.4 -> c.xp; else -> c.error }
                        LfProgressBar(w.strength.toFloat(), color = color, height = 8.dp, label = "Сила знания")
                    }
                    Text("${(w.strength * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                }
            }
        }
    }
}

/** Dictionary: the learner dictionary, favourites and the bundled offline reference. */
@Composable
fun DictionaryScreen(onBack: () -> Unit, viewModel: PracticeViewModel = hiltViewModel()) {
    val c = LfTheme.colors
    val dictionary by viewModel.dictionary.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val reference by viewModel.reference.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val tabs = buildList { add("Словарь"); add("Избранное ★"); if (viewModel.referenceAvailable) add("150 000") }
    SubScreen("Словарь", onBack) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)).border(2.dp, c.border, RoundedCornerShape(14.dp))) {
            tabs.forEachIndexed { i, t ->
                Box(
                    Modifier.weight(1f).background(if (i == tab) c.gems.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable(role = Role.Tab) { tab = i }.padding(vertical = 12.dp).testTag("dict-tab-$i"),
                    contentAlignment = Alignment.Center,
                ) { Text(t, style = MaterialTheme.typography.labelMedium, color = if (i == tab) c.gems else c.textMuted) }
            }
        }
        Box(Modifier.padding(top = 10.dp))
        if (tab == 2) {
            SearchField(reference.query, "Слово или фраза: hello / привет", "reference-search") { viewModel.search(it) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(
                        "635 учебных записей и пары предложений Tatoeba — поиск на двух языках без интернета. Вводи начало слова. " +
                            "Корпус сообщества может содержать ошибки; это справочник, а не курс.",
                        style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                    )
                }
                if (reference.busy) item { Text("Ищу…", color = c.textMuted) }
                reference.error?.let { e -> item { Text(e, color = c.error) } }
                if (!reference.busy && reference.query.isNotBlank() && reference.rows.isEmpty() && reference.error == null) item { Text("Совпадений нет. Попробуй начало слова.", color = c.textMuted) }
                items(reference.rows.take(50), key = { it.id }) { r ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, c.border, RoundedCornerShape(14.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.en, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                            SpeakChip { viewModel.speak(r.en) }
                        }
                        Text(r.ru, style = MaterialTheme.typography.bodyMedium, color = c.text)
                        Text(r.credit, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    }
                }
                if (reference.query.isNotBlank()) item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        LfButton("Назад", onClick = { viewModel.search(reference.query, reference.page - 1) }, enabled = reference.page > 0 && !reference.busy, style = LfButtonStyle.Secondary, modifier = Modifier.weight(1f))
                        Text("${reference.page + 1}", color = c.text)
                        LfButton("Дальше", onClick = { viewModel.search(reference.query, reference.page + 1) }, enabled = reference.hasNext && !reference.busy, style = LfButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    }
                }
                item { Text("Tatoeba / ManyThings · CC BY 2.0 FR. Авторы и номера сохранены у каждой пары.", style = MaterialTheme.typography.labelSmall, color = c.textMuted) }
            }
        } else {
            SearchField(query, "Поиск: английский или русский", "dict-search") { query = it.take(60) }
            val base = if (tab == 1) dictionary.filter { it.en in favorites } else dictionary
            val shown = base.filter { query.isBlank() || it.en.contains(query, true) || it.ru.contains(query, true) }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tab == 1 && base.isEmpty()) item { Text("Отмечай слова звёздочкой ☆ — они соберутся здесь.", color = c.textMuted) }
                items(shown, key = { it.en + "|" + it.ru }) { e ->
                    val open = expanded == e.en + e.ru
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, c.border, RoundedCornerShape(14.dp))
                            .clickable { expanded = if (open) null else e.en + e.ru }.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpeakChip { viewModel.speak(e.en) }
                            Column(Modifier.weight(1f)) {
                                Text(e.en, style = MaterialTheme.typography.titleMedium, color = c.text)
                                Text(e.ru, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                            }
                            if (e.level.isNotBlank()) Text(e.level, style = MaterialTheme.typography.labelSmall, color = c.gems)
                            Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Checkbox) { viewModel.toggleFavorite(e.en) }.padding(6.dp)) {
                                Text(if (e.en in favorites) "★" else "☆", style = MaterialTheme.typography.titleLarge, color = c.xp)
                            }
                        }
                        if (open && e.exampleEn.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(e.exampleEn, style = MaterialTheme.typography.bodyMedium, color = c.text)
                                    Text(e.exampleRu, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                                }
                                SpeakChip { viewModel.speak(e.exampleEn) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val groupTitles = mapOf(
    "reading" to "Чтение", "words" to "Слова", "sentences" to "Предложения", "pronouns" to "Местоимения",
    "verbs" to "Глаголы", "tenses" to "Времена", "time" to "Время",
)

@Composable
fun TopicsScreen(book: TopicBook, onOpen: (String) -> Unit, onBack: () -> Unit, viewModel: PracticeViewModel = hiltViewModel()) {
    val c = LfTheme.colors
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val mastered by viewModel.mastered.collectAsStateWithLifecycle()
    val list = topics.filter { it.book == book }
    SubScreen(if (book == TopicBook.Beginner) "Английский с нуля" else "Грамматика", onBack) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val done = list.count { it.id in mastered }
                Text("Пройдено тем: $done из ${list.size}", style = MaterialTheme.typography.titleMedium, color = c.text)
                LfProgressBar(if (list.isEmpty()) 0f else done.toFloat() / list.size, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp), label = "Темы")
            }
            items(list, key = { it.id }) { t ->
                ChoiceCard(onClick = { onOpen(t.id) }, state = ChoiceState.Idle, modifier = Modifier.fillMaxWidth().testTag("topic-${t.id}")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (t.id in mastered) c.primary else c.border), contentAlignment = Alignment.Center) {
                            Text(if (t.id in mastered) "✓" else t.level.take(2), color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text("${t.level} · ${groupTitles[t.group] ?: t.group} · вопросов: ${t.questions.size}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopicScreen(id: String, onBack: () -> Unit, viewModel: PracticeViewModel = hiltViewModel()) {
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val topic = topics.firstOrNull { it.id == id }
    SubScreen(topic?.title ?: "", onBack) {
        if (topic != null) TopicBody(topic, viewModel, onBack)
    }
}

@Composable
private fun TopicBody(topic: Topic, viewModel: PracticeViewModel, onBack: () -> Unit) {
    val c = LfTheme.colors
    var quiz by rememberSaveable(topic.id) { mutableStateOf(false) }
    var index by rememberSaveable(topic.id) { mutableIntStateOf(0) }
    var correct by rememberSaveable(topic.id) { mutableIntStateOf(0) }
    var picked by rememberSaveable(topic.id, index) { mutableStateOf<Int?>(null) }
    var finished by rememberSaveable(topic.id) { mutableStateOf<Boolean?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            finished != null -> {
                val passed = finished == true
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FoxMascot(if (passed) FoxEmotion.Complete else FoxEmotion.TryAgain, Modifier.width(180.dp))
                    Text(if (passed) "Тема пройдена!" else "Почти получилось!", style = MaterialTheme.typography.headlineSmall, color = c.text)
                    Text("Верно: $correct из ${topic.questions.size}", style = MaterialTheme.typography.titleMedium, color = c.textMuted)
                    if (!passed) Text("Нужно 80% — перечитай правило и попробуй ещё раз.", color = c.textMuted, textAlign = TextAlign.Center)
                }
                LfButton("Ещё раз", onClick = { index = 0; correct = 0; picked = null; finished = null; quiz = true }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                LfButton("К списку тем", onClick = onBack, modifier = Modifier.fillMaxWidth().testTag("topic-done"))
            }
            !quiz -> {
                Text("${topic.level} · ${topic.book.title}", style = MaterialTheme.typography.labelLarge, color = c.gems)
                Text(topic.rule, style = MaterialTheme.typography.bodyLarge, color = c.text)
                Text("Примеры", style = MaterialTheme.typography.titleMedium, color = c.text)
                topic.examples.forEach { ex ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, c.border, RoundedCornerShape(14.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(ex, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                        SpeakChip { viewModel.speak(ex.substringBefore(" — ")) }
                    }
                }
                if (topic.questions.isNotEmpty()) LfButton("Проверить себя · ${topic.questions.size} вопр.", onClick = { quiz = true }, modifier = Modifier.fillMaxWidth().testTag("topic-quiz"))
            }
            else -> {
                val q = topic.questions[index]
                LfProgressBar(index.toFloat() / topic.questions.size, label = "Тест")
                Text(q.prompt, style = MaterialTheme.typography.titleLarge, color = c.text)
                q.speech?.let { s -> Row(verticalAlignment = Alignment.CenterVertically) { SpeakChip { viewModel.speak(s) }; Text("Послушать", color = c.textMuted) } }
                q.choices.forEachIndexed { i, choice ->
                    val st = when {
                        picked == null -> ChoiceState.Idle
                        i == q.answer -> ChoiceState.Correct
                        i == picked -> ChoiceState.Wrong
                        else -> ChoiceState.Disabled
                    }
                    ChoiceCard(onClick = {
                        if (picked == null) { picked = i; if (i == q.answer) correct++ }
                    }, state = st, modifier = Modifier.fillMaxWidth().testTag("quiz-choice-$i")) {
                        Text(choice, style = MaterialTheme.typography.bodyLarge, color = c.text)
                    }
                }
                picked?.let { p ->
                    Text(if (p == q.answer) "Отлично!" else "Правильно: ${q.choices.getOrNull(q.answer)}", style = MaterialTheme.typography.titleMedium, color = if (p == q.answer) c.primary else c.error)
                    q.explanation?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.textMuted) }
                    LfButton("Продолжить", onClick = {
                        if (index + 1 >= topic.questions.size) finished = viewModel.recordQuiz(topic, correct)
                        else { index++; picked = null }
                    }, style = if (p == q.answer) LfButtonStyle.Correct else LfButtonStyle.Danger, modifier = Modifier.fillMaxWidth().testTag("quiz-next"))
                }
            }
        }
    }
}
