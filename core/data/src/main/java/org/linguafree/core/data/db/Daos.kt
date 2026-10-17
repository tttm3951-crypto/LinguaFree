package org.linguafree.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM course LIMIT 1")
    suspend fun course(): CourseEntity?

    @Query("SELECT * FROM sections ORDER BY ord")
    suspend fun sections(): List<SectionEntity>

    @Query("SELECT * FROM units ORDER BY ord")
    suspend fun units(): List<UnitEntity>

    @Query("SELECT * FROM lessons ORDER BY ord")
    suspend fun lessons(): List<LessonEntity>

    @Query("SELECT * FROM exercises WHERE lessonId = :lessonId ORDER BY ord")
    suspend fun exercises(lessonId: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises ORDER BY lessonId, ord")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM lexemes")
    suspend fun lexemes(): List<LexemeEntity>

    @Query("SELECT * FROM lexemes")
    fun lexemesFlow(): Flow<List<LexemeEntity>>

    @Query("DELETE FROM course") suspend fun clearCourse()
    @Query("DELETE FROM sections") suspend fun clearSections()
    @Query("DELETE FROM units") suspend fun clearUnits()
    @Query("DELETE FROM lessons") suspend fun clearLessons()
    @Query("DELETE FROM exercises") suspend fun clearExercises()

    @Insert suspend fun insertCourse(course: CourseEntity)
    @Insert suspend fun insertSections(items: List<SectionEntity>)
    @Insert suspend fun insertUnits(items: List<UnitEntity>)
    @Insert suspend fun insertLessons(items: List<LessonEntity>)
    @Insert suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLexemesIgnore(items: List<LexemeEntity>)

    @Query("UPDATE lexemes SET word = :word, translation = :translation, image = :image WHERE id = :id")
    suspend fun updateLexemeContent(id: String, word: String, translation: String, image: String?)

    @Upsert suspend fun upsertLexemes(items: List<LexemeEntity>)

    @Query("UPDATE lexemes SET strength = :strength, lastSeen = :lastSeen, halfLife = :halfLife, rightCount = :right, errorCount = :error WHERE id = :id")
    suspend fun updateMemory(id: String, strength: Double, lastSeen: Long, halfLife: Double, right: Int, error: Int)

    /** Replaces all course content in one transaction; learner memory in `lexemes` is kept. */
    @Transaction
    suspend fun replaceContent(
        course: CourseEntity,
        sections: List<SectionEntity>,
        units: List<UnitEntity>,
        lessons: List<LessonEntity>,
        exercises: List<ExerciseEntity>,
        lexemes: List<LexemeEntity>,
    ) {
        clearExercises(); clearLessons(); clearUnits(); clearSections(); clearCourse()
        insertCourse(course)
        insertSections(sections)
        insertUnits(units)
        insertLessons(lessons)
        exercises.chunked(500).forEach { insertExercises(it) }
        insertLexemesIgnore(lexemes)
        lexemes.forEach { updateLexemeContent(it.id, it.word, it.translation, it.image) }
    }
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM user_progress")
    fun progressFlow(): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress")
    suspend fun progress(): List<UserProgressEntity>

    @Query("SELECT * FROM user_progress WHERE lessonId = :id")
    suspend fun progressOf(id: String): UserProgressEntity?

    @Upsert suspend fun upsertProgress(items: List<UserProgressEntity>)

    @Insert suspend fun insertResult(result: LessonResultEntity): Long

    @Query("SELECT * FROM lesson_results ORDER BY finishedAt")
    fun resultsFlow(): Flow<List<LessonResultEntity>>

    @Query("SELECT * FROM lesson_results ORDER BY finishedAt")
    suspend fun results(): List<LessonResultEntity>

    @Query("SELECT * FROM lesson_results WHERE finishedAt >= :from AND finishedAt < :to ORDER BY finishedAt")
    suspend fun resultsBetween(from: Long, to: Long): List<LessonResultEntity>

    @Upsert suspend fun upsertMistakes(items: List<MistakeEntity>)

    @Query("DELETE FROM mistakes WHERE exerciseId IN (:ids)")
    suspend fun deleteMistakes(ids: List<String>)

    @Query("SELECT exerciseId FROM mistakes ORDER BY at DESC LIMIT :limit")
    suspend fun mistakeIds(limit: Int): List<String>

    @Query("SELECT COUNT(*) FROM mistakes")
    fun mistakesCount(): Flow<Int>

    @Query("SELECT * FROM mistakes")
    suspend fun mistakes(): List<MistakeEntity>

    @Query("DELETE FROM user_progress") suspend fun clearProgress()
    @Query("DELETE FROM lesson_results") suspend fun clearResults()
    @Query("DELETE FROM mistakes") suspend fun clearMistakes()
    @Query("UPDATE lexemes SET strength = 0, lastSeen = 0, halfLife = 0, rightCount = 0, errorCount = 0")
    suspend fun clearMemory()
    @Insert suspend fun insertResults(items: List<LessonResultEntity>)
}

