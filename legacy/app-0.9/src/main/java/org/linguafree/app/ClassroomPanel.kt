package org.linguafree.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Composable
private fun preferenceVersion(prefs:SharedPreferences):Int{
    var version by remember{mutableIntStateOf(0)}
    DisposableEffect(prefs){
        val listener=SharedPreferences.OnSharedPreferenceChangeListener{_,_->version++}
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    return version
}
private fun dateLabel(value:Long):String=if(value==0L)"ещё не было" else
    DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value))
private fun dateLabel(value:String):String=runCatching{dateLabel(Instant.parse(value).toEpochMilli())}.getOrDefault("ещё не было")

@Composable
fun StudentSyncPanel(context:Context){
    val prefs=remember{context.getSharedPreferences("linguafree_sync",Context.MODE_PRIVATE)}
    val version=preferenceVersion(prefs)
    val enabled=remember(version){prefs.getBoolean("enabled",false)}
    val deleting=remember(version){prefs.getBoolean("revokePending",false)}
    val paired=remember(version){!prefs.getString("studentId",null).isNullOrBlank()}
    var code by remember{mutableStateOf("")}
    var consent by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)}
    var status by remember{mutableStateOf("")}
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("🔗 Связь с преподавателем",style=MaterialTheme.typography.titleMedium)
            Text("После разрешения передаются псевдоним, уроки, XP, даты занятий, навыки и количество освоенных тем грамматики. Это отчёт, а не резервная копия телефона.")
            if(!ClassroomSync.configured)Text("Сервер пока не подключён. Можно сохранить и отправить отчёт файлом.")
            if(enabled||paired||deleting){
                Text(if(deleting)"Передача остановлена. Удаление ожидает подтверждения сервера." else if(enabled)"Синхронизация разрешена" else "Передача отключена")
                Text("Последняя доставка: ${dateLabel(prefs.getLong("lastSyncedAt",0))}")
                if(prefs.contains("pending"))Text("Есть сохранённый отчёт. Android повторит отправку при доступной сети.")
                Text(prefs.getString("status","").orEmpty())
                if(enabled&&!deleting)Button(enabled=!busy&&ClassroomSync.configured,onClick={
                    busy=true;ClassroomSync.syncOnOpen(context){r->busy=false;status=r.fold({prefs.getString("status","Готово").orEmpty()},{it.message.orEmpty()})}
                }){Text("Отправить сейчас")}
                OutlinedButton(enabled=!busy,onClick={
                    busy=true;ClassroomSync.revoke(context){r->busy=false;status=r.fold({"Подключение удалено"},{"Передача остановлена; удаление повторится при появлении сети. ${it.message.orEmpty()}"})}
                }){Text(if(deleting)"Повторить удаление" else "Остановить передачу и удалить данные с сервера")}
            }else if(ClassroomSync.configured){
                OutlinedTextField(value=code,onValueChange={code=it.filter{c->c.isLetterOrDigit()}.uppercase().take(16)},label={Text("Код преподавателя")},singleLine=true,modifier=Modifier.fillMaxWidth())
                Row{
                    Checkbox(checked=consent,onCheckedChange={consent=it},enabled=!busy)
                    Text("Я совершеннолетний ученик или законный представитель. Я разрешаю передачу перечисленных данных преподавателю и ознакомлен с правилами обработки. Разрешение можно отозвать.",modifier=Modifier.weight(1f))
                }
                Text("Сервер автоматически удаляет неактивные учебные записи через 90 дней. Не вводите полное имя, если достаточно псевдонима.")
                Button(enabled=!busy&&consent&&code.matches(Regex("[A-F0-9]{16}")),onClick={
                    busy=true;ClassroomSync.join(context,code){r->busy=false;status=r.fold({"Подключено. Первый отчёт сохранён для отправки."},{"Подключение не выполнено: ${it.message}"})}
                }){Text("Разрешить передачу и подключиться")}
            }
            if(status.isNotBlank())Text(status)
        }
    }
}

