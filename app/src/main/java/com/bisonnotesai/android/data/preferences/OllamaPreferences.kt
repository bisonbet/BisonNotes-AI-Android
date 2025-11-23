package com.bisonnotesai.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ollamaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ollama_preferences"
)

@Singleton
class OllamaPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.ollamaDataStore

    companion object {
        private val ENABLED = booleanPreferencesKey("enabled")
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val MODEL = stringPreferencesKey("model")

        const val DEFAULT_SERVER_URL = "http://localhost:11434"
        const val DEFAULT_MODEL = "llama3"
    }

    val enabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLED] ?: false
    }

    val serverUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
    }

    val model: Flow<String> = dataStore.data.map { preferences ->
        preferences[MODEL] ?: DEFAULT_MODEL
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[ENABLED] = enabled }
    }

    suspend fun setServerUrl(url: String) {
        dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun setModel(model: String) {
        dataStore.edit { it[MODEL] = model }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
