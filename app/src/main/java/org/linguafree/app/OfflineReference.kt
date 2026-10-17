package org.linguafree.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

data class ReferenceEntry(val id:Long,val en:String,val ru:String,val credit:String)

object OfflineReferenceStore {
    private const val NAME="offline-reference-v1.db"
    @Synchronized fun open(context:Context):SQLiteDatabase {
        val file=context.getDatabasePath(NAME)
        if(!file.exists()) {
            file.parentFile?.mkdirs()
            val temporary=File(file.path+".tmp")
            context.assets.open("offline-reference.db").use { input -> temporary.outputStream().use { input.copyTo(it) } }
            check(temporary.renameTo(file)){"Не удалось подготовить справочник"}
        }
        return SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READONLY)
    }
    fun find(context:Context,query:String,page:Int):List<ReferenceEntry> {
        val words=Regex("[\\p{L}\\p{N}]+").findAll(query.take(200)).take(8).map { it.value+"*" }.toList()
        if(words.isEmpty())return emptyList()
        return open(context).use { db ->
            db.rawQuery("SELECT e.id,e.en,e.ru,e.credit FROM search JOIN entries e ON e.id=search.docid WHERE search MATCH ? ORDER BY e.id LIMIT 51 OFFSET ?",arrayOf(words.joinToString(" AND "),(page.coerceIn(0,3000)*50).toString())).use { c ->
                buildList { while(c.moveToNext())add(ReferenceEntry(c.getLong(0),c.getString(1),c.getString(2),c.getString(3))) }
            }
        }
    }
}

/** Hosted by the app's LazyColumn: results stay bounded; no 150k JSON objects on the UI thread. */
@Composable fun OfflineReference(context:Context,onSpeak:(String)->Unit,onBack:()->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf(emptyList<ReferenceEntry>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(query,page) {
        rows=emptyList(); error=""; busy=true; delay(300)
        try { rows=withContext(Dispatchers.IO) { OfflineReferenceStore.find(context,query,page) } }
        catch(e:CancellationException) { throw e }
        catch(e:Exception) { error="Справочник недоступен. Проверьте целостность установки." }
        finally { busy=false }
    }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        TextButton(onClick=onBack){Text("‹ К учебному словарю")}
        Text("Офлайн-справочник · 150 000",style=MaterialTheme.typography.headlineSmall)
        Text("635 учебных записей и 149 365 пар предложений Tatoeba. Поиск на двух языках без интернета. Формы слов могут различаться; вводите начало слова. Это справочник, а не начальный курс.")
        Text("Корпус сообщества может содержать ошибки и взрослые темы. Он не используется для автоматической выдачи детских заданий. Фильтр не заменяет редакторскую проверку.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(value=query,onValueChange={query=it.take(200);page=0},label={Text("Слово или фраза: hello / привет")},singleLine=true,modifier=Modifier.fillMaxWidth())
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(error.isNotEmpty())Text(error)
        if(query.isBlank())Text("Начните поиск. Все записи уже в приложении; загрузка сети не нужна.")
        else if(!busy&&rows.isEmpty())Text("На этой странице совпадений нет. Попробуйте начало слова.")
        rows.take(50).forEach { row ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(row.en,style=MaterialTheme.typography.titleMedium); Text(row.ru)
                OutlinedButton(onClick={onSpeak(row.en)}){Text("🔊 Послушать")}
                Text(row.credit,style=MaterialTheme.typography.labelSmall)
            } }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={page--},enabled=page>0&&!busy){Text("Назад")}
            Text("${page+1}",modifier=Modifier.padding(top=15.dp))
            OutlinedButton(onClick={page++},enabled=rows.size>50&&!busy){Text("Дальше")}
        }
        Text("Tatoeba / ManyThings · CC BY 2.0 FR. Авторы и ID сохранены у каждой пары. https://tatoeba.org · https://www.manythings.org/anki/ · https://creativecommons.org/licenses/by/2.0/fr/",style=MaterialTheme.typography.bodySmall)
    }
}