private val notificationLock=Any()
private fun notifyTeacher(context:Context,reports:JSONArray){
    val p=context.getSharedPreferences("linguafree_teacher",Context.MODE_PRIVATE)
    if(!p.getBoolean("backgroundWatch",false))return
    synchronized(notificationLock){
        val latest=(0 until reports.length()).map{reports.getJSONObject(it).optString("openedAt")}.maxOrNull().orEmpty()
        val previous=p.getString("lastNotifiedAt",null)
        p.edit().putString("lastNotifiedAt",maxOf(latest,previous.orEmpty())).apply()
        if(previous==null||latest<=previous)return
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("teacher_progress","Прогресс учеников",NotificationManager.IMPORTANCE_DEFAULT))
        val notification=NotificationCompat.Builder(context,"teacher_progress").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("LinguaFree: обновился прогресс учеников").setContentText("Откройте кабинет, чтобы посмотреть отчёты.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).build()
        manager.notify(200,notification)
    }
}

private fun scheduleTeacher(context:Context,enabled:Boolean){
    val manager=WorkManager.getInstance(context)
    if(!enabled){manager.cancelUniqueWork("teacher-watch");return}
    manager.enqueueUniquePeriodicWork("teacher-watch",ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<TeacherPollingWorker>(15,TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
}

class TeacherPollingWorker(context:Context,parameters:WorkerParameters):Worker(context,parameters){
    override fun doWork():Result{
        val p=applicationContext.getSharedPreferences("linguafree_teacher",Context.MODE_PRIVATE)
        if(!p.getBoolean("backgroundWatch",false)||!ClassroomSync.configured)return Result.success()
        return try{
            val response=ClassroomSync.fetchClassroomBlocking(applicationContext)
            p.edit().putString("cloudReports",response.getJSONArray("reports").toString()).putLong("cloudFetchedAt",System.currentTimeMillis()).apply()
            notifyTeacher(applicationContext,response.getJSONArray("reports"));Result.success()
        }catch(error:ClassroomHttpError){if(error.status in listOf(400,401,403))Result.failure() else Result.retry()}
        catch(_:Exception){Result.retry()}
    }
}

@Composable
fun TeacherSyncPanel(context:Context){
    val prefs=remember{context.getSharedPreferences("linguafree_teacher",Context.MODE_PRIVATE)}
    val version=preferenceVersion(prefs)
    val code=remember(version){prefs.getString("classroomCode","").orEmpty()}
    val hasClass=remember(version){!prefs.getString("classroomId",null).isNullOrBlank()}
    val background=remember(version){prefs.getBoolean("backgroundWatch",false)}
    var status by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var query by remember{mutableStateOf("")}
    var removing by remember{mutableStateOf<JSONObject?>(null)}
    val entries=remember(version){runCatching{val raw=JSONArray(prefs.getString("cloudReports","[]"));(0 until raw.length()).map{raw.getJSONObject(it)}}.getOrDefault(emptyList())}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted){prefs.edit().putBoolean("backgroundWatch",true).apply();scheduleTeacher(context,true)}
        else status="Уведомления не разрешены. Отчёты доступны в кабинете."
    }
    fun refresh(){
        if(busy)return
        busy=true
        ClassroomSync.fetchClassroom(context){r->busy=false;r.onSuccess{response->
            prefs.edit().putString("cloudReports",response.getJSONArray("reports").toString()).putLong("cloudFetchedAt",System.currentTimeMillis()).apply()
            notifyTeacher(context,response.getJSONArray("reports"));status="Отчёты обновлены"
        }.onFailure{status="Не удалось обновить: ${it.message}. Сохранённые отчёты остаются доступны."}}
    }
    LaunchedEffect(hasClass){
        if(hasClass&&ClassroomSync.configured)while(true){
            val lifecycle=(context as? LifecycleOwner)?.lifecycle
            if(lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED)!=false)refresh()
            delay(60_000L)
        }
    }
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("☁️ Кабинет и синхронизация",style=MaterialTheme.typography.titleLarge)
            if(!ClassroomSync.configured)Text("Сервер пока не подключён. Ниже доступны ручные отчёты.")
            else if(!hasClass){
                Text("Создание кабинета должно быть разрешено владельцем настроенного сервера. Код передавайте лично; данные несовершеннолетних — только после необходимых согласий.")
                Button(enabled=!busy,onClick={busy=true;ClassroomSync.createClassroom{r->busy=false;r.onSuccess{
                    prefs.edit().putString("classroomId",it.getString("classroomId")).putString("teacherToken",it.getString("teacherToken"))
                        .putString("classroomCode",it.getString("code")).putString("codeExpires",it.getString("expiresAt")).apply()
                    status="Кабинет создан"
                }.onFailure{status=it.message.orEmpty()}}}){Text("Создать кабинет")}
            }else{
                Text("Код подключения: $code",style=MaterialTheme.typography.titleMedium)
                Text("Действует до: ${dateLabel(prefs.getString("codeExpires","").orEmpty())}")
                OutlinedButton(enabled=!busy,onClick={busy=true;ClassroomSync.renewCode(context){r->busy=false;r.onSuccess{
                    prefs.edit().putString("classroomCode",it.getString("code")).putString("codeExpires",it.getString("expiresAt")).apply();status="Новый код готов. Прежний больше не принимает учеников."
                }.onFailure{status=it.message.orEmpty()}}}){Text("Получить новый код на 24 часа")}
                Button(enabled=!busy,onClick={refresh()}){Text(if(busy)"Загрузка…" else "Обновить отчёты")}
                Row{Switch(checked=background,onCheckedChange={enabled->
                    if(enabled&&Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else{prefs.edit().putBoolean("backgroundWatch",enabled).apply();scheduleTeacher(context,enabled)}
                });Text("Проверять прогресс в фоне",modifier=Modifier.weight(1f))}
                Text("В открытом кабинете — раз в минуту. В фоне — периодическая проверка Android, обычно не чаще раза в 15 минут; система может отложить её. Это не мгновенные push-уведомления.")
                Text("Последняя загрузка: ${dateLabel(prefs.getLong("cloudFetchedAt",0))}")
                Text("Подключений: ${entries.size} · с отчётами: ${entries.count{!it.optBoolean("pending",false)}}")
                OutlinedTextField(value=query,onValueChange={query=it},label={Text("Найти ученика")},singleLine=true,modifier=Modifier.fillMaxWidth())
                entries.filter{it.optJSONObject("report")?.optString("student").orEmpty().contains(query,true)}
                    .sortedByDescending{it.optString("openedAt")}.forEach{entry->
                        val r=entry.optJSONObject("report")?:JSONObject()
                        HorizontalDivider()
                        Text("👤 ${r.optString("student","Ученик")} · ${entry.optString("studentId").takeLast(6)}",style=MaterialTheme.typography.titleMedium)
                        if(entry.optBoolean("pending",false))Text("Подключён ${dateLabel(entry.optString("joinedAt"))}. Первый отчёт ещё не доставлен.")
                        else{
                            Text("Отчёт получен: ${dateLabel(entry.optString("openedAt"))}")
                            Text("Уроки: ${r.optInt("completedLessons")}/300 · XP: ${r.optInt("xp")} · грамматика: ${r.optInt("grammarTopicsCompleted")} тем")
                            Text("Дней занятий: ${r.optInt("studyDaysLast7")} за неделю · ${r.optInt("studyDaysLast30")} за месяц")
                            if(!entry.isNull("previousXp")){val delta=r.optInt("xp")-entry.optInt("previousXp");Text("Изменение XP: ${if(delta>=0)"+" else ""}$delta")}
                            val skills=r.optJSONArray("skills")?:JSONArray()
                            for(i in 0 until skills.length()){val skill=skills.getJSONObject(i);Text("${skill.optString("name")}: ${skill.optInt("correct")}/${skill.optInt("attempts")}")}
                        }
                        OutlinedButton(enabled=!busy,onClick={removing=entry}){Text("Отключить ученика и удалить отчёт")}
                    }
            }
            if(status.isNotBlank())Text(status)
        }
    }
    removing?.let{entry->AlertDialog(onDismissRequest={removing=null},title={Text("Отключить ученика?")},text={Text("Сервер удалит отчёт и отзовёт доступ. Учебный прогресс на телефоне ученика сохранится.")},
        confirmButton={TextButton(onClick={removing=null;busy=true;ClassroomSync.removeStudent(context,entry.getString("studentId")){r->busy=false;r.onSuccess{refresh()}.onFailure{status=it.message.orEmpty()}}}){Text("Отключить")}},
        dismissButton={TextButton(onClick={removing=null}){Text("Отмена")}})}
}
