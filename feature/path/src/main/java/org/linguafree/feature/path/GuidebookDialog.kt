package org.linguafree.feature.path

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.linguafree.core.domain.model.CourseUnit
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme

/** «Справочник» of a unit: short grammar notes in Russian with spoken examples. */
@Composable
fun GuidebookDialog(unit: CourseUnit, onSpeak: (String) -> Unit, onDismiss: () -> Unit) {
    val c = LfTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding().testTag("guidebook")) {
            Row(
                Modifier.fillMaxWidth().background(Color(unit.color)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FoxMascot(FoxEmotion.Thinking, Modifier.size(72.dp), breathing = false)
                Column(Modifier.weight(1f)) {
                    Text("Справочник", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
                    Text(unit.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(unit.guidebook.sections) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleLarge, color = c.text)
                        Text(section.text, style = MaterialTheme.typography.bodyLarge, color = c.text)
                        section.examples.forEach { ex ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surfaceRaised)
                                    .clickable { onSpeak(ex.en) }.padding(12.dp)
                                    .semantics { contentDescription = "Пример: ${ex.en}. ${ex.ru}" },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(c.gems).padding(6.dp)) { LfIcon(R.drawable.ic_speaker, size = 20.dp) }
                                Column {
                                    Text(ex.en, style = MaterialTheme.typography.titleMedium, color = c.text)
                                    if (ex.ru.isNotBlank()) Text(ex.ru, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                                }
                            }
                        }
                    }
                }
            }
            LfButton("Понятно", onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }
}
