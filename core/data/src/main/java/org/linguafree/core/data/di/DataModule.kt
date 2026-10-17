package org.linguafree.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.linguafree.core.data.AppBootstrapper
import org.linguafree.core.data.backup.BackupManager
import org.linguafree.core.data.backup.TeacherTools
import org.linguafree.core.data.content.ContentLoader
import org.linguafree.core.data.db.LinguaFreeDatabase
import org.linguafree.core.data.migration.LegacyMigration
import org.linguafree.core.data.platform.Feedback
import org.linguafree.core.data.platform.SpeechInput
import org.linguafree.core.data.platform.SpeechOutput
import org.linguafree.core.data.repository.ContentRepositoryImpl
import org.linguafree.core.data.repository.GameRepositoryImpl
import org.linguafree.core.data.repository.ProgressRepositoryImpl
import org.linguafree.core.data.settings.SettingsRepositoryImpl
import org.linguafree.core.data.settings.settingsStore
import org.linguafree.core.domain.repository.ContentRepository
import org.linguafree.core.domain.repository.GameRepository
import org.linguafree.core.domain.repository.ProgressRepository
import org.linguafree.core.domain.repository.SettingsRepository
import org.linguafree.core.domain.usecase.DailyMaintenanceUseCase
import org.linguafree.core.domain.usecase.FinishSessionUseCase
import org.linguafree.core.domain.usecase.OpenChestUseCase
import org.linguafree.core.domain.usecase.ShopUseCase
import org.linguafree.core.domain.usecase.StartSessionUseCase
import org.linguafree.core.domain.usecase.SystemTime
import org.linguafree.core.domain.usecase.TimeProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): LinguaFreeDatabase =
        Room.databaseBuilder(context, LinguaFreeDatabase::class.java, LinguaFreeDatabase.NAME).build()

    @Provides @Singleton
    fun time(): TimeProvider = SystemTime

    @Provides @Singleton
    fun contentLoader(@ApplicationContext context: Context, db: LinguaFreeDatabase) = ContentLoader(context, db.content())

    @Provides @Singleton
    fun contentRepository(loader: ContentLoader, db: LinguaFreeDatabase): ContentRepository = ContentRepositoryImpl(loader, db.content())

    @Provides @Singleton
    fun progressRepository(db: LinguaFreeDatabase): ProgressRepository = ProgressRepositoryImpl(db.progress(), db.content())

    @Provides @Singleton
    fun gameRepositoryImpl(db: LinguaFreeDatabase): GameRepositoryImpl = GameRepositoryImpl(db.game())

    @Provides @Singleton
    fun gameRepository(impl: GameRepositoryImpl): GameRepository = impl

    @Provides @Singleton
    fun settingsImpl(@ApplicationContext context: Context) = SettingsRepositoryImpl(context.settingsStore)

    @Provides @Singleton
    fun settingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

    @Provides
    fun startSession(c: ContentRepository, p: ProgressRepository, g: GameRepository, s: SettingsRepository, t: TimeProvider) =
        StartSessionUseCase(c, p, g, s, t)

    @Provides
    fun finishSession(c: ContentRepository, p: ProgressRepository, g: GameRepository, s: SettingsRepository, t: TimeProvider) =
        FinishSessionUseCase(c, p, g, s, t)

    @Provides
    fun dailyMaintenance(p: ProgressRepository, g: GameRepository, s: SettingsRepository, t: TimeProvider) =
        DailyMaintenanceUseCase(p, g, s, t)

    @Provides
    fun shop(g: GameRepository, s: SettingsRepository, t: TimeProvider) = ShopUseCase(g, s, t)

    @Provides
    fun openChest(p: ProgressRepository, g: GameRepository) = OpenChestUseCase(p, g)

    @Provides @Singleton
    fun legacyMigration(
        @ApplicationContext context: Context, c: ContentRepository, p: ProgressRepository, g: GameRepository, s: SettingsRepositoryImpl,
    ) = LegacyMigration(context, c, p, g, s)

    @Provides @Singleton
    fun speechOutput(@ApplicationContext context: Context) = SpeechOutput(context)

    @Provides @Singleton
    fun speechInput(@ApplicationContext context: Context) = SpeechInput(context)

    @Provides @Singleton
    fun feedback(@ApplicationContext context: Context) = Feedback(context)

    @Provides @Singleton
    fun backupManager(@ApplicationContext context: Context, db: LinguaFreeDatabase, s: SettingsRepositoryImpl, m: LegacyMigration) =
        BackupManager(context, db, s, m)

    @Provides @Singleton
    fun teacherTools(@ApplicationContext context: Context, db: LinguaFreeDatabase, s: SettingsRepositoryImpl) = TeacherTools(context, db, s)

    @Provides @Singleton
    fun bootstrapper(c: ContentRepository, m: LegacyMigration, d: DailyMaintenanceUseCase) = AppBootstrapper(c, m, d)

    @Provides @Singleton
    fun library(@ApplicationContext context: Context) = org.linguafree.core.data.library.LibraryRepository(context)
}
