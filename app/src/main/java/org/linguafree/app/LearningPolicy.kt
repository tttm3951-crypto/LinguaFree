package org.linguafree.app

import java.time.LocalDate

object LearningPolicy {
    fun reportKey(id:String,name:String):String = if(id.matches(Regex("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")))"id:"+id.lowercase() else "legacy:"+name.trim()
    fun reminderHours(count:Int):List<Int> = when(count){2->listOf(9,19);3->listOf(9,14,19);else->emptyList()}

    fun shouldRemind(count:Int,slot:Int,today:LocalDate,goalDate:String,goalCount:Int,dailyGoal:Int):Boolean {
        val hour=listOf(9,14,19).getOrNull(slot)?:return false
        if(hour !in reminderHours(count))return false
        return goalDate!=today.toString() || goalCount<dailyGoal.coerceAtLeast(1)
    }

    fun streak(dates:Set<String>,today:LocalDate=LocalDate.now()):Int {
        var day=if(today.toString() in dates)today else today.minusDays(1)
        var count=0
        while(day.toString() in dates){count++;day=day.minusDays(1)}
        return count
    }
}
