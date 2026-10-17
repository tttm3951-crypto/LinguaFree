package org.linguafree.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ClassroomHttpError(val status:Int):IllegalStateException(when(status){
    401->"Доступ недействителен или отозван. Подключитесь заново."
    403->"Создание кабинетов пока закрыто владельцем сервера."
    404->"Код истёк или запись недоступна."
    409->"Конфликт версии данных или заполненный кабинет."
    429->"Слишком много запросов. Повторим позже."
    else->"Сервер: HTTP $status"
})

/** Opt-in snapshots, durable local outbox and retryable deletion; never a remote-control interface. */
object ClassroomSync {
    val ENDPOINT:String=BuildConfig.CLASSROOM_ENDPOINT
    val configured:Boolean get()=runCatching{
        val url=URL(ENDPOINT)
        url.protocol=="https"&&url.host.isNotBlank()&&url.userInfo==null&&url.query==null&&url.ref==null
    }.getOrDefault(false)
    private val stateLock=Any()
    private val flushLock=ReentrantLock()
    private val executor=Executors.newSingleThreadExecutor()
    private fun prefs(context:Context)=context.getSharedPreferences("linguafree_sync",Context.MODE_PRIVATE)

    private fun request(path:String,method:String,payload:JSONObject?=null,token:String?=null):JSONObject{
        check(configured){"Сервер синхронизации ещё не подключён"}
        val connection=URL(ENDPOINT.trimEnd('/')+path).openConnection() as HttpURLConnection
        try{
            connection.instanceFollowRedirects=false
            connection.requestMethod=method;connection.connectTimeout=10000;connection.readTimeout=10000
            connection.setRequestProperty("Accept","application/json")
            if(token!=null)connection.setRequestProperty("Authorization","Bearer $token")
            if(payload!=null){connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.outputStream.use{it.write(payload.toString().toByteArray(Charsets.UTF_8))}}
            val status=connection.responseCode
            if(status !in 200..299)throw ClassroomHttpError(status)
            val text=connection.inputStream.bufferedReader().use{it.readText()}
            return JSONObject(text)
        }finally{connection.disconnect()}
    }

    fun report(context:Context):JSONObject{
        val p=context.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
        val today=LocalDate.now()
        val dates=p.getString("studiedDates","").orEmpty().split(",").mapNotNull{runCatching{LocalDate.parse(it)}.getOrNull()}.filter{!it.isAfter(today)}.distinct()
        val totals=p.getString("skillTotal","").orEmpty().split(",").mapNotNull{it.toIntOrNull()}
        val correct=p.getString("skillCorrect","").orEmpty().split(",").mapNotNull{it.toIntOrNull()}
        val completed=p.getString("completed","").orEmpty().split(",").mapNotNull{it.toIntOrNull()}.filter{it in 0..299}.toSet()
        return JSONObject().put("format","LinguaFreeStudyReport").put("version",1)
            .put("generatedAt",OffsetDateTime.now().toString())
            .put("student",p.getString("learnerName","").orEmpty().trim().ifBlank{"Ученик"})
            .put("learnerId",learnerId(context)).put("completedLessons",completed.size).put("totalLessons",300)
            .put("xp",p.getInt("xp",0)).put("grammarTopicsCompleted",(p.getStringSet("grammarMastered",emptySet())?.size?:0)+(p.getStringSet("beginnerMastered",emptySet())?.size?:0))
            .put("studyDaysLast7",dates.count{!it.isBefore(today.minusDays(6))})
            .put("studyDaysLast30",dates.count{!it.isBefore(today.minusDays(29))})
            .put("lastStudyDate",dates.maxOrNull()?.toString()?:"")
            .put("skills",JSONArray((0..3).map{i->val attempts=totals.getOrElse(i){0}.coerceIn(0,1000000)
                JSONObject().put("name",listOf("Чтение и перевод","Аудирование","Письмо","Говорение")[i])
                    .put("attempts",attempts).put("correct",correct.getOrElse(i){0}.coerceIn(0,attempts))}))
    }

