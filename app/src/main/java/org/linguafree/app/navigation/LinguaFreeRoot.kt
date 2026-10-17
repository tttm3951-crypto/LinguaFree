package org.linguafree.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.StatsTopBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme
import org.linguafree.core.ui.theme.LinguaFreeTheme

@Composable
fun LinguaFreeRoot() {
    LinguaFreeTheme {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val currentTab = MainTab.entries.firstOrNull { it.route == entry?.destination?.route }
        Scaffold(
            containerColor = LfTheme.colors.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentTab != null) LfBottomBar(currentTab, onSelect = { tab ->
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            },
        ) { padding ->
            NavHost(nav, startDestination = MainTab.Learn.route, modifier = Modifier.padding(padding)) {
                MainTab.entries.forEach { tab ->
                    composable(tab.route) { TabIntro(tab) }
                }
            }
        }
    }
}

/** Shell screen of each tab: the design system in action (top stats, fox, buttons). */
@Composable
private fun TabIntro(tab: MainTab) {
    val emotion = when (tab) {
        MainTab.Learn -> FoxEmotion.Waiting
        MainTab.Practice -> FoxEmotion.Thinking
        MainTab.Leagues -> FoxEmotion.Streak
        MainTab.Shop -> FoxEmotion.Correct
        MainTab.Profile -> FoxEmotion.Complete
    }
    Column(Modifier.fillMaxSize().background(LfTheme.colors.background).statusBarsPadding()) {
        StatsTopBar(streak = 0, studiedToday = false, gems = 0, hearts = 5, unlimitedHearts = false)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FoxMascot(emotion, Modifier.width(220.dp))
            Text(tab.title, style = MaterialTheme.typography.headlineMedium, color = LfTheme.colors.text)
            Text(
                "LinguaFree — бесплатно, без рекламы, без подписок и без трекеров. Всё работает офлайн.",
                style = MaterialTheme.typography.bodyLarge,
                color = LfTheme.colors.textMuted,
                textAlign = TextAlign.Center,
            )
            LfButton("Начать", onClick = {}, modifier = Modifier.fillMaxWidth())
            LfButton("Ещё", onClick = {}, modifier = Modifier.fillMaxWidth(), style = LfButtonStyle.Secondary)
        }
    }
}
