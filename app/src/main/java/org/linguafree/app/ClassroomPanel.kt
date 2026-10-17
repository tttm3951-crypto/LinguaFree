package org.linguafree.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun StudentSyncPanel(context: Context) {
    val prefs = remember { context.getSharedPreferences("linguafree_sync",Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled",false)) }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    Card(modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text("🔗 Связь с преподавателем",style=MaterialTheme.typography.titleMedium)
            Text("Только по вашему разрешению: при входе отправляются имя, уроки, XP, дата входа и результаты навыков. Можно отключить в любой момент.")
            if(!ClassroomSync.configured)Text("Сервер синхронизации пока не подключён. Используйте отправку отчёта по файлу.")
            else if(enabled){
                Text("Синхронизация разрешена")
                Button(onClick={
                    ClassroomSync.syncOnOpen(context){r->status=r.fold({"Прогресс отправлен"},{"Ошибка: ${it.message}"})}
                }){Text("Синхронизировать сейчас")}
                OutlinedButton(onClick={
                    ClassroomSync.revoke(context){r->
                        r.onSuccess{enabled=false;status="Доступ отозван, удалённые данные удалены."}
                            .onFailure{status="Ошибка удаления с сервера: ${it.message}"}
                    }
                }){Text("Отключить и удалить мои данные с сервера")}
            }else{
                OutlinedTextField(value=code,onValueChange={code=it.uppercase().take(16)},label={Text("Код от преподавателя")},singleLine=true)
                Button(enabled=code.matches(Regex("[A-F0-9]{16}")),onClick={
                    ClassroomSync.join(context,code){r->
                        r.onSuccess{
                            enabled=true
                            status="Подключено. Отправляем первый отчёт."
                            ClassroomSync.syncOnOpen(context){v->v.onFailure{status="Ошибка отправки: ${it.message}"}}
                        }.onFailure{status="Ошибка подключения: ${it.message}"}
                    }
                }){Text("Разрешить передачу прогресса")}
            }
            if(status.isNotBlank())Text(status)
        }
    }
}

@Composable
fun TeacherSyncPanel(context: Context) {
    val prefs=remember { context.getSharedPreferences("linguafree_teacher",Context.MODE_PRIVATE) }
    var code by remember { mutableStateOf(prefs.getString("classroomCode","").orEmpty()) }
    var hasClass by remember { mutableStateOf(!prefs.getString("classroomId","").isNullOrBlank()) }
    var status by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(emptyList<JSONObject>()) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    fun refresh(){
        ClassroomSync.fetchClassroom(context){r->
            r.onSuccess{response->
                val arr=response.optJSONArray("reports")?:JSONArray()
                entries=(0 until arr.length()).mapNotNull{arr.optJSONObject(it)}
                val last=prefs.getString("lastNotifiedAt","").orEmpty()
                val newer=entries.filter{it.optString("openedAt")>last}
                if(newer.isNotEmpty()){
                    val latest=newer.maxByOrNull{it.optString("openedAt")}!!
                    prefs.edit().putString("lastNotifiedAt",latest.optString("openedAt")).apply()
                    if(Build.VERSION.SDK_INT<33||context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
                        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("teacher_progress","Активность учеников",NotificationManager.IMPORTANCE_DEFAULT))
                        val report=latest.optJSONObject("report")?:JSONObject()
                        val notification=NotificationCompat.Builder(context,"teacher_progress")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("${report.optString("student","Ученик")} открыл LinguaFree")
                            .setContentText("Уроков: ${report.optInt("completedLessons")} · XP: ${report.optInt("xp")}")
                            .setAutoCancel(true).build()
                        manager.notify(200,notification)
                    }
                }
                status="Данные обновлены"
            }.onFailure{status="Ошибка загрузки: ${it.message}"}
        }
    }
    LaunchedEffect(hasClass){
        if(hasClass&&ClassroomSync.configured){
            while(true){refresh();delay(60_000L)}
        }
    }
    Card(modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text("☁️ Автоматическая синхронизация",style=MaterialTheme.typography.titleMedium)
            if(!ClassroomSync.configured)Text("Для подключения необходимо развернуть защищённый сервер. Ручные отчёты продолжают работать.")
            else if(!hasClass){
                Button(onClick={
                    ClassroomSync.createClassroom{r->
                        r.onSuccess{
                            prefs.edit().putString("classroomId",it.getString("classroomId"))
                                .putString("teacherToken",it.getString("teacherToken"))
                                .putString("classroomCode",it.getString("code")).apply()
                            code=it.getString("code");hasClass=true;status="Кабинет создан"
                        }.onFailure{status="Ошибка: ${it.message}"}
                    }
                }){Text("Создать кабинет и код подключения")}
            }else{
                Text("Код ученику: $code (действует 24 часа)")
                Text("Передавайте код лично. Ученик сам подтверждает подключение.")
                if(Build.VERSION.SDK_INT>=33)OutlinedButton(onClick={permission.launch(Manifest.permission.POST_NOTIFICATIONS)}){Text("Разрешить уведомления")}
                Button(onClick={refresh()}){Text("Обновить сейчас")}
                Text("Проверка раз в минуту, пока этот экран открыт. Фоновые push-уведомления требуют отдельной настройки Firebase.")
            }
            if(status.isNotBlank())Text(status)
            if(entries.isNotEmpty()){
                Text("Подключённых учеников: ${entries.size}",style=MaterialTheme.typography.titleMedium)
                entries.sortedByDescending{it.optString("openedAt")}.forEach{entry->
                    val r=entry.optJSONObject("report")?:JSONObject()
                    HorizontalDivider()
                    Text("👤 ${r.optString("student","Ученик")}",style=MaterialTheme.typography.titleMedium)
                    Text("Последний вход: ${entry.optString("openedAt").take(16).replace("T"," ")} UTC")
                    Text("Уроки: ${r.optInt("completedLessons")}/300 · XP: ${r.optInt("xp")}")
                    Text("Активность: ${r.optInt("studyDaysLast7")} дней за неделю")
                    val old=entry.optInt("previousXp",-1)
                    if(old>=0)Text("XP с прошлого входа: +${r.optInt("xp")-old}")
                }
            }
        }
    }
}
