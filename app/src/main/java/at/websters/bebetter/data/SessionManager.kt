package at.websters.bebetter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        const val DEFAULT_BASE_URL = "https://app.bebetter.websters.at"
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val baseUrlFlow: Flow<String> = context.dataStore.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }
    val themeFlow: Flow<String?> = context.dataStore.data.map { it[KEY_THEME] }
    val keepScreenOnFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }

    suspend fun getToken(): String? = context.dataStore.data.map { it[KEY_TOKEN] }.first()
    suspend fun saveToken(token: String) { context.dataStore.edit { it[KEY_TOKEN] = token } }
    suspend fun clearToken() { context.dataStore.edit { it.remove(KEY_TOKEN) } }

    suspend fun getBaseUrl(): String = context.dataStore.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }.first()
    suspend fun saveBaseUrl(url: String) { context.dataStore.edit { it[KEY_BASE_URL] = url.trimEnd('/') } }

    suspend fun getTheme(): String? = context.dataStore.data.map { it[KEY_THEME] }.first()
    suspend fun saveTheme(theme: String?) {
        context.dataStore.edit { if (theme == null) it.remove(KEY_THEME) else it[KEY_THEME] = theme }
    }

    suspend fun getKeepScreenOn(): Boolean = context.dataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }.first()
    suspend fun saveKeepScreenOn(on: Boolean) { context.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = on } }
}
