package org.linguafree.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.linguafree.core.ui.theme.LfTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Month calendar of the streak: study days (orange) and days saved by a freeze (blue). */
@Composable
fun StreakCalendar(month: YearMonth, study: Set<LocalDate>, frozen: Set<LocalDate>, today: LocalDate) {
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

