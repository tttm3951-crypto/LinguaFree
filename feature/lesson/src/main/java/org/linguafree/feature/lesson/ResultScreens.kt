package org.linguafree.feature.lesson

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import org.linguafree.core.domain.game.ShopItem
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.mascot.rememberReduceMotion
import org.linguafree.core.ui.theme.LfTheme

@Composable
fun SummaryScreen(state: LessonUiState, onContinue: () -> Unit) {
    val s = state.summary ?: return
    val c = LfTheme.colors
    val reduceMotion = rememberReduceMotion()
    val title = when {
        s.legendary -> "Легендарно!"
        s.unitSkipped -> "Юнит пропущен!"
        state.kind == SessionKind.Placement -> if (s.placementUnits > 0) "Стартуем с юнита ${s.placementUnits + 1}!" else "Начнём с самого начала!"
        state.kind == SessionKind.HeartsRefill -> "Сердца восстановлены!"
        state.kind.isPractice -> "Тренировка завершена!"
        state.kind == SessionKind.UnitTest -> "Проверка пройдена!"
        else -> "Урок пройден!"
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FoxMascot(if (s.streakExtended) FoxEmotion.Streak else FoxEmotion.Complete, Modifier.width(220.dp), loops = 3)
            Text(title, style = MaterialTheme.typography.headlineLarge, color = c.xpLip, textAlign = TextAlign.Center, modifier = Modifier.testTag("summary-title"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Опыт", "${s.xp} XP", c.xp, R.drawable.ic_xp, Modifier.weight(1f))
                StatCard("Точность", "${s.accuracy}%", c.primary, R.drawable.ic_target, Modifier.weight(1f))
                StatCard("Время", formatDuration(s.durationMs), c.gems, R.drawable.ic_calendar, Modifier.weight(1f))
            }
            if (s.gems > 0 || s.doubled || s.bonusXp > 0) {
                Text(
                    buildList {
                        if (s.gems > 0) add("+${s.gems} 💎")
                        if (s.bonusXp > 0) add("бонус +${s.bonusXp} XP")
                        if (s.doubled) add("двойной XP ×2")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium, color = c.gems,
                )
            }
            if (s.streakExtended) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LfIcon(R.drawable.ic_flame, size = 32.dp)
                    Text("Серия: ${s.streak} дн. Так держать!", style = MaterialTheme.typography.titleMedium, color = c.streak)
                }
            }
            if (s.dailyGoalXp > 0) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (s.todayXp >= s.dailyGoalXp) "Цель дня выполнена: ${s.todayXp}/${s.dailyGoalXp} XP 🎯" else "Цель дня: ${s.todayXp}/${s.dailyGoalXp} XP",
                        style = MaterialTheme.typography.bodyLarge, color = c.text,
                    )
                    LfProgressBar(s.todayXp.toFloat() / s.dailyGoalXp, color = c.xp, label = "Цель дня")
                }
            }
            s.questsCompleted.forEach { Text("✅ Задание выполнено: $it", style = MaterialTheme.typography.bodyLarge, color = c.primary) }
            s.achievementsUnlocked.forEach { Text("🏅 Достижение: $it", style = MaterialTheme.typography.bodyLarge, color = c.xpLip) }
            LfButton("Продолжить", onClick = onContinue, modifier = Modifier.fillMaxWidth().testTag("summary-continue"))
        }
        if (!reduceMotion) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
            LottieAnimation(composition, iterations = 1, modifier = Modifier.fillMaxWidth().padding(top = 40.dp).align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, @DrawableRes icon: Int, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).border(2.dp, color, RoundedCornerShape(16.dp)).padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LfIcon(icon, size = 20.dp)
            Text(value, style = MaterialTheme.typography.titleMedium, color = LfTheme.colors.text)
        }
    }
}

internal fun formatDuration(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(sec / 60, sec % 60)
}

/** Honest out-of-hearts screen: free practice first, gems second, waiting third. */
@Composable
fun OutOfHeartsScreen(
    state: LessonUiState,
    onPractice: () -> Unit,
    onBuy: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = LfTheme.colors
    var left by remember { mutableLongStateOf(state.msToNextHeart ?: 0L) }
    LaunchedEffect(state.msToNextHeart) {
        left = state.msToNextHeart ?: 0L
        while (left > 0) { delay(1000); left -= 1000 }
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FoxMascot(FoxEmotion.Resting, Modifier.width(200.dp))
        Text("Сердца закончились", style = MaterialTheme.typography.headlineMedium, color = c.text, modifier = Modifier.testTag("no-hearts"))
        Text("Ничего страшного! Ошибки — часть учёбы. Выбери, как продолжить:", style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = TextAlign.Center)
        LfButton("Тренировка: вернуть все сердца бесплатно", onClick = onPractice, modifier = Modifier.fillMaxWidth().testTag("hearts-practice"))
        LfButton(
            "Восстановить за ${ShopItem.HeartsRefill.price} 💎 (у тебя ${state.gems})",
            onClick = onBuy, style = LfButtonStyle.Gems, enabled = state.gems >= ShopItem.HeartsRefill.price,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            if (left > 0) "Или подожди: следующее сердце через ${formatDuration(left)}" else "Новое сердце уже появилось — можно продолжать!",
            style = MaterialTheme.typography.bodyMedium, color = c.textMuted, textAlign = TextAlign.Center,
        )
        LfButton("Режим без сердец — в настройках", onClick = onOpenSettings, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        LfButton("Назад", onClick = onClose, style = LfButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
    }
}
