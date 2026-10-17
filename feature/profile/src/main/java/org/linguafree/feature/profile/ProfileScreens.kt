package org.linguafree.feature.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.linguafree.core.data.platform.SpeechHelp
import org.linguafree.core.domain.game.LeagueRules
import org.linguafree.core.domain.model.AchievementState
import org.linguafree.core.domain.model.DailyGoal
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.components.StreakCalendar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.mascot.FoxWithOutfit
import org.linguafree.core.ui.mascot.Outfit
import org.linguafree.core.ui.theme.LfTheme
import org.linguafree.core.ui.theme.ThemeAccent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

const val REPO_URL = "https://github.com/tttm3951-crypto/LinguaFree"

@Composable
private fun MessageDialog(vm: ProfileViewModel) {
    val msg by vm.message.collectAsStateWithLifecycle()
    msg?.let { AlertDialog(onDismissRequest = vm::dismissMessage, text = { Text(it) }, confirmButton = { TextButton(onClick = vm::dismissMessage) { Text("OK") } }) }
}

@Composable
fun ProfileScreen(
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onTeacher: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LfTheme.colors
    val context = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportTo { text -> context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFrom(runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull())
    }
    var confirmImport by remember { mutableStateOf(false) }
    val s = state.settings

    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp).testTag("profile"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FoxWithOutfit(Outfit.of(s.outfit), Modifier.size(110.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.name.ifBlank { "Мой профиль" }, style = MaterialTheme.typography.headlineMedium, color = c.text)
                state.since?.let { Text("С нами с ${it.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru")))}", style = MaterialTheme.typography.bodySmall, color = c.textMuted) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LfIcon(R.drawable.ic_flag_en, size = 22.dp)
                    Text("Английский · цель ${s.dailyGoalMinutes} мин/день", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onSettings).padding(8.dp).testTag("open-settings")) {
                LfIcon(R.drawable.ic_settings_mono, size = 26.dp, tint = c.textMuted, contentDescription = "Настройки")
            }
        }

        Text("Статистика", style = MaterialTheme.typography.titleLarge, color = c.text)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(R.drawable.ic_flame, "${state.streak}", "Серия дней", Modifier.weight(1f))
            StatTile(R.drawable.ic_xp, "${state.totalXp}", "Всего XP", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val tier = LeagueRules.tiers[state.leagueTier.coerceIn(0, 9)]
            StatTile(R.drawable.ic_tab_leagues, tier.title, "Текущая лига", Modifier.weight(1f))
            StatTile(R.drawable.ic_book, "${state.wordsLearned}", "Слов выучено", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(R.drawable.ic_check, "${state.lessonsDone}", "Пройдено узлов", Modifier.weight(1f))
            StatTile(R.drawable.ic_crown, "${state.longest}", "Лучшая серия", Modifier.weight(1f))
        }

        Text("Календарь серии", style = MaterialTheme.typography.titleLarge, color = c.text)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(12.dp)) {
            StreakCalendar(YearMonth.now(), state.streakState.studyDays, state.streakState.frozenDays, LocalDate.now())
        }

        Text("Достижения", style = MaterialTheme.typography.titleLarge, color = c.text)
        state.achievements.forEach { AchievementRow(it) }

        Text("Прогресс", style = MaterialTheme.typography.titleLarge, color = c.text)
        LfButton("Экспорт прогресса в файл", onClick = { export.launch("LinguaFree-progress-${LocalDate.now()}.json") }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth().testTag("export"))
        LfButton("Импорт прогресса из файла", onClick = { confirmImport = true }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        LfButton("Для учителя и родителей", onClick = onTeacher, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        LfButton("О приложении", onClick = onAbout, style = LfButtonStyle.Ghost, modifier = Modifier.fillMaxWidth().testTag("open-about"))
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Восстановить прогресс?") },
            text = { Text("Текущий прогресс на этом телефоне заменится данными из файла. Подходят копии LinguaFree 1.0 и 0.9.") },
            confirmButton = { TextButton(onClick = { confirmImport = false; import.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Выбрать файл") } },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Отмена") } },
        )
    }
    MessageDialog(viewModel)
}

@Composable
private fun StatTile(@DrawableRes icon: Int, value: String, label: String, modifier: Modifier) {
    val c = LfTheme.colors
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LfIcon(icon, size = 28.dp)
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
    }
}

