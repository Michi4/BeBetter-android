package at.websters.bebetter.data

import android.content.Context
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile private var api: BeBetterApi? = null
    @Volatile private var baseUrl: String = SessionManager.DEFAULT_BASE_URL
    @Volatile private var tokenProvider: (() -> String?)? = null

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
            .readTimeout(30, TimeUnit.SECONDS)
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
    fun resolveUpload(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path
        return "$baseUrl${if (path.startsWith("/")) path else "/$path"}"
    }
}
