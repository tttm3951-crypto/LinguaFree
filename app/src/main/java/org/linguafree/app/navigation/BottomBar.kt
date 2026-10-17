package org.linguafree.app.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.theme.LfTheme

/** The five main tabs, in Duolingo-like order: Обучение · Практика · Лиги · Магазин · Профиль. */
enum class MainTab(val route: String, val title: String, @DrawableRes val icon: Int) {
    Learn("tab/learn", "Обучение", R.drawable.ic_tab_learn),
    Practice("tab/practice", "Практика", R.drawable.ic_tab_practice),
    Leagues("tab/leagues", "Лиги", R.drawable.ic_tab_leagues),
    Shop("tab/shop", "Магазин", R.drawable.ic_tab_shop),
    Profile("tab/profile", "Профиль", R.drawable.ic_tab_profile),
}

@Composable
fun LfBottomBar(current: MainTab?, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val c = LfTheme.colors
    Column(modifier.background(c.background).navigationBarsPadding()) {
        HorizontalDivider(thickness = 2.dp, color = c.border)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == current
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) c.gems.copy(alpha = if (c.isDark) 0.18f else 0.10f) else Color.Transparent)
                        .border(2.dp, if (selected) c.gems else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .semantics { this.selected = selected }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) { LfIcon(tab.icon, size = 30.dp) }
                    Text(
                        tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) c.gems else c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