    fun learnerId(context:Context):String=synchronized(stateLock){
        val p=context.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
        p.getString("learnerId",null)?:UUID.randomUUID().toString().also{p.edit().putString("learnerId",it).commit()}
    }

    private fun async(block:()->JSONObject,callback:(Result<JSONObject>)->Unit){
        executor.execute{val result=runCatching(block);Handler(Looper.getMainLooper()).post{callback(result)}}
    }

    fun createClassroom(callback:(Result<JSONObject>)->Unit)=async({request("/teacher/create","POST",JSONObject())},callback)

    fun join(context:Context,code:String,callback:(Result<JSONObject>)->Unit){
        async({
            val p=prefs(context)
            check(!p.getBoolean("revokePending",false)){"Сначала дождитесь удаления прежнего подключения"}
            val response=request("/student/join","POST",JSONObject().put("code",code.trim())
                .put("name",report(context).getString("student")).put("consentVersion",1).put("consentConfirmed",true))
            check(response.optInt("protocol")==2){"Обновите сервер до протокола 2"}
            synchronized(stateLock){check(p.edit().clear().putString("studentId",response.getString("studentId"))
                .putString("studentToken",response.getString("studentToken")).putBoolean("enabled",true).putInt("consentVersion",1).commit())}
            queue(context)
            response
        },callback)
    }

    fun queue(context:Context){
        if(!configured)return
        val app=context.applicationContext
        synchronized(stateLock){
            val p=prefs(app)
            if(!p.getBoolean("enabled", false)||p.getBoolean("revokePending",false)||p.getBoolean("blocked",false))return
            val revision=p.getLong("revision",0)+1
            val payload=JSONObject().put("studentId",p.getString("studentId","")).put("revision",revision)
                .put("snapshotId",UUID.randomUUID().toString().replace("-","")).put("report",report(app))
            check(p.edit().putLong("revision",revision).putString("pending",payload.toString()).commit())
        }
        schedule(app)
    }

    private fun schedule(context:Context){
        val manager=WorkManager.getInstance(context)
        val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        manager.enqueueUniqueWork("classroom-send",ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ClassroomSendWorker>().setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build())
        manager.enqueueUniquePeriodicWork("classroom-retry",ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ClassroomSendWorker>(15,TimeUnit.MINUTES).setConstraints(constraints).build())
    }

    fun syncOnOpen(context:Context,callback:(Result<JSONObject>)->Unit={}){
        val p=prefs(context)
        if(!configured){callback(Result.failure(IllegalStateException("Сервер синхронизации ещё не подключён")));return}
        if(p.getBoolean("enabled",false))queue(context)
        if(!p.getBoolean("enabled",false)&&!p.getBoolean("revokePending",false)){
            callback(Result.failure(IllegalStateException("Синхронизация отключена")));return
        }
        async({flushPending(context.applicationContext)},callback)
    }

