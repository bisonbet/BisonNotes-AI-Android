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

private val Context.geminiDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gemini_preferences"
)

@Singleton
class GeminiPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.geminiDataStore

    companion object {
        private val ENABLED = booleanPreferencesKey("enabled")
        private val API_KEY = stringPreferencesKey("api_key")
        private val MODEL = stringPreferencesKey("model")

        const val DEFAULT_MODEL = "gemini-1.5-pro"
    }

    val enabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLED] ?: false
    }

    val apiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }

    val model: Flow<String> = dataStore.data.map { preferences ->
        preferences[MODEL] ?: DEFAULT_MODEL
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[ENABLED] = enabled }
    }

    suspend fun setApiKey(apiKey: String) {
        dataStore.edit { it[API_KEY] = apiKey }
    }

    suspend fun setModel(model: String) {
        dataStore.edit { it[MODEL] = model }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
