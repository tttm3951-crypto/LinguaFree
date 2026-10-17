package org.linguafree.core.domain.path

import org.linguafree.core.domain.model.Course
import org.linguafree.core.domain.model.CourseUnit
import org.linguafree.core.domain.model.NodeProgress
import org.linguafree.core.domain.model.NodeState
import org.linguafree.core.domain.model.NodeType
import org.linguafree.core.domain.model.PathNode
import org.linguafree.core.domain.model.Section

/** A node with its computed state, ready for the path screen. */
data class PlannedNode(
    val node: PathNode,
    val state: NodeState,
    /** Horizontal offset in the "snake" layout, −1..1. */
    val offset: Float,
    val indexInUnit: Int,
)

data class PlannedUnit(
    val unit: CourseUnit,
    val section: Section,
    val nodes: List<PlannedNode>,
    val completed: Boolean,
    /** The unit is ahead of the learner: offer "перейти сюда" (jump test). */
    val locked: Boolean,
    val progress: Float,
)

/**
 * Linear unlocking: everything before the first unfinished node is done, that node is current,
 * everything after is locked. Completed lessons can be replayed or raised to the legendary level.
 */
object PathPlanner {

    /** Zig-zag pattern used by the snake path. */
    private val snake = floatArrayOf(0f, 0.45f, 0.8f, 0.45f, 0f, -0.45f, -0.8f, -0.45f)

    fun plan(course: Course, progress: Map<String, NodeProgress>): List<PlannedUnit> {
        val allNodes = course.sections.flatMap { s -> s.units.flatMap { it.nodes } }
        val currentId = allNodes.firstOrNull { progress[it.id]?.completed != true }?.id
        return course.sections.flatMap { section ->
            section.units.map { unit ->
                val planned = unit.nodes.mapIndexed { i, node ->
                    val p = progress[node.id]
                    val state = when {
                        node.id == currentId -> NodeState.Current
                        p?.legendary == true -> NodeState.Legendary
                        p?.completed == true -> NodeState.Completed
                        else -> NodeState.Locked
                    }
                    PlannedNode(node, state, snake[i % snake.size], i)
                }
                val done = planned.count { it.state == NodeState.Completed || it.state == NodeState.Legendary }
                PlannedUnit(
                    unit = unit,
                    section = section,
                    nodes = planned,
                    completed = done == planned.size,
                    locked = planned.isNotEmpty() && planned.all { it.state == NodeState.Locked },
                    progress = if (planned.isEmpty()) 1f else done.toFloat() / planned.size,
                )
            }
        }
    }

    fun currentNode(units: List<PlannedUnit>): PlannedNode? =
        units.asSequence().flatMap { it.nodes.asSequence() }.firstOrNull { it.state == NodeState.Current }

    /** Nodes to mark completed when the learner passes the jump test of [unitId]. */
    fun nodesCompletedBySkip(course: Course, unitId: String): List<String> {
        val ids = mutableListOf<String>()
        for (s in course.sections) for (u in s.units) {
            ids += u.nodes.map { it.id }
            if (u.id == unitId) return ids
        }
        return ids
    }

    /** Whether a node can be opened (current, completed or legendary). Chests open once. */
    fun canOpen(state: NodeState, type: NodeType): Boolean = when (state) {
        NodeState.Locked -> false
        NodeState.Current -> true
        NodeState.Completed, NodeState.Legendary -> type != NodeType.Chest
    }

    /** Legendary is available for completed lessons and stories. */
    fun canGoLegendary(state: NodeState, type: NodeType) =
        state == NodeState.Completed && (type == NodeType.Lesson || type == NodeType.Story)
}
