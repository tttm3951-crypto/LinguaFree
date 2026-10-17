package org.linguafree.core.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.linguafree.core.data.migration.LegacyMigration
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.ContentStatus
import org.linguafree.core.domain.usecase.DailyMaintenanceUseCase

/** Runs before the first screen: content import, 0.9 migration, daily maintenance. */
class AppBootstrapper(
    private val content: ContentRepository,
    private val migration: LegacyMigration,
    private val maintenance: DailyMaintenanceUseCase,
) {
    private val lock = Mutex()
    @Volatile var lastReport: DailyMaintenanceUseCase.Report? = null
        private set

    suspend fun start(): ContentStatus = lock.withLock {
        val status = content.ensureContent()
        if (status is ContentStatus.Ready) {
            runCatching { migration.runOnce() }
            lastReport = runCatching { maintenance() }.getOrNull()
        }
        status
    }

    /** Called when the app returns to foreground (e.g. after midnight). */
    suspend fun refresh() = lock.withLock { lastReport = runCatching { maintenance() }.getOrNull() }
}
