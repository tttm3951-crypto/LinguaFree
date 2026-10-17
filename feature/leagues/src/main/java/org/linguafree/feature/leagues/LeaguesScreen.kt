package org.linguafree.feature.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.linguafree.core.domain.game.LeagueRules
import org.linguafree.core.domain.model.LeagueStanding
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.LeagueState
import org.linguafree.core.domain.repository.ProgressRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.TimeProvider
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme
import javax.inject.Inject

data class LeaguesUiState(
    val standing: LeagueStanding? = null,
    val msLeft: Long = 0,
    val last: LeagueState? = null,
)

@HiltViewModel
class LeaguesViewModel @Inject constructor(
    private val game: GameRepository,
    progress: ProgressRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val ticker = flow { while (true) { emit(time.now()); delay(60_000) } }

    val state: StateFlow<LeaguesUiState> = combine(game.leagueTier(), progress.results(), settings.settings, ticker) { tier, results, s, now ->
        val zoned = time.zoned()
        val weekStart = LeagueRules.weekStartMillis(zoned.toLocalDate(), time.zone())
        val xp = results.filter { it.finishedAt >= weekStart }.sumOf { it.xp }
        val st = LeagueRules.standing(tier, s.name.ifBlank { "Ты" }, xp, zoned)
        LeaguesUiState(st, (st.endsAt - now).coerceAtLeast(0), game.leagueState())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeaguesUiState())
}

private val avatarColors = listOf(0xFF4CC417, 0xFFFF8A1F, 0xFF2EB8F2, 0xFFA56BF7, 0xFFFF4B6E, 0xFF34D1A2)

@Composable
fun LeaguesScreen(viewModel: LeaguesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LfTheme.colors
    val st = state.standing ?: return Box(Modifier.fillMaxSize().background(c.background))
    val tier = LeagueRules.tiers[st.tier.coerceIn(0, LeagueRules.TIERS - 1)]
    val userRank = st.entries.indexOfFirst { it.isUser } + 1

    LazyColumn(Modifier.fillMaxSize().background(c.background).statusBarsPadding().testTag("leagues"), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeagueRules.tiers.forEachIndexed { i, t ->
                        val current = i == st.tier
                        Box(
                            Modifier.size(if (current) 56.dp else 40.dp).clip(CircleShape)
                                .background(if (i <= st.tier) Color(t.color) else c.locked)
                                .border(if (current) 4.dp else 0.dp, if (current) c.xp else Color.Transparent, CircleShape)
                                .semantics { contentDescription = "Лига «${t.title}»" + if (current) ", текущая" else "" },
                            contentAlignment = Alignment.Center,
                        ) { Text(t.emoji, style = if (current) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium) }
                    }
                }
                Text("Лига «${tier.title}»", style = MaterialTheme.typography.headlineMedium, color = c.text, modifier = Modifier.testTag("league-title"))
                Text(
                    "Топ-${LeagueRules.PROMOTE} переходят в следующую лигу · итоги через ${formatLeft(state.msLeft)}",
                    style = MaterialTheme.typography.bodyMedium, color = c.textMuted, textAlign = TextAlign.Center,
                )
                state.last?.takeIf { it.lastResultWeek.isNotEmpty() && it.lastResultRank > 0 }?.let { last ->
                    Text(
                        "Прошлая неделя: ${last.lastResultRank}-е место" + when {
                            last.lastResultTierChange > 0 -> " — повышение! 🎉"
                            last.lastResultTierChange < 0 -> " — лига понижена, но всё поправимо"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall, color = c.primary,
                    )
                }
                if (!st.joined) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FoxMascot(FoxEmotion.Waiting, Modifier.width(64.dp))
                        Text("Пройди урок, чтобы присоединиться к лиге этой недели!", style = MaterialTheme.typography.bodyMedium, color = c.text)
                    }
                }
            }
        }
        itemsIndexed(st.entries, key = { _, e -> e.id }) { i, e ->
            val rank = i + 1
            if (rank == LeagueRules.PROMOTE + 1 && st.tier < LeagueRules.TIERS - 1) ZoneDivider("ЗОНА ПОВЫШЕНИЯ ↑", c.primary)
            if (rank == LeagueRules.SIZE - LeagueRules.DEMOTE + 1 && st.tier > 0) ZoneDivider("ЗОНА ПОНИЖЕНИЯ ↓", c.error)
            val zone = LeagueRules.zone(rank, st.tier)
            Row(
                Modifier.fillMaxWidth()
                    .background(if (e.isUser) c.gems.copy(alpha = 0.12f) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(if (e.isUser) "league-me" else "league-row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "$rank", modifier = Modifier.width(28.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = when (zone) { LeagueRules.Zone.Promotion -> c.primary; LeagueRules.Zone.Demotion -> c.error; else -> c.textMuted },
                )
                if (e.isUser) {
                    FoxMascot(FoxEmotion.Waiting, Modifier.size(40.dp), breathing = false)
                } else {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(avatarColors[e.avatar.coerceIn(0, avatarColors.lastIndex)])), contentAlignment = Alignment.Center) {
                        Text(e.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }
                Text(e.name + if (e.isUser) " (ты)" else "", style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                Text("${e.xp} XP", style = MaterialTheme.typography.titleSmall, color = if (e.isUser) c.gems else c.textMuted)
            }
        }
        item {
            Text(
                "Соперники — локальные участники, которые «занимаются» по своему расписанию. Никаких данных никуда не отправляется. " +
                    "Твоё место сейчас: $userRank из ${LeagueRules.SIZE}.",
                style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ZoneDivider(text: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(Modifier.weight(1f), thickness = 2.dp, color = color)
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        HorizontalDivider(Modifier.weight(1f), thickness = 2.dp, color = color)
    }
}

private fun formatLeft(ms: Long): String {
    val h = ms / 3_600_000
    val d = h / 24
    return if (d > 0) "$d д ${h % 24} ч" else "$h ч ${(ms / 60_000) % 60} мин"
}
