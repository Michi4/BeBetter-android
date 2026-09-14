package at.websters.bebetter.wear

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

private val Context.wearStore by preferencesDataStore(name = "bebetter_wear")

class WearSession(private val ctx: Context) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        const val DEFAULT_BASE = "https://app.bebetter.websters.at"
        private val KEY_BASE = stringPreferencesKey("base_url")
    }
    suspend fun token(): String? = ctx.wearStore.data.map { it[KEY_TOKEN] }.first()
    suspend fun saveToken(t: String) { ctx.wearStore.edit { it[KEY_TOKEN] = t } }
    suspend fun clear() { ctx.wearStore.edit { it.remove(KEY_TOKEN) } }
    suspend fun base(): String = ctx.wearStore.data.map { it[KEY_BASE] ?: DEFAULT_BASE }.first()
    suspend fun saveBase(u: String) { ctx.wearStore.edit { it[KEY_BASE] = u } }
}

// Minimal API surface the watch needs (same /api as phone/web).
data class WearUser(val id: String = "", val username: String = "", val role: String = "user", val isDemo: Boolean = false)
data class WearAuth(val token: String = "", val user: WearUser? = null)
data class WearHabit(val id: String = "", val title: String = "", val emoji: String = "", val scheduledTime: String? = null, val completedToday: Boolean? = null, val bestStreak: Int = 0)
data class WearHabits(val habits: List<WearHabit> = emptyList())
data class WearStats(val activeStreak: Int = 0, val todayLogs: Int = 0, val consistency: Int = 0, val totalLogs: Int = 0)

interface WearApi {
    @POST("auth/login") suspend fun login(@Body b: Map<String, String>): WearAuth
    @GET("auth/me") suspend fun me(): Map<String, WearUser>
    @GET("habits/scheduled") suspend fun scheduled(@Query("date") d: String? = null): WearHabits
    @POST("logs") suspend fun complete(@Body b: Map<String, String?>): Map<String, Any>
    @GET("stats/overview") suspend fun stats(): WearStats
    @POST("assistant/chat") suspend fun chat(@Body b: Map<String, Any?>): Map<String, com.google.gson.JsonElement>
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
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
            Retrofit.Builder().baseUrl("$base/api/").client(client)
                .addConverterFactory(GsonConverterFactory.create(gson)).build()
                .create(WearApi::class.java).also { api = it }
        }
    }

    fun reset() { api = null }
}
