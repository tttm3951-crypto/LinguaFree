package org.linguafree.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.linguafree.core.domain.model.DailyGoal
import org.linguafree.core.domain.model.LearningGoal
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.ui.components.ChoiceCard
import org.linguafree.core.ui.components.ChoiceState
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    fun finish(name: String, goal: LearningGoal, minutes: Int, reminders: Boolean, reminderMinute: Int, placement: Boolean, done: () -> Unit) {
        viewModelScope.launch {
            settings.update {
                it.copy(
                    onboarded = true, name = name.trim().take(24), goal = goal, dailyGoalMinutes = minutes,
                    remindersEnabled = reminders, reminderMinute = reminderMinute,
                    placementDone = !placement, createdAt = if (it.createdAt == 0L) System.currentTimeMillis() else it.createdAt,
                )
            }
            done()
        }
    }
}

private const val STEPS = 6

/**
 * Onboarding without registration: goal → daily goal → level → reminders (with an explanation
 * before the notification permission) → profile name. Everything stays on the phone.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(onFinished: (placement: Boolean) -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val c = LfTheme.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableStateOf<LearningGoal?>(null) }
    var minutes by rememberSaveable { mutableIntStateOf(10) }
    var placement by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var reminders by rememberSaveable { mutableStateOf(true) }
    var name by rememberSaveable { mutableStateOf("") }
    val time = rememberTimePickerState(initialHour = 19, initialMinute = 0, is24Hour = true)
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { step++ }

    BackHandler(enabled = step > 0) { step-- }

    val fox = when (step) { 0 -> FoxEmotion.Waiting; 1 -> FoxEmotion.Thinking; 2 -> FoxEmotion.Correct; 3 -> FoxEmotion.Thinking; 4 -> FoxEmotion.Resting; else -> FoxEmotion.Complete }
    val bubble = when (step) {
        0 -> "Привет! Я лисёнок LinguaFree. Будем учить английский вместе — бесплатно, без рекламы и подписок."
        1 -> "Зачем тебе английский?"
        2 -> "Сколько времени в день готов заниматься?"
        3 -> "Как хорошо ты уже знаешь английский?"
        4 -> "Когда напомнить о занятии?"
        else -> "Как тебя зовут? Имя видно только тебе."
    }
    val canContinue = when (step) { 1 -> goal != null; 3 -> placement != null; else -> true }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) { LfProgressBar((step + 1f) / STEPS, label = "Шаг онбординга") }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FoxMascot(fox, Modifier.width(if (step == 0) 150.dp else 104.dp))
                Text(
                    bubble,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(14.dp).testTag("onboarding-bubble"),
                )
            }
            AnimatedContent(step, transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut() }, label = "step") { s ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (s) {
                        0 -> {
                            listOf(
                                "🎯" to "Короткие уроки по 3–5 минут",
                                "🔥" to "Серии дней, лиги и награды",
                                "🎧" to "Аудирование и говорение без интернета",
                                "💚" to "Всё бесплатно: нет рекламы, подписок и трекеров",
                            ).forEach { (e, t) -> Text("$e  $t", style = MaterialTheme.typography.bodyLarge, color = c.text) }
                        }
                        1 -> FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LearningGoal.entries.forEach { g ->
                                ChoiceCard(onClick = { goal = g }, state = if (goal == g) ChoiceState.Selected else ChoiceState.Idle, modifier = Modifier.testTag("goal-${g.name}")) {
                                    Text("${g.emoji} ${g.title}", style = MaterialTheme.typography.titleSmall, color = c.text)
                                }
                            }
                        }
                        2 -> DailyGoal.minuteOptions.zip(listOf("Легко", "Нормально", "Серьёзно", "Интенсивно")).forEach { (m, label) ->
                            ChoiceCard(onClick = { minutes = m }, state = if (minutes == m) ChoiceState.Selected else ChoiceState.Idle, modifier = Modifier.fillMaxWidth().testTag("minutes-$m"), contentPadding = PaddingValues(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$m мин в день", style = MaterialTheme.typography.titleMedium, color = c.text)
                                    Text("$label · ${DailyGoal.xpForMinutes(m)} XP", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                                }
                            }
                        }
                        3 -> {
                            ChoiceCard(onClick = { placement = false }, state = if (placement == false) ChoiceState.Selected else ChoiceState.Idle, modifier = Modifier.fillMaxWidth().testTag("level-zero"), contentPadding = PaddingValues(16.dp)) {
                                Column { Text("🌱 Начинаю с нуля", style = MaterialTheme.typography.titleMedium, color = c.text); Text("Начнём с самых первых слов", style = MaterialTheme.typography.bodySmall, color = c.textMuted) }
                            }
                            ChoiceCard(onClick = { placement = true }, state = if (placement == true) ChoiceState.Selected else ChoiceState.Idle, modifier = Modifier.fillMaxWidth().testTag("level-test"), contentPadding = PaddingValues(16.dp)) {
                                Column { Text("🧭 Уже немного знаю", style = MaterialTheme.typography.titleMedium, color = c.text); Text("Короткий тест определит, с какого юнита начать", style = MaterialTheme.typography.bodySmall, color = c.textMuted) }
                            }
                        }
                        4 -> {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Ежедневное напоминание", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                                Switch(checked = reminders, onCheckedChange = { reminders = it }, colors = SwitchDefaults.colors(checkedTrackColor = c.primary))
                            }
                            if (reminders) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimeInput(state = time) }
                                Text(
                                    "Лисёнок пришлёт одно дружелюбное напоминание в это время и одно вечером, если серия под угрозой. Сейчас Android спросит разрешение на уведомления — его можно отозвать в любой момент.",
                                    style = MaterialTheme.typography.bodyMedium, color = c.textMuted,
                                )
                            }
                        }
                        else -> {
                            OutlinedTextField(
                                value = name, onValueChange = { name = it.take(24).replace("\n", "") },
                                label = { Text("Имя") }, singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.gems, unfocusedBorderColor = c.border, focusedTextColor = c.text, unfocusedTextColor = c.text),
                                modifier = Modifier.fillMaxWidth().testTag("name-input"),
                            )
                            Text("Без регистрации: профиль хранится только на этом телефоне. Можно оставить пустым.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        }
                    }
                }
            }
        }
        LfButton(
            when (step) { 0 -> "Начать"; STEPS - 1 -> "Поехали!"; else -> "Продолжить" },
            enabled = canContinue,
            onClick = {
                when {
                    step == 4 && reminders && Build.VERSION.SDK_INT >= 33 -> notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    step < STEPS - 1 -> step++
                    else -> viewModel.finish(name, goal ?: LearningGoal.Other, minutes, reminders, time.hour * 60 + time.minute, placement == true) {
                        onFinished(placement == true)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("onboarding-continue"),
        )
    }
}
