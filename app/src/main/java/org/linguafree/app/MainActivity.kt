package org.linguafree.app

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.os.Handler
import android.os.Looper
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import kotlin.math.sin
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.BitmapFactory
import android.graphics.Bitmap
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
 private var reactionNumber=0
 private val uiHandler=Handler(Looper.getMainLooper())
 private var introPlayed=false
 private val askNotifications=registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
 private fun playWelcomeMelody(){
  if(introPlayed)return
  introPlayed=true
  Thread {
   try {
    val rate=22050
    val notes=listOf(523.25,659.25,783.99,1046.5,783.99,880.0,1046.5)
    val durations=listOf(140,140,140,250,130,130,340)
    val total=durations.sum()*rate/1000
    val samples=ShortArray(total)
    var offset=0
    for(j in notes.indices){
     val count=durations[j]*rate/1000
     for(i in 0 until count){
      val envelope=minOf(1.0,i/250.0,(count-i)/500.0).coerceAtLeast(0.0)
      val wave=sin(2.0*Math.PI*notes[j]*i/rate)+0.20*sin(4.0*Math.PI*notes[j]*i/rate)
      samples[offset+i]=(wave*envelope*7000).toInt().toShort()
     }
     offset+=count
    }
    val track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(samples.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
    track.write(samples,0,samples.size)
    track.play()
    Thread.sleep(durations.sum().toLong()+120)
    track.stop();track.release()
   }catch(_:Exception){}
  }.start()
 }
 private fun sayEnglish(value:String,flush:Boolean=true){
  if(!speechReady)return
  tts?.speak(value,if(flush)TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,null,"english-"+System.nanoTime())
 }
 private fun playCorrectFeedback(){
  try {
   val vibrator=getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
   if(Build.VERSION.SDK_INT>=26) vibrator?.vibrate(VibrationEffect.createOneShot(65,VibrationEffect.DEFAULT_AMPLITUDE))
   else @Suppress("DEPRECATION") vibrator?.vibrate(65)
  }catch(_:Exception){}
  Thread {
   try {
    val rate=22050
    val notes=doubleArrayOf(523.25,783.99)
    val durations=intArrayOf(105,190)
    val samples=ShortArray(durations.sum()*rate/1000)
    var offset=0
    for(n in notes.indices){
     val count=durations[n]*rate/1000
     for(i in 0 until count){
      val envelope=minOf(1.0,i/180.0,(count-i)/550.0).coerceAtLeast(0.0)
      val tone=sin(2.0*Math.PI*notes[n]*i/rate)+0.18*sin(4.0*Math.PI*notes[n]*i/rate)
      samples[offset+i]=(tone*envelope*5500).toInt().toShort()
     }
     offset+=count
    }
    val track=AudioTrack.Builder()
     .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
     .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
     .setBufferSizeInBytes(samples.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
    track.write(samples,0,samples.size);track.play()
    Thread.sleep(durations.sum().toLong()+80);track.stop();track.release()
   }catch(_:Exception){}
  }.start()
 }
 private fun playMilestoneSound(){
  Thread {
   try {
    val rate=22050;val notes=doubleArrayOf(659.25,783.99,987.77,1174.66)
    val duration=110;val samples=ShortArray(notes.size*duration*rate/1000)
    for(n in notes.indices){val count=duration*rate/1000;for(i in 0 until count){val fade=minOf(1.0,i/180.0,(count-i)/450.0).coerceAtLeast(0.0);samples[n*count+i]=(sin(2*Math.PI*notes[n]*i/rate)*fade*6500).toInt().toShort()}}
    val track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(samples.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
    track.write(samples,0,samples.size);track.play();Thread.sleep((notes.size*duration+80).toLong());track.stop();track.release()
   }catch(_:Exception){}
  }.start()
 }
 private fun sayReaction(correct:Boolean){
  if(correct)playCorrectFeedback()
  if(!speechReady)return
  val phrases=if(correct) listOf("Great!","Well done!","Excellent!","Fantastic!","Amazing!","You did it!")
   else listOf("Try again!","Keep going!","You can do it!","Nice try!")
  val phrase=phrases[reactionNumber%phrases.size]
  reactionNumber++
  sayEnglish(phrase,false)
 }
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
  playWelcomeMelody()
  tts=TextToSpeech(this,this)
  val raw=JSONArray(assets.open("lessons.json").bufferedReader().use{it.readText()})
  val baseLessons=(0 until raw.length()).map{n->
   val u=raw.getJSONObject(n);val qs=u.getJSONArray("questions")
   Lesson(u.getString("title"),(0 until qs.length()).map{i->
    val q=qs.getJSONObject(i);val a=q.getJSONArray("choices")
    Quiz(q.getString("prompt"),(0 until a.length()).map{a.getString(it)},q.getInt("answer"),q.getString("explanation"),q.getString("speech"))
   })
  }
  val lessons=(0 until 300).map { n -> val original=baseLessons[n%baseLessons.size]; original.copy(title=if(n<baseLessons.size)original.title else "${original.title} · этап ${n/baseLessons.size+1}") }
  val prefs=getSharedPreferences("linguafree_progress",Activity.MODE_PRIVATE)
  ReminderReceiver.schedule(this)
  setContent {
   var current by remember { mutableIntStateOf(prefs.getInt("lesson",0).coerceIn(0,lessons.lastIndex)) }
   var index by remember { mutableIntStateOf(0) }
   var xp by remember { mutableIntStateOf(prefs.getInt("xp",0)) }
   var gems by remember { mutableIntStateOf(prefs.getInt("gems",0)) }
   var completed by remember { mutableStateOf(prefs.getString("completed","")!!.split(",").mapNotNull{it.toIntOrNull()}.toSet()) }
   var mistakes by remember { mutableStateOf(prefs.getString("mistakes","")!!.split(",").mapNotNull{it.toIntOrNull()}.toSet()) }
   var repeatQueue by remember { mutableStateOf(prefs.getString("repeatQueue","")!!.split(",").mapNotNull{it.toIntOrNull()}.toSet()) }
   var selected by remember { mutableStateOf<Int?>(null) }
   var screen by remember { mutableStateOf(if(prefs.getBoolean("onboarded",false))"home" else "onboarding") }
   var onboardStep by remember { mutableIntStateOf(0) }
   var level by remember { mutableStateOf("Начинающий") }
   var dailyGoal by remember { mutableIntStateOf(10) }
   var streak by remember { mutableIntStateOf(prefs.getInt("dailyStreak",0)) }
   var answerCombo by remember { mutableIntStateOf(0) }
   var reminderCount by remember { mutableIntStateOf(prefs.getInt("reminders",3)) }
   var studiedDates by remember { mutableStateOf(prefs.getString("studiedDates","")!!.split(",").filter{it.isNotBlank()}.toSet()) }
   fun markStudyDay(){
    val today=LocalDate.now();val key=today.toString()
    if(key !in studiedDates){
     streak=if(today.minusDays(1).toString() in studiedDates)streak+1 else 1
     studiedDates=studiedDates+key
     prefs.edit().putInt("dailyStreak",streak).putString("studiedDates",studiedDates.joinToString(",")).apply()
    }
   }
   var bankWords by remember { mutableStateOf(listOf<String>()) }
   var typedAnswer by remember { mutableStateOf("") }
   var hintShown by remember { mutableStateOf(false) }
   var showTranslation by remember { mutableStateOf(false) }
   var placementScore by remember { mutableIntStateOf(0) }
   val placementLessons=listOf(0,2,4,6,8,12,16,25,40,55,75,100,135,185,245)
   fun activeQuestion():Quiz=if(screen=="placement")lessons[placementLessons[index]].questions[index%7] else lessons[current].questions[index]
   var reviewStages by remember { mutableStateOf(prefs.getString("reviewStages","")!!.split(";").mapNotNull { entry -> val p=entry.split(":");if(p.size==2) p[0].toIntOrNull()?.let { key -> p[1].toIntOrNull()?.let { key to it } } else null }.toMap()) }
   var reviewDue by remember { mutableStateOf(prefs.getString("reviewDue","")!!.split(";").mapNotNull { entry -> val p=entry.split(":");if(p.size==2) p[0].toIntOrNull()?.let { key -> p[1].toLongOrNull()?.let { key to it } } else null }.toMap()) }
   fun recordRecall(key:Int,correct:Boolean){
    val stage=if(correct)((reviewStages[key]?:0)+1).coerceAtMost(5) else 0
    reviewStages=reviewStages+(key to stage)
    reviewDue=reviewDue+(key to (System.currentTimeMillis()+listOf(0L,1L,3L,7L,14L,30L)[stage]*86400000L))
    prefs.edit().putString("reviewStages",reviewStages.entries.joinToString(";"){"${it.key}:${it.value}"}).putString("reviewDue",reviewDue.entries.joinToString(";"){"${it.key}:${it.value}"}).apply()
   }

   var correctCount by remember { mutableIntStateOf(0) }
   var foxMessage by remember { mutableStateOf("Давай попробуем!") }
   var foxExcited by remember { mutableStateOf(false) }
   var celebration by remember { mutableStateOf(false) }
   var reviewLeft by remember { mutableStateOf<String?>(null) }
   var reviewRight by remember { mutableStateOf<String?>(null) }
   var reviewMatched by remember { mutableStateOf(setOf<String>()) }
   val reviewPairs=lessons[current].questions.map { q ->
    val quoted=q.prompt.substringAfter("«","").substringBefore("»","")
    if(q.prompt.startsWith("Как по-английски"))q.choices[q.answer] to quoted
    else quoted to q.choices[q.answer]
   }.filter { it.first.isNotBlank() && it.second.isNotBlank() }.distinctBy { it.first }.take(5)
   fun checkPair(left:String?,right:String?){
    if(left!=null && right!=null){
     if(reviewPairs.any{it.first==left && it.second==right}){
      reviewMatched=reviewMatched+left
      sayEnglish(left)
      if(reviewMatched.size==reviewPairs.size){foxExcited=true;playMilestoneSound();sayEnglish("Ta-da! Amazing! You did it!",false)}
     }else{sayEnglish("Try again!")}
     reviewLeft=null;reviewRight=null
    }
   }
   val foxScale by animateFloatAsState(targetValue=if(foxExcited)1.14f else 1f,animationSpec=spring(dampingRatio=Spring.DampingRatioMediumBouncy,stiffness=Spring.StiffnessLow),label="foxReaction")
   fun save(){prefs.edit().putString("repeatQueue",repeatQueue.joinToString(",")).putInt("xp",xp).putInt("gems",gems).putInt("lesson",current).putString("completed",completed.joinToString(",")).putString("mistakes",mistakes.joinToString(",")).apply()}
   fun start(n:Int){current=n;index=0;selected=null;bankWords=emptyList();typedAnswer="";hintShown=false;showTranslation=false;correctCount=0;answerCombo=0;foxMessage="Давай попробуем!";foxExcited=false;screen="lesson";save()}
   LaunchedEffect(screen) { if(screen=="review"){reviewMatched=emptySet();reviewLeft=null;reviewRight=null;sayEnglish("Match the words!")} }
   LaunchedEffect(screen) { if(screen=="done"){ foxExcited=true;celebration=true;playMilestoneSound();sayEnglish("Hooray! Fantastic! You did it!"); kotlinx.coroutines.delay(1800); foxExcited=false;celebration=false } }
   LaunchedEffect(screen,current,index) {
    if(screen=="lesson" || screen=="placement") {
     kotlinx.coroutines.delay(380)
     repeat(15) {
      if(speechReady)return@repeat
      kotlinx.coroutines.delay(160)
     }
     val question=activeQuestion()
     if(!question.prompt.startsWith("Как по-английски"))sayEnglish(question.speech)
    }
   }
   val purple=Color(0xFFAF80FF)
   val pink=Color(0xFFFF7AA9)
   val teal=Color(0xFF21B8D1)
   val green=Color(0xFF91D72E)
   val ink=Color(0xFFF7F8FA)
   val dark=Color(0xFF102124)
   val panel=Color(0xFF1E3338)
   val compact=LocalConfiguration.current.screenHeightDp<780
   val foxTransparent=remember { val source=BitmapFactory.decodeResource(resources,R.drawable.fox_original); val bitmap=source.copy(Bitmap.Config.ARGB_8888,true); val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height); for(i in pixels.indices){val c=pixels[i];val rr=android.graphics.Color.red(c);val gg=android.graphics.Color.green(c);val bb=android.graphics.Color.blue(c);if(rr>235 && gg>235 && bb>235){pixels[i]=c and 0x00FFFFFF}else if(rr>215 && gg>215 && bb>215 && kotlin.math.abs(rr-gg)<15 && kotlin.math.abs(gg-bb)<15){pixels[i]=(c and 0x00FFFFFF) or ((255-(minOf(rr,gg,bb)-215)*6).coerceIn(0,255) shl 24)}};bitmap.setPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height);bitmap.asImageBitmap() }
   val floating=rememberInfiniteTransition(label="mascot")
   val bounce by floating.animateFloat(initialValue=0f,targetValue=-8f,animationSpec=infiniteRepeatable(tween(1300),RepeatMode.Reverse),label="bounce")
   val foxTilt by animateFloatAsState(targetValue=if(foxExcited)12f else 0f,animationSpec=spring(dampingRatio=Spring.DampingRatioMediumBouncy),label="foxTilt")
   val foxWiggle by floating.animateFloat(initialValue=-2f,targetValue=2f,animationSpec=infiniteRepeatable(tween(750),RepeatMode.Reverse),label="foxWiggle")
   MaterialTheme(colorScheme=darkColorScheme(primary=purple,secondary=pink,background=dark,surface=panel,onSurface=ink)) {
    Surface(Modifier.fillMaxSize(),color=dark) {
     Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
     LazyColumn(modifier=Modifier.weight(1f),contentPadding=PaddingValues(start=14.dp,end=14.dp,top=if(screen=="lesson" || screen=="placement") 10.dp else 27.dp,bottom=8.dp),verticalArrangement=Arrangement.spacedBy(if(screen=="lesson" || screen=="placement") 5.dp else 12.dp)) {
      if(screen=="onboarding") {
       item { LinearProgressIndicator(progress={(onboardStep+1)/3f},modifier=Modifier.fillMaxWidth().height(10.dp),color=green) }
       item { Row(verticalAlignment=Alignment.CenterVertically) { Image(bitmap=foxTransparent,contentDescription="Лисичка",modifier=Modifier.size(95.dp)); Text(when(onboardStep){0->"Привет! Я твоя лисичка! 🦊";1->"Как хорошо ты знаешь английский?";else->"Сколько минут будем играть каждый день?"},fontSize=22.sp,fontWeight=FontWeight.Black,color=ink,lineHeight=28.sp) } }
       if(onboardStep==0) item { Text("Учимся весело, бесплатно и без рекламы! 🌈",fontSize=23.sp,fontWeight=FontWeight.Bold,color=pink) }
       if(onboardStep==1) { item { Text("Выбери свой уровень",fontSize=21.sp,fontWeight=FontWeight.Black,color=ink) }; listOf("Начинающий","Знаю несколько слов","Могу говорить").forEach { option -> item { OutlinedButton(onClick={level=option},modifier=Modifier.fillMaxWidth().height(70.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(level==option)Color(0xFF365B4D) else panel),border=BorderStroke(2.dp,if(level==option)green else Color(0xFF3B5057))) {Text(option,fontSize=19.sp,fontWeight=FontWeight.Bold,color=ink)} } } }
       if(onboardStep==2) { listOf(3,10,15,30).forEach { minutes -> item { OutlinedButton(onClick={dailyGoal=minutes},modifier=Modifier.fillMaxWidth().height(65.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(dailyGoal==minutes)Color(0xFF365B4D) else panel),border=BorderStroke(2.dp,if(dailyGoal==minutes)green else Color(0xFF3B5057))) {Text("$minutes минут в день",fontSize=19.sp,fontWeight=FontWeight.Bold,color=ink)} } } }
      }
      if(screen=="placement") item { Text("🎯 Короткая проверка знаний · ${index+1}/${placementLessons.size}",fontSize=24.sp,fontWeight=FontWeight.Black,color=ink) }
      if(screen=="home") item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
       Column { Text("LinguaFree",fontSize=35.sp,fontWeight=FontWeight.Black,color=purple)
        Text("ИГРАЙ • УЧИСЬ • ПОБЕЖДАЙ",fontSize=12.sp,fontWeight=FontWeight.ExtraBold,color=pink) }
       Image(bitmap=foxTransparent,contentDescription="Весёлая лисичка LinguaFree",modifier=Modifier.size(92.dp).offset(y=bounce.dp))
      } }
      
      if(screen=="home") item {
       Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
         Text("🔥 ${if(studiedDates.contains(LocalDate.now().toString()) || studiedDates.contains(LocalDate.now().minusDays(1).toString()))streak else 0} дней подряд",fontSize=24.sp,fontWeight=FontWeight.Black,color=Color(0xFFFFAC35))
         Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
          val today=LocalDate.now()
          (6 downTo 0).forEach { offset ->
           val day=today.minusDays(offset.toLong())
           val active=day.toString() in studiedDates
           Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Text(listOf("Вс","Пн","Вт","Ср","Чт","Пт","Сб")[day.dayOfWeek.value%7],fontSize=13.sp,color=ink)
            Text(if(active)"🟠" else "⚪",fontSize=22.sp)
           }
          }
         }
         Text("🔔 Напоминания: $reminderCount раза в день",fontSize=16.sp,color=ink)
         Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
          listOf(0,2,3).forEach { n ->
           Button(onClick={
            reminderCount=n;prefs.edit().putInt("reminders",n).apply();ReminderReceiver.schedule(this@MainActivity)
            if(n>0 && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
           },colors=ButtonDefaults.buttonColors(containerColor=if(reminderCount==n)green else Color(0xFF40565D)),contentPadding=PaddingValues(horizontal=12.dp)) {Text(if(n==0)"Выкл." else "$n раза",color=dark)}
          }
         }
        }
       }
      }
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
        Card(onClick={start(n)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=panel),elevation=CardDefaults.cardElevation(defaultElevation=3.dp)) {
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
       item { Text("Слов для повторения: ${repeatQueue.size}. Ошибки сохраняются, чтобы можно было вернуться к сложным заданиям.") }
       item { Text("🧠 По расписанию на сегодня: ${reviewDue.values.count{it<=System.currentTimeMillis()}}. Интервалы 1, 3, 7, 14 и 30 дней.",fontSize=16.sp,color=ink) }
       if(repeatQueue.isNotEmpty() || reviewDue.values.any{it<=System.currentTimeMillis()}) item { Button(onClick={val key=reviewDue.filterValues{it<=System.currentTimeMillis()}.keys.firstOrNull()?:repeatQueue.firstOrNull()?:0;start((key/1000).coerceIn(0,lessons.lastIndex));index=(key%1000).coerceIn(0,lessons[current].questions.lastIndex)},modifier=Modifier.fillMaxWidth()){Text("🧠 Повторить сложные слова")} }
       item { Button(onClick={screen="home"},modifier=Modifier.fillMaxWidth()){Text("К урокам")} }
      } else if(screen=="review") {
       item { Text("🧠 Закрепляем слова",fontSize=29.sp,fontWeight=FontWeight.Black,color=purple) }
       item { Text("Нажимай на пары: английское слово и его перевод",fontSize=17.sp,color=ink) }
       item { LinearProgressIndicator(progress={reviewMatched.size.toFloat()/reviewPairs.size.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(9.dp)),color=green) }
       item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
         reviewPairs.forEach { (en,_) ->
          val matched=en in reviewMatched
          OutlinedButton(onClick={reviewLeft=en;sayEnglish(en);checkPair(reviewLeft,reviewRight)},enabled=!matched,modifier=Modifier.fillMaxWidth().height(74.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(matched)Color(0xFF284F39) else if(reviewLeft==en)Color(0xFF3A4866) else panel),border=BorderStroke(2.dp,if(matched)green else if(reviewLeft==en)purple else Color(0xFF40565D))) { Text(if(matched)"✓ $en" else en,fontSize=18.sp,fontWeight=FontWeight.Bold,color=ink) }
         }
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
         reviewPairs.map{it.second}.reversed().forEach { ru ->
          val matched=reviewPairs.any{it.second==ru && it.first in reviewMatched}
          OutlinedButton(onClick={reviewRight=ru;checkPair(reviewLeft,reviewRight)},enabled=!matched,modifier=Modifier.fillMaxWidth().height(74.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(matched)Color(0xFF284F39) else if(reviewRight==ru)Color(0xFF3A4866) else panel),border=BorderStroke(2.dp,if(matched)green else if(reviewRight==ru)purple else Color(0xFF40565D))) { Text(if(matched)"✓ $ru" else ru,fontSize=17.sp,fontWeight=FontWeight.Bold,color=ink) }
         }
        }
       } }
       if(reviewMatched.size==reviewPairs.size) item {
        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
         Image(bitmap=foxTransparent,contentDescription="Лисичка радуется",modifier=Modifier.size(110.dp).offset(y=bounce.dp).graphicsLayer{scaleX=foxScale;scaleY=foxScale;rotationZ=foxTilt+foxWiggle})
         Text("🎊 ТАДА-А-АМ! Все пары найдены!",fontSize=21.sp,fontWeight=FontWeight.Black,color=green)
         Text("Ты отлично закрепил слова! ⭐",fontSize=17.sp,color=ink)
        }
       }
      } else if(screen=="done") {
       item { Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()) {
        Image(bitmap=foxTransparent,contentDescription="Лисичка празднует победу",modifier=Modifier.size(138.dp).offset(y=bounce.dp).graphicsLayer { rotationZ=bounce*2.5f+foxTilt; scaleX=foxScale;scaleY=foxScale })
        Text("🎉 УРА! УРОК ПРОЙДЕН!",fontSize=30.sp,fontWeight=FontWeight.Black,color=pink)
        Text(if(celebration)"🎊 ✨ 🌟 🎉 ✨ 🎊" else "🦊 Hooray! Fantastic! 🌟",fontSize=23.sp,fontWeight=FontWeight.Black,color=green)
       } }
       item { Text("Верных ответов: $correctCount из ${lessons[current].questions.size}") }
       item { Button(onClick={start((current+1).coerceAtMost(lessons.lastIndex))},modifier=Modifier.fillMaxWidth()){Text("Следующий урок")} }
       item { OutlinedButton(onClick={start(current)},modifier=Modifier.fillMaxWidth()){Text("Повторить урок")} }
       item { TextButton(onClick={screen="home"}){Text("К карте уроков")} }
      } else {
       val l=lessons[current];val q=activeQuestion();val key=current*1000+index
       val mastery=reviewStages[key]?:0
       val wordBank=screen=="lesson" && index%7==2 && q.choices[q.answer].contains(" ") && mastery<2
       val typedMode=screen=="lesson" && q.prompt.startsWith("Как по-английски") && (mastery>=2 || index%7==4 || index%7==5 || index%7==6)
       val audioMode=screen=="lesson" && index%7==1 && mastery>=1 && !q.prompt.startsWith("Как по-английски")
       val pictureMode=screen=="lesson" && index%7==0 && q.choices.all { it.lowercase(Locale.ROOT) in setOf("tea","coffee","milk","apple","cat","dog","water","book","house","hello") }
       item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text("🎯 "+l.title,fontSize=19.sp,fontWeight=FontWeight.Black,color=purple,modifier=Modifier.weight(1f),maxLines=1)
        Text("⚡ $answerCombo  ⭐ ${index+1}/${l.questions.size}",fontSize=15.sp,fontWeight=FontWeight.Black,color=pink)
       } }
       item { Text("Уровень слова: ${when(mastery){0->"Изучаем";1->"Закрепляем";2->"Вспоминаем";else->"Уверенно знаем"}}",fontSize=12.sp,color=teal) }
       item { LinearProgressIndicator(progress={(index+1).toFloat()/l.questions.size},modifier=Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(9.dp)),color=green,trackColor=Color(0xFFE7DDF9)) }

       item { Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF3B2A45))) { Column(Modifier.padding(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
         Image(bitmap=foxTransparent,contentDescription=null,modifier=Modifier.size(24.dp).offset(y=bounce.dp).graphicsLayer { scaleX=foxScale;scaleY=foxScale;rotationZ=foxTilt+foxWiggle })
         Text(foxMessage,fontSize=15.sp,fontWeight=FontWeight.Black,color=pink)
        }
        Spacer(Modifier.height(4.dp))
        Text(if(audioMode)"🎧 Прослушай слово и выбери перевод" else q.prompt,fontSize=23.sp,fontWeight=FontWeight.Black,color=ink,lineHeight=28.sp)
       } } }
       if(selected!=null) item { Text(q.speech,modifier=Modifier.clickable {sayEnglish(q.speech)},fontSize=19.sp,fontWeight=FontWeight.Black,color=purple) }
       if(selected!=null && showTranslation) item { Text(q.explanation,fontSize=14.sp,color=ink) }
       if(selected!=null || !q.prompt.startsWith("Как по-английски")) item { Button(onClick={sayEnglish(q.speech)},modifier=Modifier.fillMaxWidth().height(43.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=teal)){Text("🔊 СЛУШАТЬ",fontSize=15.sp,fontWeight=FontWeight.Black)} }
       if(typedMode) item {
        Text(if(index%7==6)"🔤 Восстанови слово по памяти" else "✍️ Напиши английский ответ",fontSize=17.sp,fontWeight=FontWeight.Bold,color=ink)
        OutlinedTextField(value=typedAnswer,onValueChange={typedAnswer=it},label={Text("Твой ответ")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=selected==null)
        if(selected==null) TextButton(onClick={hintShown=true}){Text(if(hintShown)"Подсказка: первая буква — ${q.choices[q.answer].firstOrNull()?:'?'}" else "💡 Нужна подсказка?")}
        Button(onClick={
         val right=typedAnswer.trim().equals(q.choices[q.answer],ignoreCase=true)
         selected=if(right)q.answer else (q.answer+1)%q.choices.size
         foxExcited=right;foxMessage=if(right)when(answerCombo+1){3->"🔥 Три подряд!";5->"🌟 Пять подряд!";10->"🏆 Десять подряд!";else->"Отлично! 🌟"} else "Попробуй ещё! 💪"
         sayEnglish(q.speech);sayReaction(right)
        },enabled=selected==null && typedAnswer.isNotBlank(),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=green)){Text("ПРОВЕРИТЬ",fontWeight=FontWeight.Black)}
       }
       if(wordBank) item {
        Text("Составьте перевод из слов:",fontSize=19.sp,fontWeight=FontWeight.Bold,color=ink)
        Text(bankWords.joinToString("  ").ifBlank { "Нажимайте на слова ниже ↓" },modifier=Modifier.fillMaxWidth().padding(vertical=12.dp),fontSize=23.sp,fontWeight=FontWeight.Bold,color=green)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
         (q.choices[q.answer].split(" ").reversed()+listOf("the")).distinct().forEach { word ->
          OutlinedButton(onClick={bankWords=bankWords+word;sayEnglish(word)},enabled=selected==null && word !in bankWords,contentPadding=PaddingValues(8.dp)) { Text(word,fontSize=16.sp) }
         }
        }
        TextButton(onClick={bankWords=emptyList()},enabled=selected==null){Text("Очистить")}
        Button(onClick={val right=bankWords.joinToString(" ").equals(q.choices[q.answer],ignoreCase=true);selected=if(right)q.answer else (q.answer+1)%q.choices.size;foxExcited=right;foxMessage=if(right)"Отлично! 🦊" else "Потренируемся ещё!";sayEnglish(q.speech);sayReaction(right)},enabled=selected==null && bankWords.isNotEmpty(),modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=green)){Text("ПРОВЕРИТЬ")}
       }
       if(!wordBank && !typedMode) q.choices.forEachIndexed { i,choice -> item {
        val right=selected!=null && i==q.answer
        val chosen=selected==i
        val bg=if(right)Color(0xFFD8F8E7) else if(chosen)Color(0xFFFFE0EA) else panel
        OutlinedButton(onClick={
         selected=i
         foxExcited=i==q.answer
         foxMessage=if(i==q.answer) when(answerCombo+1){3->"🔥 Три подряд! Great!";5->"🌟 Пять подряд! Excellent!";10->"🏆 Десять подряд! Amazing!";else->listOf("Great! 🌟","Well done! 🎉","Excellent! 🏆","Amazing! ✨")[index%4]} else "Try again! 💪"
         sayEnglish(q.choices[q.answer])
         sayReaction(i==q.answer)
        },enabled=selected==null,modifier=Modifier.fillMaxWidth().height(if(pictureMode) 68.dp else 54.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=bg,disabledContainerColor=bg,contentColor=ink,disabledContentColor=ink),border=BorderStroke(3.dp,if(right)green else if(chosen)pink else Color(0xFF40565D))) {
         Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(1.dp)) {
          if(pictureMode) Text(when { choice.contains("Tea",true)->"🍵";choice.contains("Coffee",true)->"☕";choice.contains("Milk",true)->"🥛";choice.contains("Apple",true)->"🍎";choice.contains("Cat",true)->"🐈";choice.contains("Dog",true)->"🐕";choice.contains("Water",true)->"💧";choice.contains("Book",true)->"📚";choice.contains("House",true)->"🏠";choice.contains("Hello",true)->"👋";else->"🗂️" },fontSize=if(compact) 21.sp else 24.sp)
          Text(choice,fontSize=if(compact) 16.sp else 18.sp,fontWeight=FontWeight.ExtraBold,lineHeight=21.sp,maxLines=2)
         }
        }
       } }
       if(selected!=null) {
        if(selected==q.answer && (answerCombo+1)==5) item { Text("🔥 Пять ответов подряд! Бонус после продолжения",fontSize=15.sp,fontWeight=FontWeight.Black,color=green) }
        item { Text(q.explanation,fontSize=14.sp,fontWeight=FontWeight.Bold,color=ink,maxLines=2) }
        /* fixed bottom action handles advancing */
       /* item { Button(onClick={
         if(selected==q.answer){xp+=10;gems+=2;correctCount++;mistakes=mistakes-key}else{mistakes=mistakes+key}
         selected=null
         foxMessage="Давай попробуем!"
         foxExcited=false
         if(index==l.questions.lastIndex){completed=completed+current;screen="done"}else index++
         save()
        },modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=green)){Text("ПРОДОЛЖИТЬ 🚀",fontSize=17.sp,fontWeight=FontWeight.Black)} } */
       }
       if(selected==null) item { TextButton(onClick={screen="home"},modifier=Modifier.height(30.dp)){Text("← К урокам",fontSize=13.sp)} }
      }
     }
     if(screen=="review" && reviewMatched.size==reviewPairs.size) {
      Button(onClick={screen="done";foxExcited=true},modifier=Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,top=4.dp,bottom=14.dp).height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=green,contentColor=dark),shape=RoundedCornerShape(17.dp)) {Text("ЗАБРАТЬ ОПЫТ ⭐",fontSize=21.sp,fontWeight=FontWeight.Black)}
     }
     if(screen=="onboarding" || ((screen=="lesson" || screen=="placement") && selected!=null)) {
      Button(onClick={
       if(screen=="onboarding") {
        if(onboardStep<2)onboardStep++ else {prefs.edit().putBoolean("onboarded",true).putString("level",level).putInt("dailyGoal",dailyGoal).apply();if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);if(level=="Начинающий")screen="home" else {current=0;index=0;placementScore=0;screen="placement"}}
       } else {
        val q=activeQuestion()
        val key=current*1000+index
        if(selected==q.answer){xp+=10;gems+=2;correctCount++;answerCombo++;mistakes=mistakes-key}else{answerCombo=0;mistakes=mistakes+key}
        if(screen=="lesson")recordRecall(key,selected==q.answer)
        if(screen=="placement" && selected==q.answer)placementScore++
        selected=null;bankWords=emptyList();typedAnswer="";hintShown=false;showTranslation=false;foxMessage="Давай попробуем!";foxExcited=false
        if(screen=="placement") {if(index==placementLessons.lastIndex){current=when{placementScore>=13->100;placementScore>=11->65;placementScore>=8->35;placementScore>=5->15;placementScore>=3->5;else->0};prefs.edit().putInt("placementScore",placementScore).apply();index=0;screen="home";save()}else index++}
        else if(index==lessons[current].questions.lastIndex){completed=completed+current;markStudyDay();screen="review"}else index++
        save()
       }
      },modifier=Modifier.fillMaxWidth().padding(start=16.dp,end=16.dp,top=4.dp,bottom=14.dp).height(54.dp),shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(containerColor=green,contentColor=dark)) {
       Text(if(screen=="onboarding")if(onboardStep==2)"ПОЕХАЛИ! 🚀" else "ПРОДОЛЖИТЬ" else "ПРОДОЛЖИТЬ 🚀",fontSize=21.sp,fontWeight=FontWeight.Black)
      }
     }
     }
    }
   }
  }
 }
}
