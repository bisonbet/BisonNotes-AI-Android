package com.bisonnotesai.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bisonnotesai.android.data.transcription.openai.OpenAIConfig
import com.bisonnotesai.android.data.transcription.openai.OpenAITranscribeModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore extension for OpenAI preferences
 */
private val Context.openAIPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "openai_preferences"
)

/**
 * Manager for OpenAI settings persistence
 * Uses encrypted DataStore for secure API key storage
 */
@Singleton
class OpenAIPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.openAIPreferencesDataStore

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val MODEL = stringPreferencesKey("model")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val LANGUAGE = stringPreferencesKey("language")

        private const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        private const val DEFAULT_LANGUAGE = "en"
    }

    /**
     * Get OpenAI configuration as Flow
     */
    val config: Flow<OpenAIConfig> = dataStore.data.map { preferences ->
        OpenAIConfig(
            apiKey = preferences[API_KEY] ?: "",
            model = OpenAITranscribeModel.fromModelId(
                preferences[MODEL] ?: OpenAITranscribeModel.GPT_4O_MINI_TRANSCRIBE.modelId
            ),
            baseURL = preferences[BASE_URL] ?: DEFAULT_BASE_URL,
            language = preferences[LANGUAGE] ?: DEFAULT_LANGUAGE
        )
    }

    /**
     * Get API key
     */
    val apiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }

    /**
     * Get selected model
     */
    val model: Flow<OpenAITranscribeModel> = dataStore.data.map { preferences ->
        OpenAITranscribeModel.fromModelId(
            preferences[MODEL] ?: OpenAITranscribeModel.GPT_4O_MINI_TRANSCRIBE.modelId
        )
    }

    /**
     * Get base URL
     */
    val baseURL: Flow<String> = dataStore.data.map { preferences ->
        preferences[BASE_URL] ?: DEFAULT_BASE_URL
    }

    /**
     * Get language
     */
    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: DEFAULT_LANGUAGE
    }

    /**
     * Save API key
     */
    suspend fun saveApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    /**
     * Save model
     */
    suspend fun saveModel(model: OpenAITranscribeModel) {
        dataStore.edit { preferences ->
            preferences[MODEL] = model.modelId
        }
    }

    /**
     * Save base URL
     */
    suspend fun saveBaseURL(baseURL: String) {
        dataStore.edit { preferences ->
            preferences[BASE_URL] = baseURL
        }
    }

    /**
     * Save language
     */
    suspend fun saveLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    /**
     * Save complete configuration
     */
    suspend fun saveConfig(config: OpenAIConfig) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = config.apiKey
            preferences[MODEL] = config.model.modelId
            preferences[BASE_URL] = config.baseURL
            preferences[LANGUAGE] = config.language ?: DEFAULT_LANGUAGE
        }
    }

    /**
     * Reset to defaults
     */
    suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Check if API key is configured
     */
    suspend fun isConfigured(): Boolean {
        var configured = false
        dataStore.data.collect { preferences ->
            configured = !preferences[API_KEY].isNullOrBlank()
        }
        return configured
    }
}
