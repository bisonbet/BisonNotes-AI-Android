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

private val Context.openAISummarizationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "openai_summarization_preferences"
)

/**
 * Preferences for OpenAI summarization settings
 */
@Singleton
class OpenAISummarizationPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.openAISummarizationDataStore

    companion object {
        private val ENABLED = booleanPreferencesKey("enabled")
        private val API_KEY = stringPreferencesKey("api_key")
        private val MODEL = stringPreferencesKey("model")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val TEMPERATURE = doublePreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val TIMEOUT = intPreferencesKey("timeout")

        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_TEMPERATURE = 0.1
        const val DEFAULT_MAX_TOKENS = 2048
        const val DEFAULT_TIMEOUT = 60
    }

    /**
     * Flow of enabled state
     */
    val enabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLED] ?: false
    }

    /**
     * Flow of API key
     */
    val apiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }

    /**
     * Flow of model
     */
    val model: Flow<String> = dataStore.data.map { preferences ->
        preferences[MODEL] ?: DEFAULT_MODEL
    }

    /**
     * Flow of base URL
     */
    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[BASE_URL] ?: DEFAULT_BASE_URL
    }

    /**
     * Flow of temperature
     */
    val temperature: Flow<Double> = dataStore.data.map { preferences ->
        preferences[TEMPERATURE] ?: DEFAULT_TEMPERATURE
    }

    /**
     * Flow of max tokens
     */
    val maxTokens: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MAX_TOKENS] ?: DEFAULT_MAX_TOKENS
    }

    /**
     * Flow of timeout
     */
    val timeout: Flow<Int> = dataStore.data.map { preferences ->
        preferences[TIMEOUT] ?: DEFAULT_TIMEOUT
    }

    /**
     * Save enabled state
     */
    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLED] = enabled
        }
    }

    /**
     * Save API key
     */
    suspend fun setApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    /**
     * Save model
     */
    suspend fun setModel(model: String) {
        dataStore.edit { preferences ->
            preferences[MODEL] = model
        }
    }

    /**
     * Save base URL
     */
    suspend fun setBaseUrl(baseUrl: String) {
        dataStore.edit { preferences ->
            preferences[BASE_URL] = baseUrl
        }
    }

    /**
     * Save temperature
     */
    suspend fun setTemperature(temperature: Double) {
        dataStore.edit { preferences ->
            preferences[TEMPERATURE] = temperature
        }
    }

    /**
     * Save max tokens
     */
    suspend fun setMaxTokens(maxTokens: Int) {
        dataStore.edit { preferences ->
            preferences[MAX_TOKENS] = maxTokens
        }
    }

    /**
     * Save timeout
     */
    suspend fun setTimeout(timeout: Int) {
        dataStore.edit { preferences ->
            preferences[TIMEOUT] = timeout
        }
    }

    /**
     * Clear all settings
     */
    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
