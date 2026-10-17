package org.linguafree.app

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import java.util.Locale

private data class Quiz(val prompt:String,val choices:List<String>,val answer:Int,val explanation:String,val speech:String)
private data class Lesson(val title:String,val questions:List<Quiz>)

class MainActivity:ComponentActivity(),TextToSpeech.OnInitListener {
 private var tts:TextToSpeech?=null
 override fun onInit(status:Int){if(status==TextToSpeech.SUCCESS) tts?.language=Locale.US}
 override fun onDestroy(){tts?.stop();tts?.shutdown();super.onDestroy()}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  tts=TextToSpeech(this,this)
  val raw=JSONArray(assets.open("lessons.json").bufferedReader().use{it.readText()})
  val lessons=(0 until raw.length()).map{n->
   val u=raw.getJSONObject(n);val qs=u.getJSONArray("questions")
   Lesson(u.getString("title"),(0 until qs.length()).map{i->
    val q=qs.getJSONObject(i);val a=q.getJSONArray("choices")
    Quiz(q.getString("prompt"),(0 until a.length()).map{a.getString(it)},q.getInt("answer"),q.getString("explanation"),q.getString("speech"))
   })
  }
  val prefs=getSharedPreferences("linguafree_progress",Activity.MODE_PRIVATE)
  setContent {
   var current by remember { mutableIntStateOf(prefs.getInt("lesson",0).coerceIn(0,lessons.lastIndex)) }
   var index by remember { mutableIntStateOf(0) }
   var xp by remember { mutableIntStateOf(prefs.getInt("xp",0)) }
   var gems by remember { mutableIntStateOf(prefs.getInt("gems",0)) }
   var completed by remember { mutableStateOf(prefs.getString("completed","")!!.split(",").mapNotNull{it.toIntOrNull()}.toSet()) }
   var mistakes by remember { mutableStateOf(prefs.getString("mistakes","")!!.split(",").mapNotNull{it.toIntOrNull()}.toSet()) }
   var selected by remember { mutableStateOf<Int?>(null) }
   var screen by remember { mutableStateOf("home") }
   var correctCount by remember { mutableIntStateOf(0) }
   fun save(){prefs.edit().putInt("xp",xp).putInt("gems",gems).putInt("lesson",current).putString("completed",completed.joinToString(",")).putString("mistakes",mistakes.joinToString(",")).apply()}
   fun start(n:Int){current=n;index=0;selected=null;correctCount=0;screen="lesson";save()}
   MaterialTheme(colorScheme=lightColorScheme(primary=androidx.compose.ui.graphics.Color(0xFF48A52C),secondary=androidx.compose.ui.graphics.Color(0xFF1674CC))) {
    Surface(Modifier.fillMaxSize()) {
     LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
      item { Text("LinguaFree 🌱",style=MaterialTheme.typography.headlineMedium) }
      item { Text("Без рекламы · Без подписок · Без лимитов",style=MaterialTheme.typography.bodySmall) }
      item { Card(shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
       Text("⭐ $xp XP       💎 $gems",style=MaterialTheme.typography.titleLarge)
       Text("Пройдено уроков: ${completed.size}/${lessons.size}",style=MaterialTheme.typography.bodyMedium)
      } } }
      if(screen=="home") {
       item { Text("Путь обучения",style=MaterialTheme.typography.titleLarge) }
       lessons.forEachIndexed { n,l -> item {
        val done=n in completed
        FilledTonalButton(onClick={start(n)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)) {
         Text("${if(done) "✓" else "●"}  ${n+1}. ${l.title} · ${l.questions.size} заданий")
        }
       } }
       item { OutlinedButton(onClick={screen="stats"},modifier=Modifier.fillMaxWidth()){Text("📊 Статистика и ошибки")} }
      } else if(screen=="stats") {
       item { Text("Статистика",style=MaterialTheme.typography.headlineSmall) }
       item { Text("Опыт: $xp XP · Алмазы: $gems · Уроков: ${completed.size} · Ошибок: ${mistakes.size}") }
       item { Text("Для повторения ошибок открой соответствующий урок и пройди его снова.") }
       item { Button(onClick={screen="home"},modifier=Modifier.fillMaxWidth()){Text("К урокам")} }
      } else if(screen=="done") {
       item { Text("🎉 Урок пройден!",style=MaterialTheme.typography.headlineMedium) }
       item { Text("Верных ответов: $correctCount из ${lessons[current].questions.size}") }
       item { Button(onClick={start((current+1).coerceAtMost(lessons.lastIndex))},modifier=Modifier.fillMaxWidth()){Text("Следующий урок")} }
       item { OutlinedButton(onClick={start(current)},modifier=Modifier.fillMaxWidth()){Text("Повторить урок")} }
       item { TextButton(onClick={screen="home"}){Text("К карте уроков")} }
      } else {
       val l=lessons[current];val q=l.questions[index];val key=current*1000+index
       item { Text(l.title,style=MaterialTheme.typography.titleLarge) }
       item { LinearProgressIndicator(progress={(index+1).toFloat()/l.questions.size},modifier=Modifier.fillMaxWidth()) }
       item { Text("Вопрос ${index+1} из ${l.questions.size}") }
       item { Text(q.prompt,style=MaterialTheme.typography.headlineSmall) }
       item { OutlinedButton(onClick={tts?.speak(q.speech,TextToSpeech.QUEUE_FLUSH,null,"linguafree")}){Text("🔊 Прослушать английский")} }
       q.choices.forEachIndexed { i,choice -> item {
        OutlinedButton(onClick={selected=i},enabled=selected==null,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Text(choice)}
       } }
       if(selected!=null) {
        item { Text(if(selected==q.answer) "✅ Верно!" else "Правильный ответ: ${q.choices[q.answer]}",style=MaterialTheme.typography.titleMedium) }
        item { Text(q.explanation) }
        item { Button(onClick={
         if(selected==q.answer){xp+=10;gems+=2;correctCount++;mistakes=mistakes-key}else{mistakes=mistakes+key}
         selected=null
         if(index==l.questions.lastIndex){completed=completed+current;screen="done"}else index++
         save()
        },modifier=Modifier.fillMaxWidth()){Text("Продолжить →")} }
       }
       item { TextButton(onClick={screen="home"}){Text("Выйти к урокам")} }
      }
     }
    }
   }
  }
 }
}
