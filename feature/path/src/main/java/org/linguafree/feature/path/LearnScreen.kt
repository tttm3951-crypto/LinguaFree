package org.linguafree.feature.path

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.linguafree.core.domain.model.NodeType
import org.linguafree.core.domain.model.SessionKind
import androidx.compose.foundation.clickable
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.components.StatsTopBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme

/** Learn tab: course outline loaded from Room (stage 2). */
@Composable
fun LearnScreen(
    onStartSession: (SessionKind, String?, String?) -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LfTheme.colors
    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        StatsTopBar(state.stats.streak, state.stats.studiedToday, state.stats.gems, state.stats.hearts, state.stats.unlimitedHearts)
        val course = state.course
        if (course == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FoxMascot(FoxEmotion.Waiting, Modifier.size(96.dp))
                    Text(course.title, style = MaterialTheme.typography.headlineSmall, color = c.text)
                }
            }
            course.sections.forEach { section ->
                item { Text(section.title + " · " + section.cefr, style = MaterialTheme.typography.titleLarge, color = c.text) }
                items(section.units, key = { it.id }) { unit ->
                    val lessons = unit.nodes.filter { it.type == NodeType.Lesson }
                    val done = lessons.count { it.id in state.completed }
                    val next = unit.nodes.firstOrNull { (it.type == NodeType.Lesson || it.type == NodeType.Story) && it.id !in state.completed }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(unit.color))
                            .clickable {
                                if (next != null) onStartSession(if (next.type == NodeType.Story) SessionKind.Story else SessionKind.Lesson, next.id, unit.id)
                                else onStartSession(SessionKind.UnitReview, unit.nodes.firstOrNull { it.type == NodeType.Review }?.id, unit.id)
                            }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(unit.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Уроков: ${lessons.size} · пройдено: $done", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        LfProgressBar(if (lessons.isEmpty()) 0f else done.toFloat() / lessons.size, color = Color.White.copy(alpha = 0.9f), height = 10.dp)
                    }
                }
            }
        }
    }
}