@Dao
interface GameDao {
    @Query("SELECT * FROM wallet WHERE id = 0")
    fun walletFlow(): Flow<WalletEntity?>

    @Query("SELECT * FROM wallet WHERE id = 0")
    suspend fun wallet(): WalletEntity?

    @Upsert suspend fun upsertWallet(wallet: WalletEntity)

    @Query("SELECT * FROM streak_days")
    fun streakDaysFlow(): Flow<List<StreakDayEntity>>

    @Query("SELECT * FROM streak_days")
    suspend fun streakDays(): List<StreakDayEntity>

    @Query("SELECT * FROM streak WHERE id = 0")
    fun streakFlow(): Flow<StreakEntity?>

    @Query("SELECT * FROM streak WHERE id = 0")
    suspend fun streak(): StreakEntity?

    @Upsert suspend fun upsertStreak(streak: StreakEntity)
    @Upsert suspend fun upsertStreakDays(days: List<StreakDayEntity>)

    @Query("SELECT * FROM quests")
    fun questsFlow(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests")
    suspend fun quests(): List<QuestEntity>

    @Query("DELETE FROM quests") suspend fun clearQuests()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertQuests(items: List<QuestEntity>)

    @Transaction
    suspend fun replaceQuests(items: List<QuestEntity>) { clearQuests(); insertQuests(items) }

    @Query("SELECT * FROM achievements")
    fun achievementsFlow(): Flow<List<AchievementEntity>>

    @Upsert suspend fun upsertAchievement(a: AchievementEntity)

    @Query("SELECT * FROM league_state WHERE id = 0")
    fun leagueStateFlow(): Flow<LeagueStateEntity?>

    @Query("SELECT * FROM league_state WHERE id = 0")
    suspend fun leagueState(): LeagueStateEntity?

    @Upsert suspend fun upsertLeagueState(s: LeagueStateEntity)
    @Upsert suspend fun upsertLeagueEntry(e: LeagueEntryEntity)

    @Query("SELECT * FROM league_entries ORDER BY weekKey DESC")
    fun leagueHistory(): Flow<List<LeagueEntryEntity>>

    @Query("SELECT item FROM inventory")
    fun inventoryFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addInventory(item: InventoryEntity)

    @Query("SELECT value FROM counters WHERE `key` = :key")
    suspend fun counter(key: String): Int?

    @Upsert suspend fun upsertCounter(c: CounterEntity)

    @Query("SELECT * FROM counters")
    fun countersFlow(): Flow<List<CounterEntity>>

    @Query("SELECT * FROM counters") suspend fun counters(): List<CounterEntity>
    @Query("SELECT * FROM achievements") suspend fun achievements(): List<AchievementEntity>
    @Query("SELECT * FROM inventory") suspend fun inventory(): List<InventoryEntity>
    @Query("SELECT * FROM league_entries") suspend fun leagueEntries(): List<LeagueEntryEntity>

    @Query("DELETE FROM wallet") suspend fun clearWallet()
    @Query("DELETE FROM streak_days") suspend fun clearStreakDays()
    @Query("DELETE FROM streak") suspend fun clearStreak()
    @Query("DELETE FROM achievements") suspend fun clearAchievements()
    @Query("DELETE FROM league_state") suspend fun clearLeagueState()
    @Query("DELETE FROM league_entries") suspend fun clearLeagueEntries()
    @Query("DELETE FROM inventory") suspend fun clearInventory()
    @Query("DELETE FROM counters") suspend fun clearCounters()
}
