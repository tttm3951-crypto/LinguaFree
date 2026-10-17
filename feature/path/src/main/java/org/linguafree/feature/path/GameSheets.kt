package org.linguafree.feature.path

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import org.linguafree.core.domain.game.HeartsRules
import org.linguafree.core.domain.game.QuestRules
import org.linguafree.core.domain.game.ShopItem
import org.linguafree.core.domain.game.StreakRules
import org.linguafree.core.domain.model.Quest
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.mascot.rememberReduceMotion
import org.linguafree.core.ui.theme.LfTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Daily quests card shown above the path. */
@Composable
fun QuestsCard(quests: List<Quest>, monthly: Quest?, onOpen: () -> Unit) {
    if (quests.isEmpty()) return
    val c = LfTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen).padding(14.dp).testTag("quests-card"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LfIcon(R.drawable.ic_target, size = 24.dp)
            Text("Задания дня", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
            Text("${quests.count { it.done }}/${quests.size}", style = MaterialTheme.typography.titleSmall, color = c.textMuted)
        }
        quests.forEach { QuestRow(it) }
        monthly?.let {
            Text("Значок месяца: ${it.progress}/${it.target} заданий", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
    }
}

@Composable
private fun QuestRow(q: Quest) {
    val c = LfTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(q.title, style = MaterialTheme.typography.bodyMedium, color = c.text)
            LfProgressBar(q.progress.toFloat() / q.target, color = if (q.done) c.primary else c.xp, height = 12.dp, label = q.title)
        }
        Box(contentAlignment = Alignment.Center) {
            LfIcon(if (q.done) R.drawable.ic_chest_open else R.drawable.ic_chest, size = 32.dp)
        }
        Text("+${q.reward}💎", style = MaterialTheme.typography.labelMedium, color = c.gems)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsSheet(quests: List<Quest>, monthly: Quest?, onDismiss: () -> Unit) {
    val c = LfTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FoxMascot(FoxEmotion.Streak, Modifier.size(80.dp))
                Column {
                    Text("Задания", style = MaterialTheme.typography.headlineSmall, color = c.text)
                    Text("Каждый день — три новых. Награда приходит сразу.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
            quests.forEach { QuestRow(it) }
            monthly?.let { m ->
                val month = runCatching { YearMonth.parse(m.periodKey) }.getOrNull()
                val name = month?.month?.getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))?.replaceFirstChar { it.uppercase() } ?: ""
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.xp.copy(alpha = 0.15f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LfIcon(R.drawable.ic_medal, size = 40.dp)
                        Column {
                            Text("Значок месяца: $name", style = MaterialTheme.typography.titleMedium, color = c.text)
                            Text("Выполни ${QuestRules.MONTHLY_TARGET} ежедневных заданий за месяц · +${m.reward} 💎", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        }
                    }
                    LfProgressBar(m.progress.toFloat() / m.target, color = c.xp, label = "Значок месяца")
                    if (m.done) Text("Значок получен! 🏅", style = MaterialTheme.typography.titleSmall, color = c.xpLip)
                }
            }
        }
    }
}

