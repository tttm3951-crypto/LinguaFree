package org.linguafree.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Opt-in classroom sync. Configure a HTTPS endpoint before release.
 * No tracking, network traffic or enrollment occurs until the student opts in.
 */
object ClassroomSync {
    // Replace after deploying the accompanying Cloudflare Worker.
    val ENDPOINT: String = BuildConfig.CLASSROOM_ENDPOINT
    val configured: Boolean get() = ENDPOINT.startsWith("https://") && ENDPOINT.length > 15

    private fun request(path: String, method: String, payload: JSONObject? = null, token: String? = null): JSONObject {
        check(configured) { "Сервер синхронизации ещё не подключён" }
        val connection = URL(ENDPOINT.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            if (connection.responseCode !in 200..299) throw IllegalStateException("Сервер: HTTP ${connection.responseCode}")
            return JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    fun report(context: Context): JSONObject {
        val p = context.getSharedPreferences("linguafree_progress", Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val dates = p.getString("studiedDates", "").orEmpty().split(",")
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.distinct()
        val totals = p.getString("skillTotal", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }
        val correct = p.getString("skillCorrect", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }
        val completed = p.getString("completed", "").orEmpty().split(",").mapNotNull { it.toIntOrNull() }.toSet()
        return JSONObject()
            .put("format", "LinguaFreeStudyReport").put("version", 1)
            .put("generatedAt", OffsetDateTime.now().toString())
            .put("student", p.getString("learnerName", "").orEmpty().trim().ifBlank { "Ученик" })
            .put("completedLessons", completed.size).put("totalLessons", 300)
            .put("xp", p.getInt("xp", 0))
            .put("studyDaysLast7", dates.count { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) })
            .put("studyDaysLast30", dates.count { !it.isBefore(today.minusDays(29)) && !it.isAfter(today) })
            .put("lastStudyDate", dates.maxOrNull()?.toString() ?: "")
            .put("skills", JSONArray((0..3).map { i ->
                JSONObject().put("name", listOf("Чтение и перевод", "Аудирование", "Письмо", "Говорение")[i])
                    .put("attempts", totals.getOrElse(i) { 0 }).put("correct", correct.getOrElse(i) { 0 })
            }))
    }

    private fun async(block: () -> JSONObject, callback: (Result<JSONObject>) -> Unit) {
        Thread {
            val result = runCatching(block)
            Handler(Looper.getMainLooper()).post { callback(result) }
        }.start()
    }

    fun createClassroom(callback: (Result<JSONObject>) -> Unit) =
        async({ request("/teacher/create", "POST", JSONObject()) }, callback)

    fun join(context: Context, code: String, callback: (Result<JSONObject>) -> Unit) {
        val name = report(context).getString("student")
        async({
            request("/student/join", "POST", JSONObject().put("code", code.trim()).put("name", name))
        }) { result ->
            result.onSuccess {
                context.getSharedPreferences("linguafree_sync", Context.MODE_PRIVATE).edit()
                    .putString("studentId", it.getString("studentId"))
                    .putString("studentToken", it.getString("studentToken"))
                    .putBoolean("enabled", true).apply()
            }
            callback(result)
        }
    }

    fun syncOnOpen(context: Context, callback: (Result<JSONObject>) -> Unit = {}) {
        val p = context.getSharedPreferences("linguafree_sync", Context.MODE_PRIVATE)
        val studentId = p.getString("studentId", null)
        val token = p.getString("studentToken", null)
        if (!configured || !p.getBoolean("enabled", false) || studentId.isNullOrBlank() || token.isNullOrBlank()) return
        async({
            request("/student/sync", "POST", JSONObject().put("studentId", studentId).put("report", report(context)), token)
        }, callback)
    }

    fun fetchClassroom(context: Context, callback: (Result<JSONObject>) -> Unit) {
        val p = context.getSharedPreferences("linguafree_teacher", Context.MODE_PRIVATE)
        val id = p.getString("classroomId", "").orEmpty()
        val token = p.getString("teacherToken", "").orEmpty()
        if (id.isBlank() || token.isBlank()) { callback(Result.failure(IllegalStateException("Сначала создайте кабинет"))); return }
        async({ request("/teacher/reports?classroomId=$id", "GET", token = token) }, callback)
    }

    fun revoke(context: Context, callback: (Result<JSONObject>) -> Unit) {
        val p=context.getSharedPreferences("linguafree_sync",Context.MODE_PRIVATE)
        val id=p.getString("studentId","").orEmpty()
        val token=p.getString("studentToken","").orEmpty()
        if(!configured||id.isBlank()||token.isBlank()){
            disable(context)
            callback(Result.success(JSONObject().put("ok",true)))
            return
        }
        async({request("/student/revoke","POST",JSONObject().put("studentId",id),token)}){result->
            result.onSuccess {
                p.edit().remove("studentId").remove("studentToken").putBoolean("enabled",false).apply()
            }
            callback(result)
        }
    }

    fun disable(context: Context) {
        // Revocation is performed by the server on explicit request; clearing local credentials
        // only stops further automatic transmissions from this device.
        context.getSharedPreferences("linguafree_sync", Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", false).apply()
    }
}
