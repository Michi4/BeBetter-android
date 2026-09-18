package at.websters.bebetter.wear

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit

private val Context.legacyWearStore by preferencesDataStore(name = "bebetter_wear")
private const val ENC_FILE = "bebetter_wear_enc.preferences_pb"

class WearSession(private val ctx: Context) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_BASE = stringPreferencesKey("base_url")
        private val KEY_SID = stringPreferencesKey("assistant_session")
        const val DEFAULT_BASE = "https://app.bebetter.websters.at"
    }

    private val appCtx = ctx.applicationContext

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(appCtx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    private val store: DataStore<Preferences> by lazy {
        androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            produceFile = {
                EncryptedFile.Builder(
                    appCtx,
                    File(appCtx.filesDir, "datastore/$ENC_FILE"),
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
            }
        )
    }

    @Volatile var cachedToken: String? = null
        private set

    /** One-shot migration from the legacy plaintext store. Safe to call every launch. */
    suspend fun ensureMigrated() {
        try {
            val legacyFile = appCtx.dataStoreFile("bebetter_wear.preferences_pb")
            val encFile = File(appCtx.filesDir, "datastore/$ENC_FILE")
            if (!legacyFile.exists() || encFile.exists()) return
            val old = appCtx.legacyWearStore.data.first()
            store.edit { e ->
                old[KEY_TOKEN]?.let { e[KEY_TOKEN] = it }
                old[KEY_BASE]?.let { e[KEY_BASE] = it }
                old[KEY_SID]?.let { e[KEY_SID] = it }
            }
            runCatching { legacyFile.delete() }
        } catch (_: Exception) { /* stay logged out rather than crash */ }
    }


    suspend fun token(): String? {
        cachedToken?.let { return it }
        return try {
            store.data.map { it[KEY_TOKEN] }.first()?.also { cachedToken = it }
        } catch (_: Exception) { null }
    }

    suspend fun saveToken(t: String) {
        cachedToken = t
        try { store.edit { it[KEY_TOKEN] = t } } catch (_: Exception) {}
    }

    suspend fun clear() {
        cachedToken = null
        try { store.edit { it.remove(KEY_TOKEN); it.remove(KEY_SID) } } catch (_: Exception) {}
    }

    suspend fun base(): String = try {
        store.data.map { it[KEY_BASE] ?: DEFAULT_BASE }.first()
    } catch (_: Exception) { DEFAULT_BASE }

    suspend fun saveBase(u: String) {
        try { store.edit { it[KEY_BASE] = u } } catch (_: Exception) {}
    }

    suspend fun sessionId(): String? = try {
        store.data.map { it[KEY_SID] }.first()
    } catch (_: Exception) { null }

    suspend fun saveSessionId(id: String) {
        try { store.edit { it[KEY_SID] = id } } catch (_: Exception) {}
    }
}
