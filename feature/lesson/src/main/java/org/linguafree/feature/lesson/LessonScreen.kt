package org.linguafree.feature.lesson

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.components.LfProgressBar
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.mascot.FoxMascot
import org.linguafree.core.ui.theme.LfTheme
import org.linguafree.feature.lesson.exercises.ExerciseHost
import org.linguafree.feature.lesson.exercises.SelfCheckingTypes

@Composable
fun LessonScreen(
    onClose: () -> Unit,
    onStartSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = LfTheme.colors

    BackHandler(enabled = state.phase == LessonPhase.Playing) { viewModel.requestExit() }

    Box(Modifier.fillMaxSize().background(c.background)) {
        when (state.phase) {
            LessonPhase.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.primary) }
            LessonPhase.Playing -> Playing(state, viewModel)
            LessonPhase.OutOfHearts -> OutOfHeartsScreen(
                state = state,
                onPractice = { onStartSession(LessonRoute.of(SessionKind.HeartsRefill)) },
                onBuy = viewModel::buyHearts,
                onClose = onClose,
                onOpenSettings = onOpenSettings,
            )
            LessonPhase.Empty -> EmptySession(state.kind, onClose)
            LessonPhase.Summary -> SummaryScreen(state, onContinue = onClose)
            LessonPhase.TestFailed -> TestFailed(onRetry = viewModel::retry, onClose = onClose)
        }
        state.message?.let { msg ->
            LaunchedEffect(msg) { delay(2600); viewModel.dismissMessage() }
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 16.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    msg, style = MaterialTheme.typography.bodyMedium, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xE6333333)).padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }

    if (state.exitConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExit,
            title = { Text("Выйти из урока?") },
            text = { Text("Прогресс этого урока не сохранится. Лисёнок будет ждать тебя!") },
            confirmButton = { TextButton(onClick = viewModel::dismissExit) { Text("Продолжить урок") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissExit(); onClose() }) { Text("Выйти", color = c.error) } },
        )
    }
}

@Composable
private fun Playing(state: LessonUiState, vm: LessonViewModel) {
    val c = LfTheme.colors
    val ex = state.exercise ?: return
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        // Top bar: close · progress · hearts.
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { vm.requestExit() }
                    .semantics { contentDescription = "Закрыть урок" }.testTag("lesson-close"),
                contentAlignment = Alignment.Center,
            ) { LfIcon(R.drawable.ic_close_mono, size = 24.dp, tint = c.textMuted) }
            Box(Modifier.weight(1f)) { LfProgressBar(state.progress, label = "Прогресс урока") }
            when {
                state.mistakesLeft != null -> Text("❗ ${state.mistakesLeft}", style = MaterialTheme.typography.titleMedium, color = c.error)
                state.heartsEnabled -> Row(verticalAlignment = Alignment.CenterVertically) {
                    LfIcon(R.drawable.ic_heart, size = 26.dp)
                    Text(" ${state.hearts}", style = MaterialTheme.typography.titleMedium, color = c.heart, modifier = Modifier.testTag("hearts"))
                }
                else -> LfIcon(R.drawable.ic_heart_infinite, size = 26.dp, contentDescription = "Режим без сердец")
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (state.isRetry) {
                    Text("🔁 Ещё раз", style = MaterialTheme.typography.labelLarge, color = c.streak, modifier = Modifier.padding(bottom = 8.dp))
                }
                key(state.exerciseKey) {
                    ExerciseHost(
                        exercise = ex,
                        kind = state.kind,
                        locked = state.feedback != null,
                        checkedCorrect = state.feedback?.correct,
                        hints = state.hints,
                        speechInput = vm.speechIn,
                        onSpeak = { t, slow -> vm.speak(t, slow) },
                        onAnswer = vm::onAnswerChanged,
                        onSubmit = vm::submit,
                        onCantDoNow = vm::cantDoNow,
                        onTap = { vm.feedback.tap() },
                    )
                }
            }
            // «5 подряд!» banner with the clapping fox.
            androidx.compose.animation.AnimatedVisibility(
                visible = state.comboBanner != null,
                enter = scaleIn() + fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            ) {
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(c.streak).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FoxMascot(FoxEmotion.Streak, Modifier.size(48.dp), breathing = false)
                    Text("${state.comboBanner ?: state.combo} подряд!", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }

        // Bottom: «Проверить» or the feedback panel sliding up.
        val fb = state.feedback
        Box(Modifier.fillMaxWidth()) {
            if (fb == null && ex.type !in SelfCheckingTypes) {
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    LfButton("Проверить", onClick = vm::check, enabled = state.answer != null, modifier = Modifier.fillMaxWidth().testTag("check"))
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = fb != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                if (fb != null) FeedbackPanel(fb, state.fox, onContinue = vm::continueNext)
            }
        }
    }
}

@Composable
private fun FeedbackPanel(fb: FeedbackUi, fox: FoxEmotion, onContinue: () -> Unit) {
    val c = LfTheme.colors
    val accent = if (fb.correct) c.primary else c.error
    Column(
        Modifier.fillMaxWidth().background(if (fb.correct) c.correctPanel else c.wrongPanel).padding(16.dp).testTag(if (fb.correct) "feedback-correct" else "feedback-wrong"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FoxMascot(fox, Modifier.width(64.dp), breathing = false, loops = 2)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(fb.title, style = MaterialTheme.typography.headlineSmall, color = accent)
                fb.answer?.let {
                    Text(if (fb.correct) "Правильно: $it" else "Правильный ответ:", style = MaterialTheme.typography.titleSmall, color = accent)
                    if (!fb.correct) Text(it, style = MaterialTheme.typography.bodyLarge, color = accent, modifier = Modifier.testTag("correct-answer"))
                }
                fb.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = accent) }
            }
        }
        LfButton(
            "Продолжить", onClick = onContinue,
            style = if (fb.correct) LfButtonStyle.Correct else LfButtonStyle.Danger,
            modifier = Modifier.fillMaxWidth().testTag("continue"),
        )
    }
}

@Composable
private fun EmptySession(kind: SessionKind, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        FoxMascot(FoxEmotion.Resting, Modifier.width(200.dp))
        Text(
            if (kind == SessionKind.PracticeMistakes) "Ошибок для повторения нет — так держать!" else "Пока нечего повторять. Пройди пару уроков!",
            style = MaterialTheme.typography.headlineSmall, color = LfTheme.colors.text,
        )
        LfButton("Назад", onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TestFailed(onRetry: () -> Unit, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        FoxMascot(FoxEmotion.TryAgain, Modifier.width(220.dp))
        Text("Почти получилось!", style = MaterialTheme.typography.headlineMedium, color = LfTheme.colors.text)
        Text("Ошибок оказалось чуть больше, чем можно. Повтори материал и попробуй снова — это бесплатно.", style = MaterialTheme.typography.bodyLarge, color = LfTheme.colors.textMuted)
        LfButton("Попробовать снова", onClick = onRetry, modifier = Modifier.fillMaxWidth())
        LfButton("Назад", onClick = onClose, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
    }
}
