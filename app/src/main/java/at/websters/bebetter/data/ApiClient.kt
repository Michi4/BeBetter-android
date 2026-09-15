package at.websters.bebetter.data

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class ChatResult(val reply: String, val sessionId: String?, val error: String? = null)

object ApiClient {
    @Volatile private var api: BeBetterApi? = null
    @Volatile private var baseUrl: String = SessionManager.DEFAULT_BASE_URL
    @Volatile private var tokenProvider: (() -> String?)? = null
    @Volatile private var http: OkHttpClient? = null

    fun init(context: Context, session: SessionManager) {
        tokenProvider = { runBlocking { runCatching { session.getToken() }.getOrNull() } }
        runBlocking { runCatching { baseUrl = session.getBaseUrl() }.getOrNull() }
    }

    fun setBaseUrl(url: String) {
        val clean = url.trimEnd('/')
        if (clean != baseUrl) {
            baseUrl = clean
            api = null
        }
    }

    fun get(): BeBetterApi {
        return api ?: synchronized(this) {
            api ?: build().also { api = it }
        }
    }

    fun invalidate() { api = null }

    private fun build(): BeBetterApi {
        val gson = GsonBuilder().setLenient().create()
        val auth = Interceptor { chain ->
            val token = tokenProvider?.invoke()
            val req = if (!token.isNullOrBlank()) {
                chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else chain.request()
            chain.proceed(req)
        }
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // AI chat can be slow
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("$baseUrl/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(BeBetterApi::class.java)
    }

    fun uploadsBase(): String = baseUrl

    // SSE streaming chat like web Assistant.vue: onDelta for live text, onDone(fullReply, sessionId)
    fun chatStream(
        messages: List<Map<String, String>>,
        sessionId: String?,
        onDelta: (String) -> Unit,
        onDone: (String, String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val payload = mutableMapOf<String, Any?>("messages" to messages.map { mapOf("role" to it["role"]!!, "content" to it["content"]!!) })
        sessionId?.let { payload["sessionId"] = it }
        val json = org.json.JSONObject(payload as Map<*, *>).toString()
        val req = okhttp3.Request.Builder()
            .url("$baseUrl/api/assistant/chat")
            .post(json.toRequestBody("application/json".toMediaType()))
            .apply { tokenProvider?.invoke()?.let { addHeader("Authorization", "Bearer $it") } }
            .build()
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                onError("Connection problem — check your internet and try again.")
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { res ->
                    if (!res.isSuccessful) {
                        val body = runCatching { res.body?.string() }.getOrNull()
                        val msg = runCatching { org.json.JSONObject(body ?: "{}").optString("error") }.getOrNull()
                        onError(when (res.code) {
                            403 -> msg ?: "Not available for this account."
                            429 -> "Slow down a little — try again in a minute."
                            else -> msg ?: "Something went wrong on our side — try again."
                        })
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
                                        "delta" -> obj.optString("text").let { if (it.isNotEmpty()) { full += it; onDelta(it) } }
                                        "done" -> {
                                            if (obj.has("reply") && obj.getString("reply").isNotEmpty()) full = obj.getString("reply")
                                            sid = obj.optString("sessionId").ifBlank { null }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    onDone(full.ifBlank { "Done." }, sid)
                }
            }
        })
    }

    fun resolveUpload(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path
        return "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
    }
}
