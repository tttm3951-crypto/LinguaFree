package org.linguafree.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CourseEntity::class, SectionEntity::class, UnitEntity::class, LessonEntity::class, ExerciseEntity::class,
        LexemeEntity::class, UserProgressEntity::class, LessonResultEntity::class, MistakeEntity::class,
        StreakDayEntity::class, StreakEntity::class, WalletEntity::class, QuestEntity::class,
        AchievementEntity::class, LeagueEntryEntity::class, LeagueStateEntity::class,
        InventoryEntity::class, CounterEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LinguaFreeDatabase : RoomDatabase() {
    abstract fun content(): ContentDao
    abstract fun progress(): ProgressDao
    abstract fun game(): GameDao

    companion object {
        const val NAME = "linguafree.db"
    }
}
