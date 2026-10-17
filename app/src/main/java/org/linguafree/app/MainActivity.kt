package org.linguafree.app

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
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
 private var speechReady=false
 override fun onInit(status:Int){
  if(status!=TextToSpeech.SUCCESS)return
  val engine=tts?:return
  val result=engine.setLanguage(Locale.US)
  if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED)return
  val englishVoices=engine.voices.orEmpty().filter{
   it.locale.language=="en" && !it.isNetworkConnectionRequired
  }
  val female=englishVoices.filter{
   val name=it.name.lowercase(Locale.ROOT)
   name.contains("female")||name.contains("woman")||name.contains("feminine")||
   name.contains("en-us-x-sfg")||name.contains("en-us-x-tpf")
  }
  val preferred=female.firstOrNull{it.locale.country=="US"}?:female.firstOrNull()
  if(preferred!=null)engine.voice=preferred
  engine.setSpeechRate(0.85f)
  speechReady=true
 }
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
   val purple=Color(0xFF7444D5)
   val pink=Color(0xFFFF5398)
   val teal=Color(0xFF23B9BA)
   val green=Color(0xFF26AD73)
   val ink=Color(0xFF29213F)
   val compact=LocalConfiguration.current.screenHeightDp<780
   val floating=rememberInfiniteTransition(label="mascot")
   val bounce by floating.animateFloat(initialValue=0f,targetValue=-8f,animationSpec=infiniteRepeatable(tween(1300),RepeatMode.Reverse),label="bounce")
   MaterialTheme(colorScheme=lightColorScheme(primary=purple,secondary=pink,background=Color(0xFFFFF7EF),surface=Color.White,onSurface=ink)) {
    Surface(Modifier.fillMaxSize(),color=Color(0xFFFFF7EF)) {
     LazyColumn(contentPadding=PaddingValues(start=14.dp,end=14.dp,top=8.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(if(screen=="lesson") 7.dp else 12.dp)) {
      if(screen=="home") item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
       Column { Text("LinguaFree",fontSize=35.sp,fontWeight=FontWeight.Black,color=purple)
        Text("ИГРАЙ • УЧИСЬ • ПОБЕЖДАЙ",fontSize=12.sp,fontWeight=FontWeight.ExtraBold,color=pink) }
       Image(painter=painterResource(R.drawable.fox_mascot),contentDescription="Весёлая лисичка LinguaFree",modifier=Modifier.size(92.dp).offset(y=bounce.dp))
      } }
      
      if(screen=="home") item { Card(shape=RoundedCornerShape(26.dp),modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=purple)) { Column(Modifier.padding(21.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
       Text("⭐ $xp XP       💎 $gems",fontSize=24.sp,fontWeight=FontWeight.Black,color=Color.White)
       Text("🏆 Пройдено уроков: ${completed.size}/${lessons.size}",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color.White)
       LinearProgressIndicator(progress={completed.size.toFloat()/lessons.size},modifier=Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(9.dp)),color=Color(0xFFFFC74A),trackColor=Color.White.copy(alpha=.3f))
       Text("🎁 Без рекламы и подписок!",fontSize=13.sp,fontWeight=FontWeight.Bold,color=Color.White)
      } } }
      if(screen=="home") {
       item { Text("🚀 Путь приключений",fontSize=26.sp,fontWeight=FontWeight.Black,color=ink) }
       itemsIndexed(lessons) { n,l ->
        val done=n in completed
        val accent=listOf(pink,teal,green,purple,Color(0xFFFF9A35))[n%5]
        Card(onClick={start(n)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(defaultElevation=3.dp)) {
         Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
          Box(Modifier.size(56.dp).clip(RoundedCornerShape(17.dp)).background(accent.copy(alpha=.14f)),contentAlignment=Alignment.Center) { Text(if(done)"🏆" else listOf("🌈","🎯","🎨","🎵","🚀")[n%5],fontSize=29.sp) }
          Column(Modifier.weight(1f)) {
           Text("УРОК ${n+1}",fontSize=12.sp,fontWeight=FontWeight.Black,color=accent)
           Text(l.title,fontSize=18.sp,fontWeight=FontWeight.ExtraBold,color=ink,lineHeight=23.sp)
           Text(if(done)"✓ Пройден!" else "⚡ ${l.questions.size} заданий",fontSize=13.sp,fontWeight=FontWeight.Bold,color=if(done)green else Color.Gray)
          }
          Text("➜",fontSize=24.sp,color=accent,fontWeight=FontWeight.Black)
         }
        }
       }
       item { OutlinedButton(onClick={screen="stats"},modifier=Modifier.fillMaxWidth()){Text("📊 Статистика и ошибки")} }
      } else if(screen=="stats") {
       item { Text("🏆 Мои достижения",fontSize=30.sp,fontWeight=FontWeight.Black,color=purple) }
       item { Text("Опыт: $xp XP · Алмазы: $gems · Уроков: ${completed.size} · Ошибок: ${mistakes.size}") }
       item { Text("Для повторения ошибок открой соответствующий урок и пройди его снова.") }
       item { Button(onClick={screen="home"},modifier=Modifier.fillMaxWidth()){Text("К урокам")} }
      } else if(screen=="done") {
       item { Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()) {
        Image(painter=painterResource(R.drawable.fox_mascot),contentDescription="Лисичка празднует победу",modifier=Modifier.size(160.dp).graphicsLayer { rotationZ=bounce*1.2f })
        Text("🎉 УРА! УРОК ПРОЙДЕН!",fontSize=30.sp,fontWeight=FontWeight.Black,color=pink)
       } }
       item { Text("Верных ответов: $correctCount из ${lessons[current].questions.size}") }
       item { Button(onClick={start((current+1).coerceAtMost(lessons.lastIndex))},modifier=Modifier.fillMaxWidth()){Text("Следующий урок")} }
       item { OutlinedButton(onClick={start(current)},modifier=Modifier.fillMaxWidth()){Text("Повторить урок")} }
       item { TextButton(onClick={screen="home"}){Text("К карте уроков")} }
      } else {
       val l=lessons[current];val q=l.questions[index];val key=current*1000+index
       item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text("🎯 "+l.title,fontSize=19.sp,fontWeight=FontWeight.Black,color=purple,modifier=Modifier.weight(1f),maxLines=1)
        Text("⭐ ${index+1}/${l.questions.size}",fontSize=15.sp,fontWeight=FontWeight.Black,color=pink)
       } }
       item { LinearProgressIndicator(progress={(index+1).toFloat()/l.questions.size},modifier=Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(9.dp)),color=green,trackColor=Color(0xFFE7DDF9)) }

       item { Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFFE5F0))) { Column(Modifier.padding(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
         Image(painter=painterResource(R.drawable.fox_mascot),contentDescription=null,modifier=Modifier.size(40.dp).offset(y=bounce.dp))
         Text("Давай попробуем!",fontSize=15.sp,fontWeight=FontWeight.Black,color=pink)
        }
        Spacer(Modifier.height(4.dp))
        Text(q.prompt,fontSize=23.sp,fontWeight=FontWeight.Black,color=ink,lineHeight=28.sp)
       } } }
       item { Button(onClick={if(speechReady)tts?.speak(q.speech,TextToSpeech.QUEUE_FLUSH,null,"linguafree")},modifier=Modifier.fillMaxWidth().height(46.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=teal)){Text("🔊 СЛУШАТЬ",fontSize=15.sp,fontWeight=FontWeight.Black)} }
       q.choices.forEachIndexed { i,choice -> item {
        val right=selected!=null && i==q.answer
        val chosen=selected==i
        val bg=if(right)Color(0xFFD8F8E7) else if(chosen)Color(0xFFFFE0EA) else Color.White
        OutlinedButton(onClick={selected=i},enabled=selected==null,modifier=Modifier.fillMaxWidth().height(55.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=bg,disabledContainerColor=bg,contentColor=ink,disabledContentColor=ink),border=BorderStroke(3.dp,if(right)green else if(chosen)pink else Color(0xFFE6DCF2))) {
         Text(choice,fontSize=19.sp,fontWeight=FontWeight.ExtraBold,lineHeight=23.sp)
        }
       } }
       if(selected!=null) {
        item { AnimatedVisibility(visible=true,enter=fadeIn()+expandVertically()) { Row(verticalAlignment=Alignment.CenterVertically) {
         Image(painter=painterResource(R.drawable.fox_mascot),contentDescription=null,modifier=Modifier.size(36.dp).offset(y=if(selected==q.answer)bounce.dp else 0.dp))
         Text(if(selected==q.answer) "🎉 СУПЕР! ПРАВИЛЬНО!" else "💡 Попробуем запомнить!",fontSize=17.sp,fontWeight=FontWeight.Black,color=if(selected==q.answer)green else pink)
        } } }
        item { Text(q.explanation,fontSize=14.sp,fontWeight=FontWeight.Bold,color=ink,maxLines=2) }
        item { Button(onClick={
         if(selected==q.answer){xp+=10;gems+=2;correctCount++;mistakes=mistakes-key}else{mistakes=mistakes+key}
         selected=null
         if(index==l.questions.lastIndex){completed=completed+current;screen="done"}else index++
         save()
        },modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=green)){Text("ПРОДОЛЖИТЬ 🚀",fontSize=17.sp,fontWeight=FontWeight.Black)} }
       }
       item { TextButton(onClick={screen="home"},modifier=Modifier.height(34.dp)){Text("← К урокам",fontSize=13.sp)} }
      }
     }
    }
   }
  }
 }
}