@Composable
private fun AchievementRow(a: AchievementState) {
    val c = LfTheme.colors
    val target = a.nextTarget ?: a.tiers.last()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            LfIcon(R.drawable.ic_medal, size = 48.dp)
            Text("${a.level}", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.padding(top = 14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${a.title} · уровень ${a.level} из ${a.tiers.size}", style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(if (a.maxed) "Максимальный уровень!" else a.description, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            LfProgressBar(if (a.maxed) 1f else a.value.toFloat() / target, color = c.xp, height = 10.dp, label = a.title)
            Text("${a.value.coerceAtMost(target)} / $target", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
    }
}

@Composable
private fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = LfTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onBack).padding(10.dp)) {
                LfIcon(R.drawable.ic_back_mono, size = 24.dp, tint = c.textMuted, contentDescription = "Назад")
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.text)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String?, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val c = LfTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted) }
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = c.primary), modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = LfTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(2.dp, c.border, RoundedCornerShape(14.dp))) {
        options.forEach { (v, label) ->
            Box(
                Modifier.weight(1f).background(if (v == selected) c.gems.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(v) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelMedium, color = if (v == selected) c.gems else c.textMuted) }
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state.settings
    val c = LfTheme.colors
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun ensureNotifications(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    var name by remember(state.loaded) { mutableStateOf(s.name) }
    ScreenScaffold("Настройки", onBack) {
        Text("Профиль", style = MaterialTheme.typography.titleLarge, color = c.text)
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(24); viewModel.update { st -> st.copy(name = name.trim()) } },
            label = { Text("Имя") }, singleLine = true, shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.gems, unfocusedBorderColor = c.border, focusedTextColor = c.text, unfocusedTextColor = c.text),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Цель дня", style = MaterialTheme.typography.titleMedium, color = c.text)
        Segmented(DailyGoal.minuteOptions.map { it to "$it мин" }, s.dailyGoalMinutes) { m -> viewModel.update { it.copy(dailyGoalMinutes = m) } }

        Text("Обучение", style = MaterialTheme.typography.titleLarge, color = c.text)
        SettingSwitch("Режим без сердец", "Ошибки не отнимают сердца", s.heartsDisabled, "settings-no-hearts") { v -> viewModel.update { it.copy(heartsDisabled = v) } }
        SettingSwitch("Звуки ответов", null, s.soundEnabled, "settings-sound") { v -> viewModel.update { it.copy(soundEnabled = v) } }
        SettingSwitch("Вибрация", null, s.hapticsEnabled, "settings-haptics") { v -> viewModel.update { it.copy(hapticsEnabled = v) } }
        val now = System.currentTimeMillis()
        if (!s.speakingEnabled(now) || !s.listeningEnabled(now)) {
            LfButton("Вернуть задания на говорение и аудирование", onClick = viewModel::resetSpeaking, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        }
        Text("Скорость английской речи: ${"%.1f".format(s.voiceSpeed)}×", style = MaterialTheme.typography.titleMedium, color = c.text)
        Slider(value = s.voiceSpeed, onValueChange = { v -> viewModel.update { it.copy(voiceSpeed = (v * 10).toInt() / 10f) } }, valueRange = 0.6f..1.4f, steps = 7)
        LfButton("Прослушать голос", onClick = viewModel::testVoice, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        LfButton("Офлайн-голос Android (озвучка)", onClick = {
            runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { runCatching { context.startActivity(SpeechHelp.fallbackSettingsIntent()) } }
        }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        Text(SpeechHelp.OFFLINE_PACK_HINT, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        LfButton("Офлайн-распознавание речи", onClick = {
            runCatching { context.startActivity(SpeechHelp.settingsIntent()) }.onFailure { runCatching { context.startActivity(SpeechHelp.fallbackSettingsIntent()) } }
        }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())

        Text("Напоминания", style = MaterialTheme.typography.titleLarge, color = c.text)
        SettingSwitch("Ежедневное напоминание", "Время: %02d:%02d".format(s.reminderMinute / 60, s.reminderMinute % 60), s.remindersEnabled, "settings-reminders") { v -> ensureNotifications(v); viewModel.update { it.copy(remindersEnabled = v) } }
        if (s.remindersEnabled) {
            Segmented(listOf(8 * 60 to "08:00", 12 * 60 to "12:00", 18 * 60 to "18:00", 19 * 60 to "19:00", 21 * 60 to "21:00"), s.reminderMinute) { m -> viewModel.update { it.copy(reminderMinute = m) } }
        }
        SettingSwitch("Вечером, если серия под угрозой", "Одно напоминание около 20:30, если сегодня ещё не было урока", s.eveningStreakReminder, "settings-evening") { v -> ensureNotifications(v); viewModel.update { it.copy(eveningStreakReminder = v) } }

        Text("Оформление", style = MaterialTheme.typography.titleLarge, color = c.text)
        Segmented(listOf(0 to "Как в системе", 1 to "Светлая", 2 to "Тёмная"), s.darkMode) { m -> viewModel.update { it.copy(darkMode = m) } }
        val ownedThemes = listOf(ThemeAccent.Forest) + ThemeAccent.entries.filter { "Theme${it.name}" in state.inventory }
        if (ownedThemes.size > 1) Segmented(ownedThemes.map { it.name to it.title }, s.accent) { a -> viewModel.update { it.copy(accent = a) } }
        else Text("Новые темы оформления — в магазине за кристаллы.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
    MessageDialog(viewModel)
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = LfTheme.colors
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
    ScreenScaffold("О приложении", onBack) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FoxMascot(FoxEmotion.Waiting, Modifier.width(160.dp))
            Text("LinguaFree", style = MaterialTheme.typography.displaySmall, color = c.primary)
            Text("Версия $version", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.testTag("about-version"))
            Text("Без рекламы и подписок", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.testTag("about-free"))
        }
        listOf(
            "Бесплатно — весь курс и все функции открыты.",
            "Без рекламы, подписок и покупок за деньги. Кристаллы зарабатываются только учёбой.",
            "Без трекеров и аналитики. У приложения нет доступа к интернету: прогресс хранится только на телефоне.",
            "Разрешения: вибрация, уведомления, микрофон (для говорения) и автозапуск напоминаний после перезагрузки.",
        ).forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge, color = c.text) }
        LfButton("Исходный код на GitHub", onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth().testTag("about-repo"))
        Text(REPO_URL, style = MaterialTheme.typography.bodySmall, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Благодарности и лицензии", style = MaterialTheme.typography.titleMedium, color = c.text)
        Text(
            "Шрифт Nunito — SIL Open Font License 1.1. Пары предложений справочника — Tatoeba (CC BY 2.0 FR), авторы указаны у каждой записи. " +
                "Уроки, иллюстрации, иконки и звуки созданы для LinguaFree.",
            style = MaterialTheme.typography.bodySmall, color = c.textMuted,
        )
    }
}

@Composable
fun TeacherScreen(onBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val c = LfTheme.colors
    val context = LocalContext.current
    val students by viewModel.students.collectAsStateWithLifecycle()
    val saveReport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.studyReport { text -> context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
    }
    val importReport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importReport(runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull())
    }
    ScreenScaffold("Для учителя и родителей", onBack) {
        Text("Отчёт ученика", style = MaterialTheme.typography.titleLarge, color = c.text)
        Text(
            "Файл с именем, датами занятий, уроками, XP и ошибками. Отправь его учителю любым удобным способом — приложение само ничего не передаёт.",
            style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
        )
        LfButton("Сохранить отчёт", onClick = { saveReport.launch("LinguaFree-study-report.json") }, modifier = Modifier.fillMaxWidth())
        Text("Кабинет преподавателя", style = MaterialTheme.typography.titleLarge, color = c.text)
        Text("Загрузи отчёты учеников, чтобы видеть их прогресс. Данные хранятся только на этом телефоне.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        LfButton("Загрузить отчёт ученика", onClick = { importReport.launch(arrayOf("application/json", "text/plain", "*/*")) }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        if (students.isEmpty()) Text("Пока нет отчётов.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        students.forEach { st ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("👤 ${st.name}", style = MaterialTheme.typography.titleMedium, color = c.text)
                if (st.legacyId) Text("Старый отчёт без ID: одноимённых учеников не различить.", style = MaterialTheme.typography.bodySmall, color = c.error)
                Text("Уроки: ${st.lessons}/${st.totalLessons} · XP: ${st.xp}", style = MaterialTheme.typography.bodyMedium, color = c.text)
                LfProgressBar(st.lessons.toFloat() / st.totalLessons.coerceAtLeast(1), height = 10.dp, label = "Уроки")
                Text("Дней занятий: за 7 дней — ${st.days7}, за 30 дней — ${st.days30}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                Text("Последнее занятие: ${st.lastDate.ifBlank { "нет данных" }} · ошибок к повторению: ${st.mistakes}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                if (st.xpDelta != null && st.lessonsDelta != null) Text("С прошлого отчёта: XP +${st.xpDelta}, уроков +${st.lessonsDelta}", style = MaterialTheme.typography.bodySmall, color = c.primary)
                Text("Обновлено: ${st.updated} · отчётов: ${st.reports}", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                LfButton("Удалить данные ученика", onClick = { viewModel.deleteStudent(st.key) }, style = LfButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    MessageDialog(viewModel)
}

@Suppress("unused")
private val unusedPadding = PaddingValues(0.dp)
