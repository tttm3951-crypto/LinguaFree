package org.linguafree.feature.lesson.exercises

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.linguafree.core.data.platform.SpeechHelp
import org.linguafree.core.data.platform.SpeechInput
import org.linguafree.core.domain.learning.LearnerAnswer
import org.linguafree.core.ui.R
import org.linguafree.core.ui.components.LfButton
import org.linguafree.core.ui.components.LfButtonStyle
import org.linguafree.core.ui.components.LfIcon
import org.linguafree.core.ui.components.Pressable3D
import org.linguafree.core.ui.mascot.FoxEmotion
import org.linguafree.core.ui.theme.LfTheme

/** 7. Read the phrase aloud. Scored by recognised words (not by accent). */
@Composable
fun SpeakExercise(scope: ExerciseScope) {
    val ex = scope.exercise
    val c = LfTheme.colors
    val context = LocalContext.current
    val speech = scope.speechInput
    val state by speech.state.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) speech.start()
    }

    DisposableEffect(ex.id) {
        speech.reset()
        onDispose { speech.cancel() }
    }
    LaunchedEffect(state) {
        val s = state
        if (s is SpeechInput.State.Result && !scope.locked) scope.submit(LearnerAnswer.Spoken(s.alternatives))
    }

    fun startListening() {
        if (scope.locked) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) speech.start() else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val listening = state is SpeechInput.State.Listening || state is SpeechInput.State.Level || state is SpeechInput.State.Partial
    val level = (state as? SpeechInput.State.Level)?.value ?: if (listening) 0.15f else 0f
    val pulse by animateFloatAsState(1f + level * 0.25f, label = "mic")

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Instruction(ex.displayInstruction)
        FoxPrompt(scope, ex.text, FoxEmotion.Speaking, withAudio = ex.audio ?: ex.text)
        Pressable3D(
            onClick = { if (listening) speech.stop() else startListening() },
            face = if (listening) c.gems else c.surface,
            lip = if (listening) c.gemsLip else c.border,
            borderColor = if (listening) null else c.border,
            modifier = Modifier.fillMaxWidth().testTag("mic").semantics { contentDescription = if (listening) "Остановить запись" else "Нажми и говори" },
            contentPadding = PaddingValues(vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }.clip(RoundedCornerShape(50)).background(if (listening) c.gemsLip else c.gems).padding(8.dp)) {
                    LfIcon(R.drawable.ic_mic, size = 26.dp)
                }
                Text(
                    if (listening) "Говори… (нажми, чтобы закончить)" else "Нажми и говори",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (listening) androidx.compose.ui.graphics.Color.White else c.gems,
                )
            }
        }
        (state as? SpeechInput.State.Partial)?.let { Text("«${it.text}»", style = MaterialTheme.typography.bodyLarge, color = c.textMuted) }

        (state as? SpeechInput.State.Error)?.let { err ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(2.dp, c.border, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(err.message, style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.testTag("speech-error"))
                if (err.showPackHint) {
                    LfButton("Открыть настройки голосового ввода", onClick = {
                        runCatching { context.startActivity(SpeechHelp.settingsIntent()) }
                            .onFailure { runCatching { context.startActivity(SpeechHelp.fallbackSettingsIntent()) } }
                    }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
                }
                LfButton("Попробовать снова", onClick = { startListening() }, style = LfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
        }
        Box(Modifier.fillMaxWidth().height(4.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CantDoNowButton("Не могу говорить сейчас") { scope.cantDoNow(speaking = true) }
        }
    }
}
