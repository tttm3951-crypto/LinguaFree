package org.linguafree.feature.lesson

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.linguafree.core.domain.model.SessionKind

object LessonRoute {
    const val ARG_KIND = "kind"
    const val ARG_NODE = "node"
    const val ARG_UNIT = "unit"
    const val PATTERN = "lesson/{$ARG_KIND}?$ARG_NODE={$ARG_NODE}&$ARG_UNIT={$ARG_UNIT}"

    fun of(kind: SessionKind, nodeId: String? = null, unitId: String? = null): String =
        "lesson/${kind.key}?$ARG_NODE=${Uri.encode(nodeId.orEmpty())}&$ARG_UNIT=${Uri.encode(unitId.orEmpty())}"
}

/** Full-screen lesson destination (no bottom bar). */
fun NavGraphBuilder.lessonScreen(
    onClose: () -> Unit,
    onStartSession: (route: String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable(
        LessonRoute.PATTERN,
        arguments = listOf(
            navArgument(LessonRoute.ARG_KIND) { type = NavType.StringType },
            navArgument(LessonRoute.ARG_NODE) { type = NavType.StringType; defaultValue = "" },
            navArgument(LessonRoute.ARG_UNIT) { type = NavType.StringType; defaultValue = "" },
        ),
    ) {
        LessonScreen(onClose = onClose, onStartSession = onStartSession, onOpenSettings = onOpenSettings)
    }
}
