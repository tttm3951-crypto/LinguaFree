package org.linguafree.feature.path

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.linguafree.core.domain.model.NodeState
import org.linguafree.core.domain.model.NodeType
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.path.PlannedNode
import org.linguafree.core.domain.path.PlannedUnit
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.components.Pressable3D
import org.linguafree.core.ui.components.StatsTopBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.mascot.rememberReduceMotion
import org.linguafree.core.ui.theme.LfTheme

/** Entry point of the «Обучение» tab. */
@Composable
fun LearnScreen(
    onStartSession: (SessionKind, String?, String?) -> Unit,
    onOpenShop: () -> Unit = {},
) = PathScreen(onStartSession, onOpenShop)

private sealed interface PathItem {
    val key: String
    data class SectionHeader(val title: String, val cefr: String, val description: String, override val key: String) : PathItem
    data class Banner(val unit: PlannedUnit, override val key: String) : PathItem
    data class Node(val node: PlannedNode, val unit: PlannedUnit, override val key: String) : PathItem
}

@Composable
fun PathScreen(
    onStartSession: (SessionKind, String?, String?) -> Unit,
    onOpenShop: () -> Unit,
    viewModel: PathViewModel = hiltViewModel(),
) {
    var sheet by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val guide by viewModel.guidebook.collectAsStateWithLifecycle()
    val c = LfTheme.colors
    var skipConfirm by remember { mutableStateOf<PlannedUnit?>(null) }
    var chestGems by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(event) {
        when (val e = event) {
            is PathEvent.Start -> { onStartSession(e.kind, e.nodeId, e.unitId); viewModel.consumeEvent() }
            is PathEvent.ChestOpened -> { chestGems = e.gems; viewModel.consumeEvent() }
            null -> Unit
        }
    }

    val items = remember(state.units) {
        buildList {
            var lastSection = ""
            state.units.forEach { u ->
                if (u.section.id != lastSection) {
                    lastSection = u.section.id
                    add(PathItem.SectionHeader(u.section.title, u.section.cefr, u.section.description, "s-${u.section.id}"))
                }
                add(PathItem.Banner(u, "b-${u.unit.id}"))
                u.nodes.forEach { add(PathItem.Node(it, u, "n-${it.node.id}")) }
            }
        }
    }
    val currentIndex = items.indexOfFirst { it is PathItem.Node && it.node.node.id == state.currentNodeId }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && !initialScrollDone) {
            // Put the current lesson in the upper third of the screen, whatever the font scale.
            val viewport = snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
            listState.scrollToItem(currentIndex)
            listState.scrollBy(-viewport * 0.3f)
            initialScrollDone = true
        }
    }
    val currentVisible by remember(currentIndex) { derivedStateOf { isVisible(listState, currentIndex) } }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        StatsTopBar(
            state.stats.streak, state.stats.studiedToday, state.stats.gems, state.stats.hearts, state.stats.unlimitedHearts,
            onStreakClick = { sheet = "streak" }, onGemsClick = onOpenShop, onHeartsClick = { sheet = "hearts" },
        )
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.primary) }
            return
        }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("path"), contentPadding = PaddingValues(bottom = 96.dp)) {
                item(key = "header") {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DailyGoalRow(state.todayXp, state.dailyGoalXp)
                        QuestsCard(state.quests, state.monthly, onOpen = { sheet = "quests" })
                    }
                }
                items.forEach { entry ->
                    item(key = entry.key) {
                        when (entry) {
                            is PathItem.SectionHeader -> SectionHeader(entry)
                            is PathItem.Banner -> UnitBanner(
                                entry.unit,
                                onGuide = { viewModel.showGuidebook(entry.unit.unit) },
                                onSkip = { skipConfirm = entry.unit },
                            )
                            is PathItem.Node -> NodeRow(
                                planned = entry.node,
                                unit = entry.unit,
                                selected = selected == entry.node.node.id,
                                onTap = { viewModel.select(entry.node.node.id) },
                                onStart = { viewModel.open(entry.node, entry.unit) },
                                onLegendary = { viewModel.legendary(entry.node, entry.unit) },
                            )
                        }
                    }
                }
            }
            // «Перейти к текущему» button when the current lesson is scrolled away.
            androidx.compose.animation.AnimatedVisibility(
                visible = currentIndex >= 0 && !currentVisible,
                enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                val above = currentIndex < listState.firstVisibleItemIndex
                Pressable3D(
                    onClick = { scope.launch { listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0)) } },
                    face = c.surface, lip = c.border, borderColor = c.border, shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.semantics { contentDescription = "Перейти к текущему уроку" }.testTag("jump-current"),
                ) {
                    LfIcon(R.drawable.ic_arrow_down_mono, size = 28.dp, tint = c.gems, modifier = Modifier.scale(1f, if (above) -1f else 1f))
                }
            }
        }
    }

    when (sheet) {
        "streak" -> StreakSheet(state, onShop = { sheet = null; onOpenShop() }, onDismiss = { sheet = null })
        "hearts" -> HeartsSheet(
            state,
            onPractice = { sheet = null; viewModel.startHeartsPractice() },
            onToggleNoHearts = viewModel::setHeartsDisabled,
            onDismiss = { sheet = null },
        )
        "quests" -> QuestsSheet(state.quests, state.monthly, onDismiss = { sheet = null })
    }
    toast?.let { msg ->
        AlertDialog(onDismissRequest = { toast = null }, text = { Text(msg) }, confirmButton = { TextButton(onClick = { toast = null }) { Text("OK") } })
    }

    guide?.let { unit -> GuidebookDialog(unit, onSpeak = viewModel::speak, onDismiss = { viewModel.showGuidebook(null) }) }

    skipConfirm?.let { u ->
        AlertDialog(
            onDismissRequest = { skipConfirm = null },
            title = { Text("Перейти к юниту «${u.unit.title}»?") },
            text = { Text("Пройди короткую проверку. Если ошибок будет не больше трёх, все предыдущие уроки откроются. Это бесплатно и сердца не тратятся.") },
            confirmButton = { TextButton(onClick = { skipConfirm = null; viewModel.skipTo(u) }) { Text("Начать проверку") } },
            dismissButton = { TextButton(onClick = { skipConfirm = null }) { Text("Отмена") } },
        )
    }
    chestGems?.let { g ->
        AlertDialog(
            onDismissRequest = { chestGems = null },
            icon = { FoxMascot(FoxEmotion.Correct, Modifier.size(120.dp), loops = 2) },
            title = { Text("Сундук открыт!") },
            text = { Text("+$g 💎 кристаллов за упорство. Их можно потратить в магазине.", textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { chestGems = null }) { Text("Отлично!") } },
        )
    }
}

