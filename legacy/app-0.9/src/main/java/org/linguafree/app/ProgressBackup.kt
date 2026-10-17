package org.linguafree.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.Reader

fun Reader.readBounded(maxChars:Int):String{
    val result=StringBuilder();val buffer=CharArray(4096)
    while(true){
        val count=read(buffer,0,minOf(buffer.size,maxChars+1-result.length))
        if(count<0)return result.toString()
        result.append(buffer,0,count)
        require(result.length<=maxChars){"Слишком большой файл"}
    }
}

object ProgressBackup {
    private val intKeys=setOf("xp","gems","lesson","cardIndex","dailyGoal","dailyStreak","foxAvatar","goalCount","placementScore","reminders","speedBest")
    private val booleanKeys=setOf("onboarded","effectsEnabled","hapticsEnabled","showWelcome")
    private val setKeys=setOf("cardMastered","dictionaryFavorites","grammarMastered","beginnerMastered")
    private val stringKeys=setOf("completed","goalDate","learnerName","learnerId","level","cardDue","mistakes","repeatQueue","reviewDue","reviewStages","rewardedStories","skillCorrect","skillTotal","studiedDates","grammarBest","grammarLevel","beginnerBest","beginnerLevel")

    fun restore(prefs:SharedPreferences,json:JSONObject):Boolean{
        require(json.optString("format")=="LinguaFreeProgress"&&json.optInt("version")==1){"Неверный формат резервной копии"}
        val values=json.getJSONObject("preferences")
        val keys=values.keys().asSequence().toList()
        // Validate everything before writing. An invalid backup must never partially alter progress.
        for(key in keys){
            require(key.length<=120&&key.all{it.isLetterOrDigit()||it=='_'||it=='-'}){"Недопустимое имя параметра"}
            val value=values.get(key)
            when(key){
                in intKeys->require(value is Int&&value>=0){"Некорректное число: $key"}
                in booleanKeys->require(value is Boolean){"Некорректный переключатель: $key"}
                in setKeys->require(value is JSONArray&&(0 until value.length()).all{value.get(it) is String}){"Некорректный список: $key"}
                in stringKeys->require(value is String){"Некорректная строка: $key"}
                "voiceSpeed"->require(value is Number&&value.toDouble() in 0.5..2.0){"Некорректная скорость речи"}
                else->{
                    val previous=prefs.all[key]
                    require(previous==null||
                        (previous is String&&value is String)||(previous is Boolean&&value is Boolean)||
                        (previous is Int&&value is Int)||(previous is Long&&value is Number)||
                        (previous is Float&&value is Number)||(previous is Set<*>&&value is JSONArray)){"Тип параметра изменён: $key"}
                    require(value is String||value is Boolean||value is Number||value is JSONArray){"Неподдерживаемый параметр"}
                    if(value is JSONArray)require((0 until value.length()).all{value.get(it) is String})
                }
            }
            if(key=="lesson")require((value as Int) in 0..299)
            if(key=="foxAvatar")require((value as Int) in 0..6)
            if(key=="reminders")require(value in listOf(0,2,3))
        }
        val editor=prefs.edit()
        for(key in keys){
         val value=values.get(key)
         if(key=="voiceSpeed"){editor.putFloat(key,(value as Number).toFloat());continue}
         when(value){
            is String->editor.putString(key,value)
            is Boolean->editor.putBoolean(key,value)
            is Int->if(key !in intKeys&&prefs.all[key] is Long)editor.putLong(key,value.toLong()) else editor.putInt(key,value)
            is Long->editor.putLong(key,value)
            is Double->if(prefs.all[key] is Long)editor.putLong(key,value.toLong()) else editor.putFloat(key,value.toFloat())
            is JSONArray->editor.putStringSet(key,(0 until value.length()).map{value.getString(it)}.toSet())
         }
        }
        // Overlay keeps settings introduced after an older backup was created.
        return editor.commit()
    }
}
