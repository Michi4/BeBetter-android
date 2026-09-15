package at.websters.bebetter.wear

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

private val Context.wearStore by preferencesDataStore(name = "bebetter_wear")

class WearSession(private val ctx: Context) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_BASE = stringPreferencesKey("base_url")
        private val KEY_SID = stringPreferencesKey("assistant_session")
        const val DEFAULT_BASE = "https://app.bebetter.websters.at"
    }
    suspend fun token(): String? = ctx.wearStore.data.map { it[KEY_TOKEN] }.first()
    suspend fun saveToken(t: String) { ctx.wearStore.edit { it[KEY_TOKEN] = t } }
    suspend fun clear() { ctx.wearStore.edit { it.remove(KEY_TOKEN); it.remove(KEY_SID) } }
    suspend fun base(): String = ctx.wearStore.data.map { it[KEY_BASE] ?: DEFAULT_BASE }.first()
    suspend fun saveBase(u: String) { ctx.wearStore.edit { it[KEY_BASE] = u } }
    suspend fun sessionId(): String? = ctx.wearStore.data.map { it[KEY_SID] }.first()
    suspend fun saveSessionId(id: String) { ctx.wearStore.edit { it[KEY_SID] = id } }
}

// Minimal API surface the watch needs (same /api as phone/web).
data class WearUser(val id: String = "", val username: String = "", val role: String = "user", val isDemo: Boolean = false)
data class WearAuth(val token: String = "", val user: WearUser? = null)
data class WearHabit(
    val id: String = "",
    val title: String = "",
    val emoji: String = "",
    val scheduledTime: String? = null,
    val completedToday: Boolean? = null,
    val bestStreak: Int = 0
)
data class WearHabits(val habits: List<WearHabit> = emptyList())
data class WearStats(val activeStreak: Int = 0, val todayLogs: Int = 0, val consistency: Int = 0, val totalLogs: Int = 0)

interface WearApi {
    @POST("auth/login") suspend fun login(@Body b: Map<String, String>): WearAuth
    @GET("auth/me") suspend fun me(): Map<String, WearUser>
    @GET("habits/scheduled") suspend fun scheduled(@Query("date") d: String? = null): WearHabits
    @POST("logs") suspend fun complete(@Body b: Map<String, String?>): Map<String, Any>
    @DELETE("logs/habit/{habitId}")
    suspend fun undo(
        @Path("habitId") habitId: String,
        @Query("scheduledTime") scheduledTime: String? = null,
        @Query("date") date: String? = null
    ): Map<String, String>
    @GET("stats/overview") suspend fun stats(): WearStats
}

object WearClient {
    @Volatile private var api: WearApi? = null
    @Volatile private var base: String = WearSession.DEFAULT_BASE
    @Volatile private var token: (() -> String?)? = null

    fun init(baseUrl: String, tokenProvider: () -> String?) {
        if (baseUrl != base) { base = baseUrl.trimEnd('/'); api = null }
        token = tokenProvider
    }

    fun get(): WearApi = api ?: synchronized(this) {
        api ?: run {
            val gson = GsonBuilder().setLenient().create()
            val auth = Interceptor { c ->
                val t = token?.invoke()
                val r = if (!t.isNullOrBlank()) c.request().newBuilder().addHeader("Authorization", "Bearer $t").build() else c.request()
                c.proceed(r)
            }
            val client = OkHttpClient.Builder().addInterceptor(auth)
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
            Retrofit.Builder().baseUrl("$base/api/").client(client)
                .addConverterFactory(GsonConverterFactory.create(gson)).build()
                .create(WearApi::class.java).also { api = it }
        }
    }

    fun reset() { api = null }

    // SSE streaming chat — same contract as the web Assistant / phone ApiClient.chatStream.
    fun chatStream(
        messages: List<Pair<String, String>>,
        sessionId: String?,
        onDelta: (String) -> Unit,
        onDone: (String, String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val payload = org.json.JSONObject()
        val arr = org.json.JSONArray()
        messages.forEach { arr.put(org.json.JSONObject().put("role", it.first).put("content", it.second)) }
        payload.put("messages", arr)
        sessionId?.let { payload.put("sessionId", it) }
        val req = Request.Builder()
            .url("$base/api/assistant/chat")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .apply { token?.invoke()?.let { addHeader("Authorization", "Bearer $it") } }
            .build()
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runCatching {
                    onError("Connection problem — check your internet and try again.")
                }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { res ->
                    if (!res.isSuccessful) {
                        val body = runCatching { res.body?.string() }.getOrNull()
                        val msg = runCatching { org.json.JSONObject(body ?: "{}").optString("error") }.getOrNull()
                        runCatching {
                            onError(when (res.code) {
                                403 -> msg?.takeIf { it.isNotBlank() } ?: "Sign up to use the assistant."
                                429 -> "Slow down a little — try again in a minute."
                                else -> "Something went wrong — try again."
                            })
                        }
                        return
                    }
                    val src = res.body?.source() ?: return
                    var full = ""
                    var sid: String? = null
                    var event = ""
                    while (true) {
                        val line = runCatching { src.readUtf8Line() }.getOrNull() ?: break
                        when {
                            line.startsWith("event:") -> event = line.substring(6).trim()
                            line.startsWith("data:") -> {
                                val data = line.substring(5).trim()
                                if (data.isNotEmpty()) {
                                    val obj = runCatching { org.json.JSONObject(data) }.getOrNull() ?: continue
                                    when (event) {
                                        "delta" -> obj.optString("text").let {
                                            if (it.isNotEmpty()) { full += it; runCatching { onDelta(it) } }
                                        }
                                        "done" -> {
                                            if (obj.has("reply") && obj.getString("reply").isNotEmpty()) full = obj.getString("reply")
                                            sid = obj.optString("sessionId").ifBlank { null }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    runCatching { onDone(full.ifBlank { "Done." }, sid) }
                }
            }
        })
    }

    fun currentToken(): String? = runCatching { token?.invoke() }.getOrNull()
}
