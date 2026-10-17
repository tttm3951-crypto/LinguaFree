package org.linguafree.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.linguafree.app.RootViewModel
import org.linguafree.app.StartupState
import org.linguafree.core.ui.theme.DarkMode
import org.linguafree.core.ui.theme.ThemeAccent
import org.linguafree.feature.path.LearnScreen
import org.linguafree.feature.lesson.LessonRoute
import org.linguafree.feature.lesson.lessonScreen
import org.linguafree.feature.leagues.LeaguesScreen
import org.linguafree.feature.onboarding.OnboardingScreen
import org.linguafree.feature.profile.AboutScreen
import org.linguafree.feature.profile.ProfileScreen
import org.linguafree.feature.profile.SettingsScreen
import org.linguafree.feature.profile.TeacherScreen
import org.linguafree.feature.shop.ShopScreen
import org.linguafree.feature.practice.DictionaryScreen
import org.linguafree.feature.practice.PracticeScreen
import org.linguafree.feature.practice.TopicScreen
import org.linguafree.feature.practice.TopicsScreen
import org.linguafree.feature.practice.WordsScreen
import org.linguafree.core.data.library.TopicBook
import org.linguafree.core.domain.model.SessionKind
import androidx.compose.runtime.remember
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
fun LinguaFreeRoot(root: RootViewModel) {
    val settings by root.settings.collectAsStateWithLifecycle()
    val startup by root.state.collectAsStateWithLifecycle()
    val darkMode = when (settings?.darkMode) { 1 -> DarkMode.Light; 2 -> DarkMode.Dark; else -> DarkMode.System }
    val accent = ThemeAccent.entries.firstOrNull { it.name == settings?.accent } ?: ThemeAccent.Forest
    LinguaFreeTheme(darkMode = darkMode, accent = accent) {
        when (val s = startup) {
            StartupState.Loading -> { Box(Modifier.fillMaxSize().background(LfTheme.colors.background)); return@LinguaFreeTheme }
            is StartupState.Failed -> { ContentError(s.errors); return@LinguaFreeTheme }
            StartupState.Ready -> if (settings == null) { Box(Modifier.fillMaxSize().background(LfTheme.colors.background)); return@LinguaFreeTheme }
        }
        val nav = rememberNavController()
        // Decide the start destination once, so finishing onboarding doesn't rebuild the graph.
        val startRoute = remember { if (settings?.onboarded == true) MainTab.Learn.route else ONBOARDING }
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
            NavHost(nav, startDestination = startRoute, modifier = Modifier.padding(padding)) {
                composable(MainTab.Learn.route) {
                    LearnScreen(
                        onStartSession = { kind, node, unit -> nav.navigate(LessonRoute.of(kind, node, unit)) },
                        onOpenShop = { nav.navigate(MainTab.Shop.route) { launchSingleTop = true } },
                    )
                }
                lessonScreen(
                    onClose = { nav.popBackStack() },
                    onStartSession = { route -> nav.navigate(route) { popUpTo(LessonRoute.PATTERN) { inclusive = true } } },
                    onOpenSettings = { nav.navigate(MainTab.Profile.route) { popUpTo(LessonRoute.PATTERN) { inclusive = true } } },
                )
                composable(ONBOARDING) {
                    OnboardingScreen(onFinished = { placement ->
                        nav.navigate(MainTab.Learn.route) { popUpTo(ONBOARDING) { inclusive = true } }
                        if (placement) nav.navigate(LessonRoute.of(SessionKind.Placement))
                    })
                }
                composable(MainTab.Practice.route) {
                    PracticeScreen(
                        onStart = { kind -> nav.navigate(LessonRoute.of(kind)) },
                        onDictionary = { nav.navigate(DICTIONARY) },
                        onTopics = { book -> nav.navigate("practice/topics/${book.name}") },
                        onWords = { nav.navigate(WORDS) },
                    )
                }
                composable(DICTIONARY) { DictionaryScreen(onBack = { nav.popBackStack() }) }
                composable(WORDS) { WordsScreen(onBack = { nav.popBackStack() }) }
                composable("practice/topics/{book}") { e ->
                    val book = runCatching { TopicBook.valueOf(e.arguments?.getString("book").orEmpty()) }.getOrDefault(TopicBook.Grammar)
                    TopicsScreen(book, onOpen = { id -> nav.navigate("practice/topic/$id") }, onBack = { nav.popBackStack() })
                }
                composable("practice/topic/{id}") { e -> TopicScreen(e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }) }
                composable(MainTab.Leagues.route) { LeaguesScreen() }
                composable(MainTab.Shop.route) { ShopScreen() }
                composable(MainTab.Profile.route) {
                    ProfileScreen(
                        onSettings = { nav.navigate(SETTINGS) },
                        onAbout = { nav.navigate(ABOUT) },
                        onTeacher = { nav.navigate(TEACHER) },
                    )
                }
                composable(SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
                composable(ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
                composable(TEACHER) { TeacherScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}

private const val ONBOARDING = "onboarding"
private const val DICTIONARY = "practice/dictionary"
private const val WORDS = "practice/words"
private const val SETTINGS = "profile/settings"
private const val ABOUT = "profile/about"
private const val TEACHER = "profile/teacher"

@Composable
private fun ContentError(errors: List<String>) {
    Column(
        Modifier.fillMaxSize().background(LfTheme.colors.background).statusBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FoxMascot(FoxEmotion.TryAgain, Modifier.width(180.dp))
        Text("Не удалось загрузить уроки", style = MaterialTheme.typography.headlineSmall, color = LfTheme.colors.text)
        Text("Файлы курса повреждены. Переустановите приложение. Подробности:", color = LfTheme.colors.textMuted)
        errors.take(10).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = LfTheme.colors.textMuted) }
    }
}