/** Streak: flame, number, this month's calendar, freezes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakSheet(state: PathUiState, onShop: () -> Unit, onDismiss: () -> Unit) {
    val c = LfTheme.colors
    val reduce = rememberReduceMotion()
    val today = LocalDate.now()
    val st = state.streakState
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp).verticalScroll(rememberScrollState()).testTag("streak-sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (reduce) LfIcon(R.drawable.ic_flame, size = 96.dp) else {
                val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.streak_fire))
                LottieAnimation(comp, iterations = LottieConstants.IterateForever, modifier = Modifier.size(120.dp))
            }
            Text("${state.stats.streak}", style = MaterialTheme.typography.displayLarge, color = if (state.stats.studiedToday) c.streak else c.textMuted)
            Text(
                when {
                    state.stats.studiedToday -> "дн. подряд! Сегодня ты уже позанимался 🔥"
                    state.stats.streak > 0 -> "дн. подряд. Пройди урок сегодня, чтобы продолжить серию."
                    else -> "Начни серию: пройди один урок сегодня."
                },
                style = MaterialTheme.typography.bodyLarge, color = c.text, textAlign = TextAlign.Center,
            )
            Text("Лучшая серия: ${maxOf(st.longest, state.stats.streak)} дн.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            MonthCalendar(YearMonth.from(today), st.studyDays, st.frozenDays, today)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LfIcon(R.drawable.ic_snowflake, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text("Заморозки: ${state.freezes} из ${StreakRules.MAX_FREEZES}", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text("Если пропустишь день, заморозка спасёт серию автоматически.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
            if (state.freezes < StreakRules.MAX_FREEZES) {
                LfButton("Заморозка за ${ShopItem.StreakFreeze.price} 💎 — в магазине", onClick = onShop, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MonthCalendar(month: YearMonth, study: Set<LocalDate>, frozen: Set<LocalDate>, today: LocalDate) {
    val c = LfTheme.colors
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            month.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru")).replaceFirstChar { it.uppercase() } + " ${month.year}",
            style = MaterialTheme.typography.titleMedium, color = c.text,
        )
        Row(Modifier.fillMaxWidth()) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = c.textMuted, textAlign = TextAlign.Center)
            }
        }
        val cells = offset + days
        for (week in 0 until (cells + 6) / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (d in 0 until 7) {
                    val dayNo = week * 7 + d - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (dayNo in 1..days) {
                            val date = month.atDay(dayNo)
                            val bg = when {
                                date in study -> c.streak
                                date in frozen -> c.gems
                                else -> Color.Transparent
                            }
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(bg)
                                    .border(if (date == today) 2.dp else 0.dp, if (date == today) c.streak else Color.Transparent, CircleShape)
                                    .semantics { contentDescription = "$dayNo: " + when { date in study -> "занятие"; date in frozen -> "заморозка"; else -> "нет занятия" } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$dayNo", style = MaterialTheme.typography.labelMedium, color = if (bg == Color.Transparent) c.text else Color.White)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(c.streak, "занятие"); Legend(c.gems, "заморозка")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.bodySmall, color = LfTheme.colors.textMuted)
    }
}

/** Hearts: count, timer, free practice, gems, «Режим без сердец». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartsSheet(state: PathUiState, onPractice: () -> Unit, onBuy: () -> Unit, onToggleNoHearts: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val c = LfTheme.colors
    var left by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.heartsRefillAt) {
        while (true) {
            left = state.heartsRefillAt?.let { (it - System.currentTimeMillis()).coerceAtLeast(0) } ?: 0L
            delay(1000)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(
            Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp).testTag("hearts-sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(HeartsRules.MAX) { i ->
                    LfIcon(if (state.heartsDisabled || i < state.stats.hearts) R.drawable.ic_heart else R.drawable.ic_heart_empty, size = 36.dp)
                }
            }
            Text(
                when {
                    state.heartsDisabled -> "Режим без сердец: ошибки ничего не стоят"
                    state.stats.hearts >= HeartsRules.MAX -> "Все сердца на месте!"
                    else -> "Следующее сердце через %d:%02d".format(left / 60000, left / 1000 % 60)
                },
                style = MaterialTheme.typography.titleMedium, color = c.text, textAlign = TextAlign.Center,
            )
            Text("Сердце тратится за ошибку в уроке и возвращается каждые 30 минут.", style = MaterialTheme.typography.bodySmall, color = c.textMuted, textAlign = TextAlign.Center)
            if (!state.heartsDisabled && state.stats.hearts < HeartsRules.MAX) {
                LfButton("Тренировка — вернуть все бесплатно", onClick = onPractice, modifier = Modifier.fillMaxWidth())
                LfButton(
                    "Восстановить за ${ShopItem.HeartsRefill.price} 💎", onClick = onBuy, style = LfButtonStyle.Gems,
                    enabled = state.stats.gems >= ShopItem.HeartsRefill.price, modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Режим без сердец", style = MaterialTheme.typography.titleMedium, color = c.text)
                    Text("Учись в своём темпе — ошибки не отнимают сердца.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
                Switch(
                    checked = state.heartsDisabled, onCheckedChange = onToggleNoHearts,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.primary),
                    modifier = Modifier.testTag("no-hearts-switch"),
                )
            }
            Box(Modifier.width(1.dp))
        }
    }
}