private fun isVisible(state: LazyListState, index: Int): Boolean {
    if (index < 0) return true
    val visible = state.layoutInfo.visibleItemsInfo
    return visible.any { it.index == index }
}

@Composable
private fun DailyGoalRow(todayXp: Int, goal: Int) {
    val c = LfTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LfIcon(R.drawable.ic_xp, size = 26.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (todayXp >= goal) "Цель дня выполнена! $todayXp XP" else "Цель дня: $todayXp / $goal XP",
                style = MaterialTheme.typography.labelMedium, color = c.textMuted,
            )
            LfProgressBar(todayXp.toFloat() / goal.coerceAtLeast(1), color = c.xp, height = 10.dp, label = "Цель дня")
        }
    }
}

@Composable
private fun SectionHeader(item: PathItem.SectionHeader) {
    val c = LfTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalDivider(Modifier.weight(1f), thickness = 2.dp, color = c.border)
            Text("${item.title} · ${item.cefr}", style = MaterialTheme.typography.titleMedium, color = c.textMuted)
            HorizontalDivider(Modifier.weight(1f), thickness = 2.dp, color = c.border)
        }
        Text(item.description, style = MaterialTheme.typography.bodySmall, color = c.textMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun UnitBanner(unit: PlannedUnit, onGuide: () -> Unit, onSkip: () -> Unit) {
    val color = Color(unit.unit.color)
    val lip = Color(
        red = color.red * 0.78f, green = color.green * 0.78f, blue = color.blue * 0.78f, alpha = 1f,
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(lip).padding(bottom = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ЮНИТ ${unit.unit.order + 1}".uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                    Text(unit.unit.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    if (unit.unit.description.isNotBlank()) Text(unit.unit.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                }
                if (!unit.unit.guidebook.isEmpty) {
                    Pressable3D(
                        onClick = onGuide, face = Color.White.copy(alpha = 0.18f), lip = lip,
                        borderColor = Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("guide-${unit.unit.id}"),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LfIcon(R.drawable.ic_book, size = 26.dp)
                            Text("Справочник", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }
        }
        if (unit.locked) {
            LfButton(
                "Перейти сюда?", onClick = onSkip, style = LfButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("skip-${unit.unit.id}"),
            )
        }
    }
}

@Composable
private fun NodeRow(
    planned: PlannedNode,
    unit: PlannedUnit,
    selected: Boolean,
    onTap: () -> Unit,
    onStart: () -> Unit,
    onLegendary: () -> Unit,
) {
    val c = LfTheme.colors
    val n = planned.node
    val unitColor = Color(unit.unit.color)
    val lipOf = { col: Color -> Color(col.red * 0.78f, col.green * 0.78f, col.blue * 0.78f, 1f) }
    val (face, lip) = when (planned.state) {
        NodeState.Locked -> c.locked to c.lockedLip
        NodeState.Legendary -> c.legendary to c.xpLip
        else -> unitColor to lipOf(unitColor)
    }
    val icon = when (n.type) {
        NodeType.Chest -> if (planned.state == NodeState.Completed || planned.state == NodeState.Legendary) R.drawable.ic_chest_open else R.drawable.ic_chest
        NodeType.Story -> R.drawable.ic_story
        NodeType.Review -> R.drawable.ic_dumbbell
        NodeType.Test -> R.drawable.ic_trophy
        NodeType.Lesson -> when (planned.state) {
            NodeState.Completed -> R.drawable.ic_check
            NodeState.Legendary -> R.drawable.ic_crown
            NodeState.Locked -> R.drawable.ic_lock
            NodeState.Current -> R.drawable.ic_star
        }
    }
    val isCurrent = planned.state == NodeState.Current
    val reduceMotion = rememberReduceMotion()
    val pulse = rememberInfiniteTransition(label = "node")
    val ring by pulse.animateFloat(1f, 1.18f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "ring")

    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        val shift = (maxWidth / 2 - 60.dp) * planned.offset
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.offset(x = shift), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    // «НАЧАТЬ» bubble above the current node.
                    Text(
                        "НАЧАТЬ",
                        style = MaterialTheme.typography.labelLarge,
                        color = unitColor,
                        modifier = Modifier
                            .offset(y = (-58).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.surface)
                            .border(2.dp, c.border, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    Box(
                        Modifier.size(96.dp).scale(if (reduceMotion) 1f else ring).border(6.dp, unitColor.copy(alpha = 0.35f), CircleShape)
                    )
                }
                Pressable3D(
                    onClick = onTap,
                    face = face,
                    lip = lip,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(width = 76.dp, height = 72.dp)
                        .semantics { contentDescription = "${n.title}: " + stateLabel(planned.state) }
                        .testTag("node-${n.id}"),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LfIcon(icon, size = 34.dp) }
                }
                if (isCurrent) {
                    FoxMascot(
                        FoxEmotion.Waiting,
                        Modifier.size(84.dp).offset(x = if (planned.offset <= 0f) 92.dp else (-92).dp),
                    )
                }
            }
            AnimatedVisibility(visible = selected, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                NodePopover(planned, unit, unitColor, onStart, onLegendary)
            }
        }
    }
}

