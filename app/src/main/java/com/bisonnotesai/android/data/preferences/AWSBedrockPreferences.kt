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

private val Context.awsBedrockDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aws_bedrock_preferences"
)

/**
 * Preferences for AWS Bedrock (Claude) settings
 */
@Singleton
class AWSBedrockPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.awsBedrockDataStore

    companion object {
        private val ENABLED = booleanPreferencesKey("enabled")
        private val MODEL = stringPreferencesKey("model")
        private val TEMPERATURE = doublePreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")

        const val DEFAULT_MODEL = "anthropic.claude-3-5-sonnet-20241022-v2:0"
        const val DEFAULT_TEMPERATURE = 0.1
        const val DEFAULT_MAX_TOKENS = 4096
    }

    val enabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLED] ?: false
    }

    val model: Flow<String> = dataStore.data.map { preferences ->
        preferences[MODEL] ?: DEFAULT_MODEL
    }

    val temperature: Flow<Double> = dataStore.data.map { preferences ->
        preferences[TEMPERATURE] ?: DEFAULT_TEMPERATURE
    }

    val maxTokens: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MAX_TOKENS] ?: DEFAULT_MAX_TOKENS
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLED] = enabled
        }
    }

    suspend fun setModel(model: String) {
        dataStore.edit { preferences ->
            preferences[MODEL] = model
        }
    }

    suspend fun setTemperature(temperature: Double) {
        dataStore.edit { preferences ->
            preferences[TEMPERATURE] = temperature
        }
    }

    suspend fun setMaxTokens(maxTokens: Int) {
        dataStore.edit { preferences ->
            preferences[MAX_TOKENS] = maxTokens
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
