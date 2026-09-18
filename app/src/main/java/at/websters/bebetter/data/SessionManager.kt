package at.websters.bebetter.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bebetter_session")

class SessionManager(private val context: Context) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_THEME = stringPreferencesKey("theme") // light | dark | null(system)
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_LAST_UPDATE_CHECK = androidx.datastore.preferences.core.longPreferencesKey("last_update_check")
        private val KEY_SKIPPED_VERSION = stringPreferencesKey("skipped_version")
        const val DEFAULT_BASE_URL = "https://app.bebetter.websters.at"
        private const val ENC_PREFS = "bebetter_session_enc"
    }

    private val appCtx = context.applicationContext

    // Only the auth token is sensitive: it lives in EncryptedSharedPreferences
    // (AndroidKeyStore-backed). Everything else stays in DataStore.
    private val encPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appCtx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            appCtx, ENC_PREFS, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // In-memory cache: avoids disk reads on the network path and makes
    // "stay logged in = off" (memory-only session) possible.
    @Volatile var cachedToken: String? = null
        private set

    /** One-shot migration of a plaintext legacy token. Safe to call every launch. */
    suspend fun ensureMigrated() {
        try {
            if (encPrefs.contains("token")) return
            val old = appCtx.dataStore.data.first()[KEY_TOKEN]
            if (old != null) {
                encPrefs.edit().putString("token", old).apply()
                appCtx.dataStore.edit { it.remove(KEY_TOKEN) }
            }
        } catch (_: Exception) { /* stay logged out rather than crash */ }
    }

    val themeFlow: Flow<String?> = appCtx.dataStore.data.map { it[KEY_THEME] }
    val keepScreenOnFlow: Flow<Boolean> = appCtx.dataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }

    suspend fun getToken(): String? {
        cachedToken?.let { return it }
        return try {
            encPrefs.getString("token", null)?.also { cachedToken = it }
        } catch (_: Exception) { null }
    }

    suspend fun saveToken(token: String, persist: Boolean = true) {
        cachedToken = token
        try {
            if (persist) encPrefs.edit().putString("token", token).apply()
            else encPrefs.edit().remove("token").apply()
        } catch (_: Exception) {}
    }

    suspend fun clearToken() {
        cachedToken = null
        try { encPrefs.edit().remove("token").apply() } catch (_: Exception) {}
    }

    suspend fun getBaseUrl(): String = try {
        appCtx.dataStore.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }.first()
    } catch (_: Exception) { DEFAULT_BASE_URL }

    suspend fun saveBaseUrl(url: String) {
        try { appCtx.dataStore.edit { it[KEY_BASE_URL] = url.trimEnd('/') } } catch (_: Exception) {}
    }

    suspend fun getTheme(): String? = try {
        appCtx.dataStore.data.map { it[KEY_THEME] }.first()
    } catch (_: Exception) { null }

    suspend fun saveTheme(theme: String?) {
        try { appCtx.dataStore.edit { if (theme == null) it.remove(KEY_THEME) else it[KEY_THEME] = theme } } catch (_: Exception) {}
    }

    suspend fun getKeepScreenOn(): Boolean = try {
        appCtx.dataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }.first()
    } catch (_: Exception) { true }

    suspend fun lastUpdateCheck(): Long = try {
        appCtx.dataStore.data.map { it[KEY_LAST_UPDATE_CHECK] ?: 0L }.first()
    } catch (_: Exception) { 0L }

    suspend fun saveLastUpdateCheck(now: Long) {
        try { appCtx.dataStore.edit { it[KEY_LAST_UPDATE_CHECK] = now } } catch (_: Exception) {}
    }

    suspend fun skippedVersion(): String? = try {
        appCtx.dataStore.data.map { it[KEY_SKIPPED_VERSION] }.first()
    } catch (_: Exception) { null }

    suspend fun saveSkippedVersion(tag: String?) {
        try {
            appCtx.dataStore.edit { if (tag == null) it.remove(KEY_SKIPPED_VERSION) else it[KEY_SKIPPED_VERSION] = tag }
        } catch (_: Exception) {}
    }

    suspend fun saveKeepScreenOn(on: Boolean) {
        try { appCtx.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = on } } catch (_: Exception) {}
    }
}
