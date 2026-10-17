package org.linguafree.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.linguafree.core.data.db.ContentDao
import org.linguafree.core.data.db.LessonResultEntity
import org.linguafree.core.data.db.LexemeEntity
import org.linguafree.core.data.db.MistakeEntity
import org.linguafree.core.data.db.ProgressDao
import org.linguafree.core.data.db.UserProgressEntity
import org.linguafree.core.domain.learning.Srs
import org.linguafree.core.domain.model.LessonResult
import org.linguafree.core.domain.model.LexemeState
import org.linguafree.core.domain.model.NodeProgress
import org.linguafree.core.domain.model.SessionKind
import org.linguafree.core.domain.repository.ProgressRepository

class ProgressRepositoryImpl(
    private val dao: ProgressDao,
    private val contentDao: ContentDao,
) : ProgressRepository {

    override fun nodeProgress(): Flow<Map<String, NodeProgress>> =
        dao.progressFlow().map { list -> list.associate { it.lessonId to it.toDomain() } }

    override suspend fun nodeProgressNow(): Map<String, NodeProgress> =
        dao.progress().associate { it.lessonId to it.toDomain() }

    override suspend fun completeNodes(nodeIds: List<String>, accuracy: Int?, legendary: Boolean) {
        if (nodeIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val existing = dao.progress().associateBy { it.lessonId }
        dao.upsertProgress(nodeIds.distinct().map { id ->
            val old = existing[id]
            UserProgressEntity(
                lessonId = id,
                completed = true,
                legendary = legendary || old?.legendary == true,
                timesCompleted = (old?.timesCompleted ?: 0) + 1,
                bestAccuracy = maxOf(old?.bestAccuracy ?: 0, accuracy ?: 0),
                lastAccuracy = accuracy ?: old?.lastAccuracy ?: 100,
                completedAt = now,
            )
        })
    }

    override suspend fun addResult(result: LessonResult) {
        dao.insertResult(result.toEntity())
    }

    override fun results(): Flow<List<LessonResult>> = dao.resultsFlow().map { l -> l.map { it.toDomain() } }

    override suspend fun resultsBetween(fromMs: Long, toMs: Long): List<LessonResult> =
        dao.resultsBetween(fromMs, toMs).map { it.toDomain() }

    override suspend fun memory(): Map<String, Srs.Memory> =
        contentDao.lexemes().filter { it.lastSeen > 0 }.associate { it.id to it.memory() }

    override suspend fun updateMemory(results: List<Pair<String, Boolean>>, now: Long) {
        if (results.isEmpty()) return
        val byId = contentDao.lexemes().associateBy { it.id }
        val updated = HashMap<String, LexemeEntity>()
        for ((id, correct) in results) {
            val cur = updated[id] ?: byId[id] ?: continue
            val m = Srs.update(if (cur.lastSeen > 0) cur.memory() else null, correct, now)
            updated[id] = cur.copy(
                rightCount = m.right, errorCount = m.wrong, halfLife = m.halfLifeDays, lastSeen = m.lastSeen,
                strength = Srs.strength(m, now),
            )
        }
        contentDao.upsertLexemes(updated.values.toList())
    }

    override fun lexemeStates(now: Long): Flow<List<LexemeState>> = contentDao.lexemesFlow().map { list ->
        list.filter { it.lastSeen > 0 }.map {
            LexemeState(it.id, it.word, it.translation, it.rightCount, it.errorCount, it.halfLife, it.lastSeen, Srs.strength(it.memory(), now))
        }
    }

    override suspend fun addMistakes(exerciseIds: Collection<String>, now: Long) {
        if (exerciseIds.isEmpty()) return
        // Practice copies ("id~listen") point back to the original exercise.
        dao.upsertMistakes(exerciseIds.map { MistakeEntity(it.substringBefore('~'), now) }.distinctBy { it.exerciseId })
    }

    override suspend fun removeMistakes(exerciseIds: Collection<String>) {
        if (exerciseIds.isEmpty()) return
        dao.deleteMistakes(exerciseIds.map { it.substringBefore('~') }.distinct())
    }

    override suspend fun mistakeIds(limit: Int): List<String> = dao.mistakeIds(limit)

    override fun mistakesCount(): Flow<Int> = dao.mistakesCount()
}

internal fun LexemeEntity.memory() = Srs.Memory(rightCount, errorCount, if (halfLife > 0) halfLife else Srs.halfLife(rightCount, errorCount), lastSeen)

internal fun UserProgressEntity.toDomain() = NodeProgress(lessonId, completed, legendary, timesCompleted, bestAccuracy, lastAccuracy)

internal fun LessonResult.toEntity() = LessonResultEntity(
    lessonId = nodeId, kind = kind.key, xp = xp, accuracy = accuracy, durationMs = durationMs, mistakes = mistakes,
    correct = correct, total = total, bestCombo = bestCombo, listeningDone = listeningDone, speakingDone = speakingDone,
    finishedAt = finishedAt, passed = passed,
)

internal fun LessonResultEntity.toDomain() = LessonResult(
    nodeId = lessonId, kind = SessionKind.of(kind), xp = xp, accuracy = accuracy, durationMs = durationMs,
    mistakes = mistakes, correct = correct, total = total, bestCombo = bestCombo, listeningDone = listeningDone,
    speakingDone = speakingDone, finishedAt = finishedAt, passed = passed,
)