    fun flushPending(context:Context):JSONObject=flushLock.withLock{
        val p=prefs(context)
        repeat(4){
            val state=synchronized(stateLock){Triple(p.getBoolean("revokePending",false),p.getBoolean("enabled",false)&&!p.getBoolean("blocked",false),p.getString("pending",null))}
            val id=p.getString("studentId","").orEmpty();val token=p.getString("studentToken","").orEmpty()
            if(state.first){
                val result=if(id.isBlank()||token.isBlank())JSONObject().put("ok",true).put("localOnly",true)
                    else request("/student/revoke","POST",JSONObject().put("studentId",id),token)
                synchronized(stateLock){p.edit().clear().putBoolean("enabled",false).putString("status","Подключение удалено").commit()}
                WorkManager.getInstance(context).cancelUniqueWork("classroom-retry")
                return@withLock result
            }
            if(!state.second||state.third==null)return@withLock JSONObject().put("ok",true).put("idle",true)
            val payload=JSONObject(state.third!!)
            try{
                val result=request("/student/sync","POST",payload,token)
                synchronized(stateLock){
                    val editor=p.edit().putLong("lastSyncedAt",System.currentTimeMillis()).putString("status","Прогресс доставлен")
                        .putLong("revision",maxOf(p.getLong("revision",0),result.optLong("acceptedRevision",0)))
                    if(p.getString("pending",null)==state.third)editor.remove("pending")
                    check(editor.commit())
                }
            }catch(error:Exception){
                val permanent=error is ClassroomHttpError&&error.status in listOf(400,401,403,409)
                synchronized(stateLock){p.edit().putString("status",if(permanent)"Передача приостановлена: ${error.message}" else "Нет связи. Отчёт сохранён для повторной отправки.")
                    .putBoolean("blocked",permanent).commit()}
                throw error
            }
        }
        throw IllegalStateException("Есть более свежий отчёт, повторим отправку")
    }

    fun fetchClassroomBlocking(context:Context):JSONObject{
        val p=context.getSharedPreferences("linguafree_teacher",Context.MODE_PRIVATE)
        val id=p.getString("classroomId","").orEmpty();val token=p.getString("teacherToken","").orEmpty()
        check(id.isNotBlank()&&token.isNotBlank()){ "Сначала создайте кабинет" }
        val reports=JSONArray();var cursor=""
        repeat(3){
            val response=request("/teacher/reports?classroomId=$id"+(if(cursor.isBlank())"" else "&cursor="+URLEncoder.encode(cursor,"UTF-8")),"GET",token=token)
            val page=response.optJSONArray("reports")?:JSONArray()
            for(i in 0 until page.length())reports.put(page.getJSONObject(i))
            if(!response.optBoolean("more",false))return JSONObject().put("reports",reports)
            cursor=response.getString("cursor")
        }
        throw IllegalStateException("Слишком много страниц кабинета")
    }

    fun fetchClassroom(context:Context,callback:(Result<JSONObject>)->Unit)=async({fetchClassroomBlocking(context)},callback)

    fun renewCode(context:Context,callback:(Result<JSONObject>)->Unit){
        val p=context.getSharedPreferences("linguafree_teacher",Context.MODE_PRIVATE)
        async({request("/teacher/code","POST",JSONObject().put("classroomId",p.getString("classroomId","")),p.getString("teacherToken",""))},callback)
    }

    fun removeStudent(context:Context,studentId:String,callback:(Result<JSONObject>)->Unit){
        val p=context.getSharedPreferences("linguafree_teacher",Context.MODE_PRIVATE)
        async({request("/teacher/remove","POST",JSONObject().put("classroomId",p.getString("classroomId","")).put("studentId",studentId),p.getString("teacherToken",""))},callback)
    }

    fun revoke(context:Context,callback:(Result<JSONObject>)->Unit){
        // Disable transmission before any network operation; failed deletion remains durable and retries.
        synchronized(stateLock){prefs(context).edit().putBoolean("enabled",false).putBoolean("revokePending",true)
            .putBoolean("blocked",false).remove("pending").putString("status","Передача остановлена. Удаление ожидает сеть.").commit()}
        schedule(context.applicationContext)
        async({flushPending(context.applicationContext)},callback)
    }

    fun disable(context:Context){synchronized(stateLock){prefs(context).edit().putBoolean("enabled",false).remove("pending").commit()}}
}

class ClassroomSendWorker(context:Context,parameters:WorkerParameters):Worker(context,parameters){
    override fun doWork():Result=try{ClassroomSync.flushPending(applicationContext);Result.success()}
        catch(error:ClassroomHttpError){if(error.status in listOf(400,401,403,409))Result.failure() else Result.retry()}
        catch(_:Exception){Result.retry()}
}
