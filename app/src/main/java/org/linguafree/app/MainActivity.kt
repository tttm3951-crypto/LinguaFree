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
import android.speech.RecognizerIntent
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
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
import org.json.JSONObject
import java.util.Locale

private data class Quiz(val prompt:String,val choices:List<String>,val answer:Int,val explanation:String,val speech:String)
private data class Lesson(val title:String,val questions:List<Quiz>)

class MainActivity:ComponentActivity(),TextToSpeech.OnInitListener {
 private var tts:TextToSpeech?=null
 private var speechReady=false
 private var selectedVoiceName:String?=null
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
 private fun sayEnglish(value:String,flush:Boolean=true,slow:Boolean=false){
  if(!speechReady)return
  val voicePrefs=getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
  tts?.setSpeechRate(if(slow)0.68f else voicePrefs.getFloat("voiceSpeed",0.93f))
  tts?.setPitch(if(slow)1.0f else 1.05f)
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
 private fun playWrongFeedback(){
  try {
   val vibrator=getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
   if(Build.VERSION.SDK_INT>=26)vibrator?.vibrate(VibrationEffect.createOneShot(35,70))
   else @Suppress("DEPRECATION") vibrator?.vibrate(35)
  }catch(_:Exception){}
  Thread {
   try {
    val rate=22050;val notes=doubleArrayOf(330.0,277.18);val duration=105
    val samples=ShortArray(notes.size*duration*rate/1000)
    for(n in notes.indices){val count=duration*rate/1000;for(i in 0 until count){val fade=minOf(1.0,i/180.0,(count-i)/450.0).coerceAtLeast(0.0);samples[n*count+i]=(sin(2*Math.PI*notes[n]*i/rate)*fade*2600).toInt().toShort()}}
    val track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(samples.size*2).setTransferMode(AudioTrack.MODE_STATIC).build()
    track.write(samples,0,samples.size);track.play();Thread.sleep((notes.size*duration+80).toLong());track.stop();track.release()
   }catch(_:Exception){}
  }.start()
 }
 private fun sayReaction(correct:Boolean){
  if(getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE).getBoolean("effectsEnabled",true)){if(correct)playCorrectFeedback() else playWrongFeedback()}
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
  val preferred=female.filter{it.locale.country=="US"}.maxByOrNull{it.quality}?:englishVoices.filter{it.locale.country=="US"}.maxByOrNull{it.quality}?:female.maxByOrNull{it.quality}?:englishVoices.maxByOrNull{it.quality}
  if(preferred!=null){engine.voice=preferred;selectedVoiceName=preferred.name}
  engine.setSpeechRate(getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE).getFloat("voiceSpeed",0.93f))
  engine.setPitch(1.05f)
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
  val lessons=baseLessons
  val prefs=getSharedPreferences("linguafree_progress",Activity.MODE_PRIVATE)
  ReminderReceiver.schedule(this)
  setContent {
   var backupStatus by remember { mutableStateOf("") }
   val exportProgress=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
    if(uri!=null)try{
     val data=JSONObject().put("format","LinguaFreeProgress").put("version",1)
     val values=JSONObject()
     prefs.all.forEach{(key,value)->
      when(value){
       is String->values.put(key,value)
       is Int->values.put(key,value)
       is Long->values.put(key,value)
       is Float->values.put(key,value.toDouble())
       is Boolean->values.put(key,value)
       is Set<*>->values.put(key,JSONArray(value.filterIsInstance<String>()))
      }
     }
     data.put("preferences",values)
     contentResolver.openOutputStream(uri)?.use{it.write(data.toString(2).toByteArray(Charsets.UTF_8))}
     backupStatus="Резервная копия сохранена."
    }catch(e:Exception){backupStatus="Ошибка экспорта: ${e.message}"}
   }
   val importProgress=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
    if(uri!=null)try{
     val json=JSONObject(contentResolver.openInputStream(uri)!!.bufferedReader().use{it.readText()})
     require(json.optString("format")=="LinguaFreeProgress" && json.optInt("version")==1){"Неверный формат резервной копии"}
     val values=json.getJSONObject("preferences")
     val editor=prefs.edit()
     val keys=values.keys()
     while(keys.hasNext()){
      val key=keys.next()
      when(val value=values.get(key)){
       is String->editor.putString(key,value)
       is Boolean->editor.putBoolean(key,value)
       is Int->editor.putInt(key,value)
       is Long->editor.putLong(key,value)
       is Double->editor.putFloat(key,value.toFloat())
       is JSONArray->editor.putStringSet(key,(0 until value.length()).map{value.getString(it)}.toSet())
      }
     }
     if(editor.commit()){ReminderReceiver.schedule(this@MainActivity);recreate()}
     else backupStatus="Не удалось восстановить прогресс."
    }catch(e:Exception){backupStatus="Ошибка восстановления: ${e.message}"}
   }
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
   var showHomeGreeting by remember { mutableStateOf(true) }
   var storyIndex by remember { mutableIntStateOf(0) }
   var storyStep by remember { mutableIntStateOf(0) }
   var storyFeedback by remember { mutableStateOf("") }
   var rewardedStories by remember { mutableStateOf(prefs.getString("rewardedStories","").orEmpty().split(",").mapNotNull{it.toIntOrNull()}.toSet()) }
   var speechTarget by remember { mutableStateOf("Hello") }
   var speechResult by remember { mutableStateOf("") }
   val storyData=remember {
    val json=JSONArray(assets.open("stories.json").bufferedReader().use{it.readText()})
    (0 until json.length()).map{n->
     val lines=json.getJSONObject(n).getJSONArray("lines")
     (0 until lines.length()).map{j->val line=lines.getJSONObject(j);Triple(line.getString("en"),line.getString("ru"),line.getString("ru"))}
    }
   }
   val stories=storyData
   val storyTitles=remember {
    val json=JSONArray(assets.open("stories.json").bufferedReader().use{it.readText()})
    (0 until json.length()).map{n->json.getJSONObject(n).getString("title")}
   }
   var storyQuery by remember { mutableStateOf("") }
   val dictionary=remember {
    val json=JSONArray(assets.open("dictionary.json").bufferedReader().use{it.readText()})
    (0 until json.length()).map{n->json.getJSONObject(n)}
   }
   val vocabulary=remember { dictionary.map{it.getString("en") to it.getString("ru")} }
   var vocabQuery by remember { mutableStateOf("") }
   var vocabLevel by remember { mutableStateOf("Все") }
   var vocabFavorites by remember { mutableStateOf(prefs.getStringSet("dictionaryFavorites",emptySet<String>())?.toSet()?:emptySet()) }
   var vocabOnlyFavorites by remember { mutableStateOf(false) }
   var cardIndex by remember { mutableIntStateOf(prefs.getInt("cardIndex",0)) }
   var cardRevealed by remember { mutableStateOf(false) }
   var cardMastered by remember { mutableStateOf(prefs.getStringSet("cardMastered",emptySet<String>())?.toSet()?:emptySet()) }
   var cardDue by remember {
    mutableStateOf(prefs.getString("cardDue","")!!.split(";").mapNotNull{entry->
     val parts=entry.split(":");if(parts.size==2)parts[0].toIntOrNull()?.let{key->parts[1].toLongOrNull()?.let{key to it}} else null
    }.toMap())
   }
   fun nextCard(remembered:Boolean){
    val n=cardIndex.coerceIn(0,dictionary.lastIndex)
    val due=System.currentTimeMillis()+(if(remembered)3L else 0L)*86400000L
    cardDue=cardDue+(n to due)
    if(remembered)cardMastered=cardMastered+dictionary[n].getString("en")
    else cardMastered=cardMastered-dictionary[n].getString("en")
    prefs.edit().putStringSet("cardMastered",cardMastered).putString("cardDue",cardDue.entries.joinToString(";"){"${it.key}:${it.value}"}).apply()
    val next=(1..dictionary.size).map{(n+it)%dictionary.size}.firstOrNull{cardDue[it]==null || cardDue[it]!!<=System.currentTimeMillis()}
    cardIndex=next?:((n+1)%dictionary.size)
    prefs.edit().putInt("cardIndex",cardIndex).apply()
    cardRevealed=false
   }

   var speedRound by remember { mutableIntStateOf(0) }
   var speedCorrect by remember { mutableIntStateOf(0) }
   var speedFinished by remember { mutableStateOf(false) }
   var speedStartedAt by remember { mutableLongStateOf(0L) }
   var speedRemaining by remember { mutableIntStateOf(60) }
   var speedBest by remember { mutableIntStateOf(prefs.getInt("speedBest",0)) }
   var skillCorrect by remember { mutableStateOf(prefs.getString("skillCorrect","").orEmpty().split(",").mapNotNull{it.toIntOrNull()}.let{if(it.size==4)it else listOf(0,0,0,0)}) }
   var skillTotal by remember { mutableStateOf(prefs.getString("skillTotal","").orEmpty().split(",").mapNotNull{it.toIntOrNull()}.let{if(it.size==4)it else listOf(0,0,0,0)}) }
   fun trackSkill(skill:Int,correct:Boolean){
    skillTotal=skillTotal.mapIndexed{i,n->if(i==skill)n+1 else n}
    if(correct)skillCorrect=skillCorrect.mapIndexed{i,n->if(i==skill)n+1 else n}
    prefs.edit().putString("skillCorrect",skillCorrect.joinToString(",")).putString("skillTotal",skillTotal.joinToString(",")).apply()
   }
   var level by remember { mutableStateOf(prefs.getString("level","Начинающий")?:"Начинающий") }
   var dailyGoal by remember { mutableIntStateOf(prefs.getInt("dailyGoal",10).coerceIn(5,30)) }
   var todayCompleted by remember { mutableIntStateOf(if(prefs.getString("goalDate","")==LocalDate.now().toString())prefs.getInt("goalCount",0) else 0) }
   var streak by remember { mutableIntStateOf(prefs.getInt("dailyStreak",0)) }
   var answerCombo by remember { mutableIntStateOf(0) }
   var reminderCount by remember { mutableIntStateOf(prefs.getInt("reminders",3)) }
   var effectsEnabled by remember { mutableStateOf(prefs.getBoolean("effectsEnabled",true)) }
   var voiceSpeed by remember { mutableFloatStateOf(prefs.getFloat("voiceSpeed",0.93f)) }
   var studiedDates by remember { mutableStateOf(prefs.getString("studiedDates","")!!.split(",").filter{it.isNotBlank()}.toSet()) }
   fun markStudyDay(){
    val today=LocalDate.now();val key=today.toString()
    todayCompleted=if(prefs.getString("goalDate","")==key)todayCompleted+1 else 1
    prefs.edit().putString("goalDate",key).putInt("goalCount",todayCompleted).apply()
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
   var foxMood by remember { mutableStateOf("curious") }
   fun normalizeSpoken(value:String)=value.lowercase(Locale.US).replace(Regex("[^a-z' ]")," ").replace(Regex("\\s+")," ").trim()
   fun speechSimilarity(expected:String,spoken:String):Int {
    val a=normalizeSpoken(expected).split(" ").filter{it.isNotBlank()}
    val b=normalizeSpoken(spoken).split(" ").filter{it.isNotBlank()}
    if(a.isEmpty()||b.isEmpty())return 0
    val row=IntArray(b.size+1){it}
    for(i in a.indices){
     var prev=row[0];row[0]=i+1
     for(j in b.indices){
      val old=row[j+1]
      row[j+1]=minOf(row[j+1]+1,row[j]+1,prev+if(a[i]==b[j])0 else 1)
      prev=old
     }
    }
    return ((1.0-row[b.size].toDouble()/maxOf(a.size,b.size))*100).toInt().coerceIn(0,100)
   }
   val speechLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
    val heard=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
    speechResult=heard
    val score=speechSimilarity(speechTarget,heard)
    if(heard.isNotBlank() && score>=85){
     trackSkill(3,true);foxExcited=true;foxMood="happy";sayReaction(true)
     storyFeedback="Распознано $score% слов. Произнесено: $heard. Это не оценка акцента."
    }else if(heard.isNotBlank()){
     trackSkill(3,false);foxExcited=false;foxMood="support";sayReaction(false)
     storyFeedback="Совпадение слов: $score%. Распознано: $heard. Послушай образец и повтори."
    }else{foxMood="surprised";storyFeedback="Речь не распознана. Попробуй снова."}
    prefs.edit().putInt("xp",xp).putInt("gems",gems).apply()
   }
   fun practiceSpeech(phrase:String){
    speechTarget=phrase;speechResult="";storyFeedback=""
    try {
     val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US")
      putExtra(RecognizerIntent.EXTRA_PROMPT,"Скажи: $phrase")
      putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
      putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true)
     }
     speechLauncher.launch(intent)
    }catch(_:Exception){storyFeedback="Офлайн-распознавание недоступно. Установи английский языковой пакет в настройках Android."}
   }

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
      if(effectsEnabled)playCorrectFeedback()
      sayEnglish(left)
      if(reviewMatched.size==reviewPairs.size){foxExcited=true;playMilestoneSound();sayEnglish("Ta-da! Amazing! You did it!",false)}
     }else{if(effectsEnabled)playWrongFeedback();sayEnglish("Try again!")}
     reviewLeft=null;reviewRight=null
    }
   }
   val foxScale by animateFloatAsState(targetValue=if(foxExcited)1.14f else 1f,animationSpec=spring(dampingRatio=Spring.DampingRatioMediumBouncy,stiffness=Spring.StiffnessLow),label="foxReaction")
   fun save(){prefs.edit().putString("repeatQueue",repeatQueue.joinToString(",")).putInt("xp",xp).putInt("gems",gems).putInt("lesson",current).putString("completed",completed.joinToString(",")).putString("mistakes",mistakes.joinToString(",")).apply()}
   fun start(n:Int){current=n;index=0;selected=null;bankWords=emptyList();typedAnswer="";hintShown=false;showTranslation=false;correctCount=0;answerCombo=0;foxMessage="Давай попробуем!";foxExcited=false;screen="lesson";save()}
   LaunchedEffect(screen,speedStartedAt) {
    if(screen=="speed" && !speedFinished) {
     while(screen=="speed" && !speedFinished) {
      speedRemaining=(60-((System.currentTimeMillis()-speedStartedAt)/1000L).toInt()).coerceAtLeast(0)
      if(speedRemaining==0){speedFinished=true;if(speedCorrect>speedBest){speedBest=speedCorrect;prefs.edit().putInt("speedBest",speedBest).apply()};markStudyDay();break}
      kotlinx.coroutines.delay(250)
     }
    }
   }
   LaunchedEffect(screen) { if(screen=="review"){reviewMatched=emptySet();reviewLeft=null;reviewRight=null;sayEnglish("Match the words!")} }
   LaunchedEffect(screen) { if(screen=="done"){ foxExcited=true;celebration=true;if(effectsEnabled)playMilestoneSound();sayEnglish("Hooray! Fantastic! You did it!"); kotlinx.coroutines.delay(1800); foxExcited=false;celebration=false } }
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
   val compact=LocalConfiguration.current.screenHeightDp<850
   val foxTransparent=remember {
    val source=BitmapFactory.decodeResource(resources,R.drawable.fox_original)
    val side=minOf(source.width,source.height)
    // Crop into the mascot's face rather than displaying the square logo and its white border.
    val cropSize=(side*0.66f).toInt().coerceAtLeast(1)
    val centerX=(source.width*0.48f).toInt()
    val centerY=(source.height*0.52f).toInt()
    val left=(centerX-cropSize/2).coerceIn(0,source.width-cropSize)
    val top=(centerY-cropSize/2).coerceIn(0,source.height-cropSize)
    val crop=Bitmap.createBitmap(source,left,top,cropSize,cropSize)
    val output=Bitmap.createBitmap(cropSize,cropSize,Bitmap.Config.ARGB_8888)
    val canvas=android.graphics.Canvas(output)
    val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val radius=cropSize*0.49f
    val path=android.graphics.Path().apply { addCircle(cropSize/2f,cropSize/2f,radius,android.graphics.Path.Direction.CW) }
    canvas.clipPath(path)
    canvas.drawBitmap(crop,0f,0f,paint)
    output.asImageBitmap()
   }
   val floating=rememberInfiniteTransition(label="mascot")
   val bounce by floating.animateFloat(initialValue=0f,targetValue=-8f,animationSpec=infiniteRepeatable(tween(1300),RepeatMode.Reverse),label="bounce")
   val foxTilt by animateFloatAsState(targetValue=if(foxExcited)12f else 0f,animationSpec=spring(dampingRatio=Spring.DampingRatioMediumBouncy),label="foxTilt")
   val foxWiggle by floating.animateFloat(initialValue=-2f,targetValue=2f,animationSpec=infiniteRepeatable(tween(750),RepeatMode.Reverse),label="foxWiggle")
   MaterialTheme(colorScheme=darkColorScheme(primary=purple,secondary=pink,background=dark,surface=panel,onSurface=ink)) {
    Surface(Modifier.fillMaxSize(),color=dark) {
     Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
     LazyColumn(modifier=Modifier.weight(1f),userScrollEnabled=screen!="lesson" && screen!="placement",contentPadding=PaddingValues(start=14.dp,end=14.dp,top=if(screen=="lesson" || screen=="placement") 4.dp else 16.dp,bottom=if(screen=="lesson" || screen=="placement") 0.dp else 8.dp),verticalArrangement=Arrangement.spacedBy(if(screen=="lesson" || screen=="placement") 4.dp else 12.dp)) {
      if(screen=="onboarding") {
       item { LinearProgressIndicator(progress={(onboardStep+1)/3f},modifier=Modifier.fillMaxWidth().height(10.dp),color=green) }
       item { Row(verticalAlignment=Alignment.CenterVertically) { Image(bitmap=foxTransparent,contentDescription="Лисичка",modifier=Modifier.size(95.dp)); Text(when(onboardStep){0->"Привет! Я твоя лисичка! 🦊";1->"Как хорошо ты знаешь английский?";else->"Сколько минут будем играть каждый день?"},fontSize=22.sp,fontWeight=FontWeight.Black,color=ink,lineHeight=28.sp) } }
       if(onboardStep==0) item { Text("Учимся весело, бесплатно и без рекламы! 🌈",fontSize=23.sp,fontWeight=FontWeight.Bold,color=pink) }
       if(onboardStep==1) { item { Text("Выбери свой уровень",fontSize=21.sp,fontWeight=FontWeight.Black,color=ink) }; listOf("Начинающий","Знаю несколько слов","Могу говорить").forEach { option -> item { OutlinedButton(onClick={level=option},modifier=Modifier.fillMaxWidth().height(70.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(level==option)Color(0xFF365B4D) else panel),border=BorderStroke(2.dp,if(level==option)green else Color(0xFF3B5057))) {Text(option,fontSize=19.sp,fontWeight=FontWeight.Bold,color=ink)} } } }
       if(onboardStep==2) { listOf(3,10,15,30).forEach { minutes -> item { OutlinedButton(onClick={dailyGoal=minutes},modifier=Modifier.fillMaxWidth().height(65.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(dailyGoal==minutes)Color(0xFF365B4D) else panel),border=BorderStroke(2.dp,if(dailyGoal==minutes)green else Color(0xFF3B5057))) {Text("$minutes минут в день",fontSize=19.sp,fontWeight=FontWeight.Bold,color=ink)} } } }
      }

      if(screen=="home" && showHomeGreeting) item {
       Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF2B3544)),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
         AnimatedFox(modifier=Modifier.size(82.dp).offset(y=bounce.dp),mood=foxMood)
         Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
          Text("Привет! Я твоя лисичка! 🦊",fontSize=19.sp,fontWeight=FontWeight.Black,color=ink,lineHeight=23.sp)
          Text("Учимся весело, бесплатно и без рекламы! 🌈",fontSize=14.sp,color=pink,lineHeight=18.sp)
         }
         Text("×",modifier=Modifier.clickable{showHomeGreeting=false}.padding(4.dp),fontSize=25.sp,color=ink)
        }
       }
      }
      if(screen=="home") item {
       Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF284349)),shape=RoundedCornerShape(22.dp)){
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
         Text("📖 Истории с лисичкой",fontSize=23.sp,fontWeight=FontWeight.Black,color=ink)
         Text("Слушай диалоги, отвечай и тренируй произношение.",color=ink)
         Text("${stories.size} оригинальных историй · уровни A1–B1",color=ink)
         Button(onClick={storyIndex=0;storyStep=0;storyFeedback="";screen="story"},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=green)){Text("Начать историю",color=dark)}
         OutlinedButton(onClick={screen="storyLibrary"},modifier=Modifier.fillMaxWidth()){Text("📚 Выбрать из ${stories.size} историй")}
         OutlinedButton(onClick={screen="vocabulary"},modifier=Modifier.fillMaxWidth()){Text("📗 Словарь · ${vocabulary.size} выражений")}
         OutlinedButton(onClick={cardRevealed=false;screen="wordPractice"},modifier=Modifier.fillMaxWidth()){Text("🧠 Карточки слов · выучено ${cardMastered.size}")}

         OutlinedButton(onClick={speechTarget="Hello!";storyFeedback="";screen="speaking"},modifier=Modifier.fillMaxWidth()){Text("🎙️ Тренировать произношение")}
        }
       }
      }
      if(screen=="home") item {
       Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(20.dp)){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
         Text("⚡ Игра на скорость",fontSize=22.sp,fontWeight=FontWeight.Black,color=ink)
         Text("10 вопросов за 60 секунд. Рекорд: $speedBest",color=ink)
         Button(onClick={speedRound=0;speedCorrect=0;speedFinished=false;speedStartedAt=System.currentTimeMillis();speedRemaining=60;screen="speed"},modifier=Modifier.fillMaxWidth()){Text("Начать скоростную игру")}
        }
       }
      }
      if(screen=="home") item {
       Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(20.dp)){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
         Text("🎯 Задания на сегодня",fontSize=22.sp,fontWeight=FontWeight.Black,color=purple)
         val practiced=LocalDate.now().toString() in studiedDates
         Text(if(practiced)"✅ Позаниматься сегодня" else "⬜ Позаниматься сегодня",color=ink)
         Text(if(completed.isNotEmpty())"✅ Пройти первый урок" else "⬜ Пройти первый урок",color=ink)
         Text(if(streak>=3)"✅ Заниматься 3 дня подряд" else "⬜ Заниматься 3 дня подряд",color=ink)
        }
       }
      }
      if(screen=="speed"){
       val elapsed=60-speedRemaining
       val q=lessons[(speedRound*7+current)%lessons.size].questions[speedRound%7]
       item { Text("‹  ⚡ Игра на скорость",modifier=Modifier.clickable{screen="home"},fontSize=23.sp,fontWeight=FontWeight.Black,color=purple) }
       item { Text("Вопрос ${speedRound.coerceAtMost(10)}/10 · Верно: $speedCorrect · Время: ${(60-elapsed).coerceAtLeast(0)} с",fontSize=18.sp,color=ink) }
       if(speedFinished || elapsed>=60 || speedRound>=10){
        item { Text("🏆 Результат: $speedCorrect из 10",fontSize=27.sp,fontWeight=FontWeight.Black,color=green) }
        item { Button(onClick={screen="home"},modifier=Modifier.fillMaxWidth()){Text("К урокам")} }
       }else{
        item { Text(q.prompt,fontSize=25.sp,fontWeight=FontWeight.Black,color=ink) }
        q.choices.forEachIndexed{idx,choice->item { OutlinedButton(onClick={
         val correct=idx==q.answer
         if(correct)speedCorrect++
         trackSkill(0,correct);sayReaction(correct)
         speedRound++
         if(speedRound>=10 || System.currentTimeMillis()-speedStartedAt>=60000L){
          speedFinished=true
          if(speedCorrect>speedBest){speedBest=speedCorrect;prefs.edit().putInt("speedBest",speedBest).apply()}
          markStudyDay()
         }
        },modifier=Modifier.fillMaxWidth()){Text(choice,color=ink)} }}
       }
      }
      if(screen=="wordPractice"){
       item { Text("‹  Тренировка слов",modifier=Modifier.clickable{screen="home"},fontSize=24.sp,fontWeight=FontWeight.Black,color=purple) }
       item { Text("Карточки для запоминания · ${cardMastered.size} изучено · работают без интернета",color=ink) }
       val card=dictionary[cardIndex.coerceIn(0,dictionary.lastIndex)]
       item {
        Card(colors=CardDefaults.cardColors(containerColor=panel),modifier=Modifier.fillMaxWidth()){
         Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
          Text("Уровень ${card.getString("level")}",fontSize=13.sp,color=green)
          Text(card.getString("en"),fontSize=28.sp,fontWeight=FontWeight.Black,color=ink)
          OutlinedButton(onClick={sayEnglish(card.getString("en"))}){Text("🔊 Послушать")}
          if(cardRevealed){
           Text(card.getString("ru"),fontSize=22.sp,color=pink)
           if(card.getString("example_en")!=card.getString("en")){
            Text(card.getString("example_en"),fontSize=16.sp,color=ink)
            Text(card.getString("example_ru"),fontSize=14.sp,color=ink)
            OutlinedButton(onClick={sayEnglish(card.getString("example_en"))}){Text("▶ Пример")}
           }
           Text("Вспомнил перевод самостоятельно?",color=ink)
           Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(onClick={nextCard(false)},modifier=Modifier.weight(1f)){Text("Ещё раз")}
            Button(onClick={nextCard(true)},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=green)){Text("Помню",color=dark)}
           }
          }else{
           Button(onClick={cardRevealed=true},modifier=Modifier.fillMaxWidth()){Text("Показать перевод")}
          }
         }
        }
       }
       item { Text("Слова, отмеченные «Помню», возвращаются к повторению через 3 дня. «Ещё раз» — при следующем круге. Это самостоятельная оценка памяти, не проверка знания.",fontSize=12.sp,color=ink) }
      }
      if(screen=="vocabulary"){
       item { Text("‹  Офлайн-словарь",modifier=Modifier.clickable{screen="home"},fontSize=25.sp,fontWeight=FontWeight.Black,color=purple) }
       item { Text("${dictionary.size} английских слов и выражений с русским переводом. Все данные встроены в APK.",color=ink) }
       item { OutlinedTextField(value=vocabQuery,onValueChange={vocabQuery=it},label={Text("Поиск на английском или русском")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
       item { Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Все","A1","A2","B1").forEach{level->
        OutlinedButton(onClick={vocabLevel=level},colors=ButtonDefaults.outlinedButtonColors(containerColor=if(vocabLevel==level)green else panel)){Text(level,color=if(vocabLevel==level)dark else ink)}
       }} }
       item { OutlinedButton(onClick={vocabOnlyFavorites=!vocabOnlyFavorites},modifier=Modifier.fillMaxWidth()){Text(if(vocabOnlyFavorites)"★ Только избранное" else "☆ Показать избранное")} }
       val filtered=dictionary.filter{entry->
        (vocabLevel=="Все"||entry.getString("level")==vocabLevel) &&
        (!vocabOnlyFavorites||entry.getString("en") in vocabFavorites) &&
        (vocabQuery.isBlank()||entry.getString("en").contains(vocabQuery,true)||entry.getString("ru").contains(vocabQuery,true)||entry.getString("example_en").contains(vocabQuery,true)||entry.getString("example_ru").contains(vocabQuery,true))
       }
       item { Text("Найдено: ${filtered.size}. Показаны первые 100.",color=ink) }
       items(filtered.take(100)){entry->
        val en=entry.getString("en")
        val ru=entry.getString("ru")
        val exampleEn=entry.getString("example_en")
        val exampleRu=entry.getString("example_ru")
        Card(colors=CardDefaults.cardColors(containerColor=panel),modifier=Modifier.fillMaxWidth()){
         Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
          Text(en,fontSize=19.sp,fontWeight=FontWeight.Bold,color=ink)
          Text(ru,fontSize=15.sp,color=pink)
          Text("Уровень ${entry.getString("level")} · ${entry.getString("source")}",fontSize=11.sp,color=ink)
          if(!exampleEn.equals(en,true)){
           Text("Пример: $exampleEn",fontSize=14.sp,color=ink)
           Text(exampleRu,fontSize=13.sp,color=ink)
          }else{
           Text("Пример употребления пока не добавлен",fontSize=12.sp,color=ink)
          }
          Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
           OutlinedButton(onClick={sayEnglish(en)}){Text("🔊 Слово")}
           if(!exampleEn.equals(en,true))OutlinedButton(onClick={sayEnglish(exampleEn)}){Text("▶ Пример")}
           OutlinedButton(onClick={
            vocabFavorites=if(en in vocabFavorites)vocabFavorites-en else vocabFavorites+en
            prefs.edit().putStringSet("dictionaryFavorites",vocabFavorites).apply()
           }){Text(if(en in vocabFavorites)"★" else "☆")}
          }
         }
        }
       }
      }
      if(screen=="storyLibrary"){
       item { Text("‹  Библиотека историй",modifier=Modifier.clickable{screen="home"},fontSize=24.sp,fontWeight=FontWeight.Black,color=purple) }
       item { OutlinedTextField(value=storyQuery,onValueChange={storyQuery=it},label={Text("Поиск по названию")},modifier=Modifier.fillMaxWidth(),singleLine=true) }
       val filtered=storyTitles.indices.filter{storyQuery.isBlank()||storyTitles[it].contains(storyQuery,true)}
       items(filtered){n->
        OutlinedButton(onClick={storyIndex=n;storyStep=0;storyFeedback="";screen="story"},modifier=Modifier.fillMaxWidth()){
         Text("${n+1}. ${storyTitles[n]} ${if(n in rewardedStories)"✓" else ""}",color=ink)
        }
       }
      }
      if(screen=="story") {
       val phrase=stories[storyIndex][storyStep]
       item { Text("‹  Истории с лисичкой",modifier=Modifier.clickable{screen="home"},fontSize=23.sp,fontWeight=FontWeight.Black,color=purple) }
       item { LinearProgressIndicator(progress={(storyStep+1f)/stories[storyIndex].size},modifier=Modifier.fillMaxWidth(),color=green) }
       item { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
        AnimatedFox(modifier=Modifier.size(100.dp).offset(y=bounce.dp),mood=if(storyFeedback.startsWith("Правильно"))"happy" else if(storyFeedback.isNotBlank())"support" else "curious")
        Text(phrase.first,fontSize=23.sp,fontWeight=FontWeight.Black,color=ink,modifier=Modifier.weight(1f))
       } }
       item { Button(onClick={sayEnglish(phrase.first)},modifier=Modifier.fillMaxWidth()){Text("🔊 Послушать диалог")} }
       item { OutlinedButton(onClick={sayEnglish(phrase.first,slow=true)},modifier=Modifier.fillMaxWidth()){Text("🐢 Медленно")} }
       item { Text("Как перевести фразу?",fontSize=20.sp,fontWeight=FontWeight.Bold,color=ink) }
       val storyTranslations=stories.flatMap{story->story.map{it.second}}.distinct().filter{it!=phrase.second}
       val alternatives=listOf(phrase.second,storyTranslations[(storyIndex*7+storyStep*11)%storyTranslations.size],storyTranslations[(storyIndex*17+storyStep*19+23)%storyTranslations.size]).distinct().let{options->if(options.size<3)options+storyTranslations.first{it !in options} else options}
       val ordered=if(storyStep%2==0)listOf(alternatives[1],alternatives[0],alternatives[2]) else listOf(alternatives[2],alternatives[1],alternatives[0])
       ordered.forEach{answer->item { OutlinedButton(onClick={
        if(answer==phrase.second){trackSkill(0,true);storyFeedback="Правильно! "+phrase.second;sayReaction(true)}
        else{trackSkill(0,false);storyFeedback="Не совсем. Послушай фразу ещё раз.";sayReaction(false)}
       },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(containerColor=panel)){Text(answer,color=ink)} }}

       if(storyFeedback.isNotBlank()) item { Text(storyFeedback,fontSize=18.sp,fontWeight=FontWeight.Bold,color=green) }
       if(storyFeedback.startsWith("Правильно")) item { Button(onClick={
        if(storyStep==stories[storyIndex].lastIndex){
         if(storyIndex !in rewardedStories){xp+=30;gems+=5;rewardedStories=rewardedStories+storyIndex;prefs.edit().putString("rewardedStories",rewardedStories.joinToString(",")).apply()};markStudyDay();save();playMilestoneSound()
         if(storyIndex==stories.lastIndex)screen="home" else {storyIndex++;storyStep=0}
        }else storyStep++
        storyFeedback=""
       },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=green)){Text(if(storyStep==stories[storyIndex].lastIndex)"Завершить историю +30 XP" else "Продолжить",color=dark)} }
      }
      if(screen=="speaking") {
       item { Text("‹  Говорим по-английски",modifier=Modifier.clickable{screen="home"},fontSize=23.sp,fontWeight=FontWeight.Black,color=purple) }
       item { AnimatedFox(modifier=Modifier.size(155.dp).offset(y=bounce.dp),mood=foxMood) }
       item { Text("Повтори за лисичкой",fontSize=22.sp,fontWeight=FontWeight.Bold,color=ink) }
       item { Text(speechTarget,fontSize=30.sp,fontWeight=FontWeight.Black,color=green) }
       item { Button(onClick={sayEnglish(speechTarget)},modifier=Modifier.fillMaxWidth()){Text("🔊 Слушать")} }
       item { Button(onClick={practiceSpeech(speechTarget)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=purple)){Text("🎙️ Говорить")} }
       if(storyFeedback.isNotBlank())item { Text(storyFeedback,fontSize=18.sp,color=ink) }
       item { Button(onClick={speechTarget=listOf("Hello!","My name is Fox.","How are you?","I like learning English.","Where is the station?")[(listOf("Hello!","My name is Fox.","How are you?","I like learning English.","Where is the station?").indexOf(speechTarget)+1)%5];storyFeedback=""},modifier=Modifier.fillMaxWidth()){Text("Следующая фраза →")} }
       item { Text("Для работы без интернета установи английский офлайн-пакет распознавания речи в настройках Android. Оценка показывает совпадение распознанных слов, а не качество акцента.",fontSize=13.sp,color=ink) }
      }
      if(screen=="home") item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
       Column { Text("LinguaFree",fontSize=35.sp,fontWeight=FontWeight.Black,color=purple)
        Text("ИГРАЙ • УЧИСЬ • ПОБЕЖДАЙ",fontSize=12.sp,fontWeight=FontWeight.ExtraBold,color=pink) }
       AnimatedFox(modifier=Modifier.size(92.dp).offset(y=bounce.dp),mood=foxMood)
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
       item {
        Card(colors=CardDefaults.cardColors(containerColor=panel),modifier=Modifier.fillMaxWidth()){
         Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
          Text("🎯 Цель на сегодня: $todayCompleted / $dailyGoal занятий",fontSize=18.sp,fontWeight=FontWeight.Bold,color=ink)
          LinearProgressIndicator(progress={(todayCompleted.toFloat()/dailyGoal).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth(),color=green)
          Text(if(todayCompleted>=dailyGoal)"Цель выполнена! Продолжай в удобном темпе." else "До цели осталось ${dailyGoal-todayCompleted} занятий.",color=ink)
          Text("🔥 Серия: $streak дн. · ⭐ $xp XP · 📚 ${completed.size}/300 уроков",color=ink)
          if(reviewDue.values.any{it<=System.currentTimeMillis()}){
           OutlinedButton(onClick={val key=reviewDue.filterValues{it<=System.currentTimeMillis()}.keys.first();start((key/1000).coerceIn(0,lessons.lastIndex));index=(key%1000).coerceIn(0,lessons[current].questions.lastIndex)},modifier=Modifier.fillMaxWidth()){Text("🧠 Повторить по расписанию")}
          }
         }
        }
       }
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
       item { Text("📊 Статистика навыков",fontSize=22.sp,fontWeight=FontWeight.Black,color=ink) }
       listOf("📖 Чтение и перевод","🎧 Аудирование","✍️ Письмо","🎙️ Говорение").forEachIndexed{i,name->
        item { Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
         Text("$name: ${skillCorrect[i]}/${skillTotal[i]} (${if(skillTotal[i]>0)skillCorrect[i]*100/skillTotal[i] else 0}%)",color=ink)
         LinearProgressIndicator(progress={if(skillTotal[i]>0)skillCorrect[i].toFloat()/skillTotal[i] else 0f},modifier=Modifier.fillMaxWidth(),color=green)
        } }
       }
       item { Text("⚡ Лучший результат в игре: $speedBest/10",color=ink) }
       item { Text("🎯 Ежедневная цель: $todayCompleted/$dailyGoal занятий",color=ink) }
       item { Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf(5,10,15,20).forEach{goal->
        OutlinedButton(onClick={dailyGoal=goal;prefs.edit().putInt("dailyGoal",goal).apply()},colors=ButtonDefaults.outlinedButtonColors(containerColor=if(dailyGoal==goal)green else panel)){Text("$goal",color=if(dailyGoal==goal)dark else ink)}
       }} }
       item { Text("🗣️ Голос: ${selectedVoiceName?:"Системный английский голос"}",fontSize=13.sp,color=ink) }
       item { Text("📴 Без интернета: уроки, истории, словарь, прогресс, напоминания, анимации, звуки и вибрация. Для английской озвучки нужен установленный офлайн-голос Android, для распознавания — офлайн-языковой пакет.",fontSize=13.sp,color=ink) }
       item { OutlinedButton(onClick={exportProgress.launch("LinguaFree-progress.json")},modifier=Modifier.fillMaxWidth()){Text("💾 Сохранить резервную копию прогресса")} }
       item { OutlinedButton(onClick={importProgress.launch(arrayOf("application/json","text/plain"))},modifier=Modifier.fillMaxWidth()){Text("📂 Восстановить прогресс из файла")} }
       item { Text("Восстановление заменит сохранённые значения. Перед ним сделай резервную копию. Файл хранится там, где ты выберешь.",fontSize=12.sp,color=ink) }
       if(backupStatus.isNotBlank())item { Text(backupStatus,color=ink) }
       item { OutlinedButton(onClick={try{startActivity(Intent("com.android.settings.TTS_SETTINGS"))}catch(_:Exception){startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))}},modifier=Modifier.fillMaxWidth()){Text("⚙️ Настроить офлайн-голос Android")} }

       item { Text("Скорость английского",fontSize=16.sp,color=ink) }
       item { Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(0.80f,0.93f,1.08f).forEach{speed->OutlinedButton(onClick={voiceSpeed=speed;prefs.edit().putFloat("voiceSpeed",speed).apply();sayEnglish("Hello! Welcome to LinguaFree!")},colors=ButtonDefaults.outlinedButtonColors(containerColor=if(voiceSpeed==speed)green else panel)){Text(when(speed){0.80f->"Медленно";0.93f->"Обычно";else->"Быстро"},fontSize=12.sp,color=if(voiceSpeed==speed)dark else ink)}}} }
       item { Button(onClick={sayEnglish("Hello! Let's learn English together!")},modifier=Modifier.fillMaxWidth()){Text("🔊 Послушать голос лисички")} }

       item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("🔊 Звуки и вибрация ответов",color=ink);Switch(checked=effectsEnabled,onCheckedChange={effectsEnabled=it;prefs.edit().putBoolean("effectsEnabled",it).apply()})} }
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
      } else if(screen=="lesson" || screen=="placement") {
       val l=lessons[current];val q=activeQuestion();val key=current*1000+index
       val mastery=reviewStages[key]?:0
       val wordBank=screen=="lesson" && index%7==2 && q.choices[q.answer].contains(" ") && mastery<2
       // Lessons 1-50 teach recognition; spelling is introduced gradually later.
       // A mastered word alone must never force typing in a beginner lesson.
       val englishPrompt=q.prompt.startsWith("Как по-английски")
       val missingMode=screen=="lesson" && current>=50 && index%7==6 && englishPrompt && q.choices[q.answer].length>2
       val typedMode=screen=="lesson" && englishPrompt && (missingMode || (current>=100 && mastery>=2 && (index%7==4 || index%7==5)))
       val correctWord=q.choices[q.answer]
       val missingIndex=correctWord.indexOfFirst{it.lowercaseChar() in "aeiou"}.let{if(it>0)it else 1}
       val audioMode=screen=="lesson" && index%7==1 && mastery>=1 && !q.prompt.startsWith("Как по-английски")
       val pictureMode=screen=="lesson" && index%7==0 && q.choices.all { it.lowercase(Locale.ROOT) in setOf("tea","coffee","milk","apple","cat","dog","water","book","house","hello") }
       item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text("‹",modifier=Modifier.clickable{screen="home"}.padding(end=8.dp),fontSize=26.sp,fontWeight=FontWeight.Black,color=purple)
        Text(if(screen=="placement")"🎯 Проверка знаний" else "🎯 "+l.title,fontSize=if(compact)16.sp else 19.sp,fontWeight=FontWeight.Black,color=purple,modifier=Modifier.weight(1f),maxLines=1)
        Text("⚡ $answerCombo  ⭐ ${index+1}/${if(screen=="placement")placementLessons.size else l.questions.size}",fontSize=if(compact)13.sp else 15.sp,fontWeight=FontWeight.Black,color=pink)
       } }

       item { LinearProgressIndicator(progress={(index+1).toFloat()/(if(screen=="placement")placementLessons.size else l.questions.size)},modifier=Modifier.fillMaxWidth().height(if(compact)6.dp else 9.dp).clip(RoundedCornerShape(9.dp)),color=green,trackColor=Color(0xFFE7DDF9)) }

       item { Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF3B2A45))) { Column(Modifier.padding(if(compact)9.dp else 12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
         Image(bitmap=foxTransparent,contentDescription=null,modifier=Modifier.size(if(compact)20.dp else 24.dp).offset(y=bounce.dp).graphicsLayer { scaleX=foxScale;scaleY=foxScale;rotationZ=foxTilt+foxWiggle })
         Text(foxMessage,fontSize=if(compact)13.sp else 15.sp,fontWeight=FontWeight.Black,color=pink)
        }
        Spacer(Modifier.height(2.dp))
        Text(if(audioMode)"🎧 Прослушай слово и выбери перевод" else q.prompt,fontSize=if(compact)19.sp else 23.sp,fontWeight=FontWeight.Black,color=ink,lineHeight=if(compact)23.sp else 28.sp)
       } } }
       if(selected!=null || !q.prompt.startsWith("Как по-английски")) item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){Button(onClick={sayEnglish(q.speech)},modifier=Modifier.weight(1f).height(if(compact)37.dp else 43.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=teal)){Text("🔊 СЛУШАТЬ",fontSize=if(compact)12.sp else 15.sp,fontWeight=FontWeight.Black)};OutlinedButton(onClick={sayEnglish(q.speech,slow=true)},modifier=Modifier.weight(1f).height(if(compact)37.dp else 43.dp),shape=RoundedCornerShape(14.dp)){Text("🐢 МЕДЛЕННО",fontSize=if(compact)12.sp else 15.sp,fontWeight=FontWeight.Black)}} }
       if(typedMode) item {
        Text(if(missingMode)"🔤 Впиши пропущенную букву: "+correctWord.replaceRange(missingIndex,missingIndex+1,"_") else "✍️ Напиши английский ответ",fontSize=17.sp,fontWeight=FontWeight.Bold,color=ink)
        OutlinedTextField(value=typedAnswer,onValueChange={typedAnswer=it},label={Text("Твой ответ")},singleLine=true,modifier=Modifier.fillMaxWidth(),enabled=selected==null)
        if(selected==null) TextButton(onClick={hintShown=true}){Text(if(hintShown)"Подсказка: первая буква — ${q.choices[q.answer].firstOrNull()?:'?'}" else "💡 Нужна подсказка?")}
        Button(onClick={
         val right=typedAnswer.trim().equals(if(missingMode)correctWord[missingIndex].toString() else correctWord,ignoreCase=true)
         selected=if(right)q.answer else (q.answer+1)%q.choices.size
         foxExcited=right;foxMood=if(right)"happy" else "support";foxMessage=if(right)when(answerCombo+1){3->"🔥 Три подряд!";5->"🌟 Пять подряд!";10->"🏆 Десять подряд!";else->"Отлично! 🌟"} else "Попробуй ещё! 💪"
         trackSkill(2,right)
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
         foxExcited=i==q.answer;foxMood=if(i==q.answer)if(answerCombo>=2)"celebrate" else "happy" else "support"
         foxMessage=if(i==q.answer) when(answerCombo+1){3->"🔥 Три подряд! Great!";5->"🌟 Пять подряд! Excellent!";10->"🏆 Десять подряд! Amazing!";else->listOf("Great! 🌟","Well done! 🎉","Excellent! 🏆","Amazing! ✨")[index%4]} else "Try again! 💪"
         sayEnglish(q.choices[q.answer])
         trackSkill(if(audioMode)1 else 0,i==q.answer)
         sayReaction(i==q.answer)
        },enabled=selected==null,modifier=Modifier.fillMaxWidth().height(if(compact) (if(pictureMode) 58.dp else 49.dp) else (if(pictureMode) 68.dp else 54.dp)),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=bg,disabledContainerColor=bg,contentColor=ink,disabledContentColor=ink),border=BorderStroke(3.dp,if(right)green else if(chosen)pink else Color(0xFF40565D))) {
         Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(1.dp)) {
          if(pictureMode) Text(when { choice.contains("Tea",true)->"🍵";choice.contains("Coffee",true)->"☕";choice.contains("Milk",true)->"🥛";choice.contains("Apple",true)->"🍎";choice.contains("Cat",true)->"🐈";choice.contains("Dog",true)->"🐕";choice.contains("Water",true)->"💧";choice.contains("Book",true)->"📚";choice.contains("House",true)->"🏠";choice.contains("Hello",true)->"👋";else->"🗂️" },fontSize=if(compact) 21.sp else 24.sp)
          Text(choice,fontSize=if(compact) 16.sp else 18.sp,fontWeight=FontWeight.ExtraBold,lineHeight=21.sp,maxLines=2)
         }
        }
       } }
       if(selected!=null) {
        if(selected==q.answer && (answerCombo+1)==5) item { Text("🔥 Пять ответов подряд! Бонус после продолжения",fontSize=15.sp,fontWeight=FontWeight.Black,color=green) }
        item { Text(q.explanation,fontSize=if(compact)12.sp else 14.sp,fontWeight=FontWeight.Bold,color=ink,maxLines=2) }
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

      }
     }
     if(screen=="review" && reviewMatched.size==reviewPairs.size) {
      Button(onClick={screen="done";foxExcited=true},modifier=Modifier.fillMaxWidth().padding(start=14.dp,end=14.dp,top=3.dp,bottom=5.dp).height(if(compact)46.dp else 54.dp),colors=ButtonDefaults.buttonColors(containerColor=green,contentColor=dark),shape=RoundedCornerShape(17.dp)) {Text("ЗАБРАТЬ ОПЫТ ⭐",fontSize=21.sp,fontWeight=FontWeight.Black)}
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
