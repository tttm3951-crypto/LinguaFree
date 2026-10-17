package org.linguafree.core.ui.mascot

import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.linguafree.core.ui.R

/**
 * The fox's emotions. Six are animated artwork; three reuse that artwork with a small badge
 * so the context is clear without new drawings.
 */
enum class FoxEmotion(internal val base: FoxArt, internal val badge: String?, val description: String) {
    /** Calm, attentive, paw wave: home screen, lesson start, onboarding. */
    Waiting(FoxArt.Waiting, null, "Лисёнок ждёт и машет лапкой"),
    /** Paw at cheek: before checking, hints, thinking. */
    Thinking(FoxArt.Thinking, null, "Лисёнок задумался"),
    /** Joyful bounce: correct answer. */
    Correct(FoxArt.Correct, null, "Лисёнок радуется верному ответу"),
    /** Encouraging nod: wrong answer, no hearts — never shaming. */
    TryAgain(FoxArt.TryAgain, null, "Лисёнок подбадривает: попробуем ещё"),
    /** Clapping: combo «5 подряд!», streak extended. */
    Streak(FoxArt.Streak, null, "Лисёнок хлопает в ладоши"),
    /** Arms up, jump: lesson complete, achievement, promotion. */
    Complete(FoxArt.Complete, null, "Лисёнок празднует победу"),
    /** Listening exercises. */
    Listening(FoxArt.Thinking, "🎧", "Лисёнок слушает"),
    /** Speaking exercises. */
    Speaking(FoxArt.Thinking, "🎤", "Лисёнок готов слушать тебя"),
    /** Calm reminder / rest state (notifications, hearts refilling). */
    Resting(FoxArt.TryAgain, "💤", "Лисёнок отдыхает"),
}

internal enum class FoxArt(val frames: List<Int>) {
    Waiting(listOf(R.drawable.fox_waiting_0, R.drawable.fox_waiting_1, R.drawable.fox_waiting_2, R.drawable.fox_waiting_3, R.drawable.fox_waiting_4, R.drawable.fox_waiting_5)),
    Thinking(listOf(R.drawable.fox_thinking_0, R.drawable.fox_thinking_1, R.drawable.fox_thinking_2, R.drawable.fox_thinking_3, R.drawable.fox_thinking_4, R.drawable.fox_thinking_5)),
    Correct(listOf(R.drawable.fox_correct_0, R.drawable.fox_correct_1, R.drawable.fox_correct_2, R.drawable.fox_correct_3, R.drawable.fox_correct_4, R.drawable.fox_correct_5)),
    TryAgain(listOf(R.drawable.fox_try_again_0, R.drawable.fox_try_again_1, R.drawable.fox_try_again_2, R.drawable.fox_try_again_3, R.drawable.fox_try_again_4, R.drawable.fox_try_again_5)),
    Streak(listOf(R.drawable.fox_streak_0, R.drawable.fox_streak_1, R.drawable.fox_streak_2, R.drawable.fox_streak_3, R.drawable.fox_streak_4, R.drawable.fox_streak_5)),
    Complete(listOf(R.drawable.fox_complete_0, R.drawable.fox_complete_1, R.drawable.fox_complete_2, R.drawable.fox_complete_3, R.drawable.fox_complete_4, R.drawable.fox_complete_5)),
}

/** Original frame timings of the artwork (ms): hold the key pose, then move quickly. */
private val FrameDurations = longArrayOf(450, 180, 280, 300, 180, 180)

/** Whether the user disabled animations system-wide (Accessibility → remove animations). */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * The LinguaFree fox, always on a transparent background.
 * @param loops how many times to play; `0` = forever. After finishing it rests on the first (key) pose.
 */
@Composable
fun FoxMascot(
    emotion: FoxEmotion,
    modifier: Modifier = Modifier,
    loops: Int = 0,
    breathing: Boolean = true,
) {
    val reduceMotion = rememberReduceMotion()
    val frames = emotion.base.frames
    var frame by remember(emotion) { mutableIntStateOf(0) }
    val pop = remember { Animatable(1f) }

    LaunchedEffect(emotion, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        // Small spring "pop" whenever the emotion changes makes reactions feel alive.
        pop.snapTo(0.92f)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(emotion, loops, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        var played = 0
        while (loops == 0 || played < loops) {
            for (i in frames.indices) {
                frame = i
                delay(FrameDurations[i % FrameDurations.size])
            }
            played++
        }
        frame = 0
    }

    val infinite = rememberInfiniteTransition(label = "fox-breath")
    val breath by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "breath",
    )
    val scale = pop.value * if (breathing && !reduceMotion) breath else 1f

    Box(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = emotion.description },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = frames[frame], animationSpec = tween(90), label = "fox-frame") { res ->
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    },
            )
        }
        emotion.badge?.let { badge ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val side = maxWidth * 0.26f
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = maxWidth * 0.06f)
                        .size(side)
                        .shadow(3.dp, CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(badge, fontSize = (side.value * 0.5f).sp) }
            }
        }
    }
}

/** A single still pose (e.g. for profile, notifications preview, avatars). */
@Composable
fun FoxStill(emotion: FoxEmotion = FoxEmotion.Waiting, modifier: Modifier = Modifier, frame: Int = 0) {
    Image(
        painter = painterResource(emotion.base.frames[frame.coerceIn(0, 5)]),
        contentDescription = emotion.description,
        contentScale = ContentScale.Fit,
        modifier = modifier.aspectRatio(1f),
    )
}
