package at.websters.bebetter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

// Legacy plaintext store (pre-encryption). Read once during migration, then deleted.
private val Context.legacyStore by preferencesDataStore(name = "bebetter_session")
private const val ENC_FILE = "bebetter_session_enc.preferences_pb"

class SessionManager(private val context: Context) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_THEME = stringPreferencesKey("theme") // light | dark | null(system)
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        const val DEFAULT_BASE_URL = "https://app.bebetter.websters.at"
    }

    private val appCtx = context.applicationContext

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

    // In-memory cache: avoids runBlocking reads on the network path and makes
    // "stay logged in = off" (memory-only session) possible.
    @Volatile var cachedToken: String? = null
        private set

    /** One-shot migration from the legacy plaintext store. Safe to call every launch. */
    suspend fun ensureMigrated() {
        try {
            val legacyFile = appCtx.dataStoreFile("bebetter_session.preferences_pb")
            val encFile = File(appCtx.filesDir, "datastore/$ENC_FILE")
            if (!legacyFile.exists() || encFile.exists()) return
            val old = appCtx.legacyStore.data.first()
            val token = old[KEY_TOKEN]
            val base = old[KEY_BASE_URL]
            val theme = old[KEY_THEME]
            val keep = old[KEY_KEEP_SCREEN_ON]
            store.edit { e ->
                if (token != null) e[KEY_TOKEN] = token
                if (base != null) e[KEY_BASE_URL] = base
                if (theme != null) e[KEY_THEME] = theme
                if (keep != null) e[KEY_KEEP_SCREEN_ON] = keep
            }
            runCatching { legacyFile.delete() }
        } catch (_: Exception) { /* stay logged out rather than crash */ }
    }

    val tokenFlow: Flow<String?> = store.data.map { cachedToken ?: it[KEY_TOKEN] }
    val baseUrlFlow: Flow<String> = store.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }
    val themeFlow: Flow<String?> = store.data.map { it[KEY_THEME] }
    val keepScreenOnFlow: Flow<Boolean> = store.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }

    suspend fun getToken(): String? {
        cachedToken?.let { return it }
        return try {
            store.data.map { it[KEY_TOKEN] }.first()?.also { cachedToken = it }
        } catch (_: Exception) { null }
    }

    suspend fun saveToken(token: String, persist: Boolean = true) {
        cachedToken = token
        if (persist) {
            try { store.edit { it[KEY_TOKEN] = token } } catch (_: Exception) {}
        } else {
            try { store.edit { it.remove(KEY_TOKEN) } } catch (_: Exception) {}
        }
    }

    suspend fun clearToken() {
        cachedToken = null
        try { store.edit { it.remove(KEY_TOKEN) } } catch (_: Exception) {}
    }

    suspend fun getBaseUrl(): String = try {
        store.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }.first()
    } catch (_: Exception) { DEFAULT_BASE_URL }

    suspend fun saveBaseUrl(url: String) {
        try { store.edit { it[KEY_BASE_URL] = url.trimEnd('/') } } catch (_: Exception) {}
    }

    suspend fun getTheme(): String? = try {
        store.data.map { it[KEY_THEME] }.first()
    } catch (_: Exception) { null }

    suspend fun saveTheme(theme: String?) {
        try { store.edit { if (theme == null) it.remove(KEY_THEME) else it[KEY_THEME] = theme } } catch (_: Exception) {}
    }

    suspend fun getKeepScreenOn(): Boolean = try {
        store.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }.first()
    } catch (_: Exception) { true }

    suspend fun saveKeepScreenOn(on: Boolean) {
        try { store.edit { it[KEY_KEEP_SCREEN_ON] = on } } catch (_: Exception) {}
    }
}
