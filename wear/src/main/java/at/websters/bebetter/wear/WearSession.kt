package at.websters.bebetter.wear

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.legacyWearStore by preferencesDataStore(name = "bebetter_wear")
private const val ENC_FILE = "bebetter_wear_enc"

class WearSession(private val ctx: Context) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_BASE = stringPreferencesKey("base_url")
        private val KEY_SID = stringPreferencesKey("assistant_session")
        const val DEFAULT_BASE = "https://app.bebetter.websters.at"
    }

    private val appCtx = ctx.applicationContext

    private val encPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appCtx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            appCtx, ENC_FILE, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Volatile var cachedToken: String? = null
        private set

    /** One-shot migration of a plaintext legacy token. Safe to call every launch. */
    suspend fun ensureMigrated() {
        try {
            if (encPrefs.contains("token")) return
            val old = appCtx.legacyWearStore.data.first()
            val token = old[KEY_TOKEN]
            if (token != null) {
                encPrefs.edit().putString("token", token).apply()
                appCtx.legacyWearStore.edit { it.remove(KEY_TOKEN) }
            }
        } catch (_: Exception) { /* stay logged out rather than crash */ }
    }

    suspend fun token(): String? {
        cachedToken?.let { return it }
        return try {
            encPrefs.getString("token", null)?.also { cachedToken = it }
        } catch (_: Exception) { null }
    }

    suspend fun saveToken(t: String) {
        cachedToken = t
        try { encPrefs.edit().putString("token", t).apply() } catch (_: Exception) {}
    }

    suspend fun clear() {
        cachedToken = null
        try { encPrefs.edit().remove("token").apply() } catch (_: Exception) {}
    }

    suspend fun base(): String = try {
        ctx.legacyWearStore.data.map { it[KEY_BASE] ?: DEFAULT_BASE }.first()
    } catch (_: Exception) { DEFAULT_BASE }

    suspend fun saveBase(u: String) {
        try { ctx.legacyWearStore.edit { it[KEY_BASE] = u } } catch (_: Exception) {}
    }

    suspend fun sessionId(): String? = try {
        ctx.legacyWearStore.data.map { it[KEY_SID] }.first()
    } catch (_: Exception) { null }

    suspend fun saveSessionId(id: String) {
        try { ctx.legacyWearStore.edit { it[KEY_SID] = id } } catch (_: Exception) {}
    }
}