private fun stateLabel(s: NodeState) = when (s) {
    NodeState.Locked -> "закрыто"
    NodeState.Current -> "текущий"
    NodeState.Completed -> "пройден"
    NodeState.Legendary -> "легендарный"
}

@Composable
private fun NodePopover(planned: PlannedNode, unit: PlannedUnit, color: Color, onStart: () -> Unit, onLegendary: () -> Unit) {
    val c = LfTheme.colors
    val n = planned.node
    val lessons = unit.nodes.filter { it.node.type == NodeType.Lesson }
    val lessonNo = lessons.indexOfFirst { it.node.id == n.id } + 1
    Column(
        Modifier
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (planned.state == NodeState.Locked) c.surfaceRaised else color)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val onColor = if (planned.state == NodeState.Locked) c.textMuted else Color.White
        Text(n.title, style = MaterialTheme.typography.titleLarge, color = onColor)
        Text(
            when {
                planned.state == NodeState.Locked -> "Пройди предыдущие уроки, чтобы открыть этот"
                n.type == NodeType.Lesson -> "Урок $lessonNo из ${lessons.size}" + if (n.newWords.isNotEmpty()) " · новых слов: ${n.newWords.size}" else ""
                n.type == NodeType.Chest -> if (planned.state == NodeState.Current) "Открой сундук и получи кристаллы!" else "Сундук уже открыт"
                n.type == NodeType.Story -> "История с вопросами на понимание"
                n.type == NodeType.Review -> "Повтори слабые слова этого юнита"
                else -> "Проверь, как ты усвоил юнит. Можно ошибиться 3 раза"
            },
            style = MaterialTheme.typography.bodyMedium, color = onColor,
        )
        if (planned.state != NodeState.Locked && !(n.type == NodeType.Chest && planned.state != NodeState.Current)) {
            LfButton(
                when {
                    n.type == NodeType.Chest -> "Открыть"
                    planned.state == NodeState.Current -> "Начать"
                    else -> "Повторить"
                },
                onClick = onStart, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth().testTag("node-start"),
            )
        }
        if (planned.state == NodeState.Completed && (n.type == NodeType.Lesson || n.type == NodeType.Story)) {
            LfButton("Легендарный уровень", onClick = onLegendary, style = LfButtonStyle.Gold, modifier = Modifier.fillMaxWidth(),
                leading = { LfIcon(R.drawable.ic_crown, size = 22.dp) })
            Text("Пройди урок почти без ошибок (не больше 2) — узел станет золотым.", style = MaterialTheme.typography.bodySmall, color = onColor)
        }
        Spacer(Modifier.height(0.dp).width(0.dp))
    }
}
