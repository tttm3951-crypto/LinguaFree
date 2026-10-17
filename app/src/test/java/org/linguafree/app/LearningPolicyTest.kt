package org.linguafree.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LearningPolicyTest {
    private val today=LocalDate.of(2026,10,17)

    @Test fun equalNamesDoNotMergeIdentifiedLearners(){
        assertNotEquals(LearningPolicy.reportKey("11111111-1111-1111-1111-111111111111","Саша"),LearningPolicy.reportKey("22222222-2222-2222-2222-222222222222","Саша"))
        assertEquals(LearningPolicy.reportKey("11111111-1111-1111-1111-111111111111","Саша"),LearningPolicy.reportKey("11111111-1111-1111-1111-111111111111","Новое имя"))
        assertNotEquals(LearningPolicy.reportKey("","Саша"),LearningPolicy.reportKey("11111111-1111-1111-1111-111111111111","Саша"))
    }

    @Test fun twoRemindersSkipTheAfternoonSlot(){
        assertEquals(listOf(9,19),LearningPolicy.reminderHours(2))
        assertFalse(LearningPolicy.shouldRemind(2,1,today,"",0,5))
        assertTrue(LearningPolicy.shouldRemind(2,2,today,"",0,5))
    }
    @Test fun remindersStopOnlyAfterTodaysGoal(){
        assertTrue(LearningPolicy.shouldRemind(3,0,today,today.toString(),1,5))
        assertFalse(LearningPolicy.shouldRemind(3,0,today,today.toString(),5,5))
        assertTrue(LearningPolicy.shouldRemind(3,0,today,today.minusDays(1).toString(),5,5))
    }
    @Test fun disabledAndInvalidModesNeverNotify(){
        assertFalse(LearningPolicy.shouldRemind(0,0,today,"",0,5))
        assertFalse(LearningPolicy.shouldRemind(99,0,today,"",0,5))
        assertFalse(LearningPolicy.shouldRemind(3,99,today,"",0,5))
    }
    @Test fun streakSurvivesUntilTheEndOfToday(){
        assertEquals(2,LearningPolicy.streak(setOf("2026-10-15","2026-10-16"),today))
        assertEquals(3,LearningPolicy.streak(setOf("2026-10-15","2026-10-16","2026-10-17"),today))
    }
    @Test fun gapsAndFutureDatesCannotExtendTheStreak(){
        assertEquals(0,LearningPolicy.streak(setOf("2026-10-15","2026-10-18"),today))
        assertEquals(1,LearningPolicy.streak(setOf("2026-10-14","2026-10-16"),today))
        assertEquals(0,LearningPolicy.streak(emptySet(),today))
    }
}
