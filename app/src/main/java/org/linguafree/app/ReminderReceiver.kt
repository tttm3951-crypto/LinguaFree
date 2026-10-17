package org.linguafree.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class ReminderReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action==Intent.ACTION_BOOT_COMPLETED){schedule(context);return}
  val prefs=context.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE)
  if(prefs.getInt("reminders",3)==0)return
  val today=java.time.LocalDate.now().toString()
  val studied=prefs.getString("studiedDates","").orEmpty().split(",")
  if(today in studied)return
  val slot=intent.getIntExtra("slot",0)
  if(prefs.getInt("reminders",3)==2 && slot==1)return
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("study","Напоминания об английском",NotificationManager.IMPORTANCE_DEFAULT))
  val launch=context.packageManager.getLaunchIntentForPackage(context.packageName)
  val open=PendingIntent.getActivity(context,0,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val messages=listOf("Доброе утро! Лисичка ждёт тебя на уроке 🦊","Пять минут английского — отличная идея! 🌟","Вечерняя практика поможет закрепить слова! 📚")
  val notification=NotificationCompat.Builder(context,"study").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("LinguaFree · время учиться!").setContentText(messages[slot.coerceIn(0,2)]).setAutoCancel(true).setContentIntent(open).build()
  try{manager.notify(100+slot,notification)}catch(_:SecurityException){}
 }
 companion object{
  fun schedule(context:Context){
   val count=context.getSharedPreferences("linguafree_progress",Context.MODE_PRIVATE).getInt("reminders",3)
   val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
   for(slot in 0..2){
    val intent=Intent(context,ReminderReceiver::class.java).putExtra("slot",slot)
    val pending=PendingIntent.getBroadcast(context,100+slot,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    alarm.cancel(pending)
    if(count==0 || (count==2 && slot==1))continue
    val hour=listOf(9,14,19)[slot]
    val whenTo=Calendar.getInstance().apply{set(Calendar.HOUR_OF_DAY,hour);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);if(timeInMillis<=System.currentTimeMillis())add(Calendar.DAY_OF_YEAR,1)}
    alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,whenTo.timeInMillis,AlarmManager.INTERVAL_DAY,pending)
   }
  }
 }
}
