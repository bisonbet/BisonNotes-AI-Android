package com.bisonnotesai.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.bisonnotesai.android.data.transcription.whisper.WhisperConfig
import com.bisonnotesai.android.data.transcription.whisper.WhisperProtocol
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore extension for local Whisper server preferences
 */
private val Context.whisperPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "whisper_preferences"
)

/**
 * Manager for local Whisper server settings persistence
 * Stores connection details for self-hosted Whisper server
 */
@Singleton
class WhisperPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.whisperPreferencesDataStore

    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val PORT = intPreferencesKey("port")
        private val PROTOCOL = stringPreferencesKey("protocol")
        private val LANGUAGE = stringPreferencesKey("language")
        private val ENABLE_WORD_TIMESTAMPS = booleanPreferencesKey("enable_word_timestamps")
        private val ENABLE_SPEAKER_DIARIZATION = booleanPreferencesKey("enable_speaker_diarization")
        private val MIN_SPEAKERS = intPreferencesKey("min_speakers")
        private val MAX_SPEAKERS = intPreferencesKey("max_speakers")

        private const val DEFAULT_SERVER_URL = "http://localhost"
        private const val DEFAULT_PORT = 9000
        private const val DEFAULT_PROTOCOL = "REST"
    }

    /**
     * Get Whisper configuration as Flow
     */
    val config: Flow<WhisperConfig> = dataStore.data.map { preferences ->
        WhisperConfig(
            serverURL = preferences[SERVER_URL] ?: DEFAULT_SERVER_URL,
            port = preferences[PORT] ?: DEFAULT_PORT,
            protocol = WhisperProtocol.fromString(
                preferences[PROTOCOL] ?: DEFAULT_PROTOCOL
            ),
            language = preferences[LANGUAGE],
            enableWordTimestamps = preferences[ENABLE_WORD_TIMESTAMPS] ?: false,
            enableSpeakerDiarization = preferences[ENABLE_SPEAKER_DIARIZATION] ?: false,
            minSpeakers = preferences[MIN_SPEAKERS],
            maxSpeakers = preferences[MAX_SPEAKERS]
        )
    }

    /**
     * Get server URL
     */
    val serverURL: Flow<String> = dataStore.data.map { preferences ->
        preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
    }

    /**
     * Get port
     */
    val port: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PORT] ?: DEFAULT_PORT
    }

    /**
     * Get protocol
     */
    val protocol: Flow<WhisperProtocol> = dataStore.data.map { preferences ->
        WhisperProtocol.fromString(preferences[PROTOCOL] ?: DEFAULT_PROTOCOL)
    }

    /**
     * Get language
     */
    val language: Flow<String?> = dataStore.data.map { preferences ->
        preferences[LANGUAGE]
    }

    /**
     * Save server URL
     */
    suspend fun saveServerURL(url: String) {
        dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }

    /**
     * Save port
     */
    suspend fun savePort(port: Int) {
        dataStore.edit { preferences ->
            preferences[PORT] = port
        }
    }

    /**
     * Save protocol
     */
    suspend fun saveProtocol(protocol: WhisperProtocol) {
        dataStore.edit { preferences ->
            preferences[PROTOCOL] = protocol.name
        }
    }

    /**
     * Save language
     */
    suspend fun saveLanguage(language: String?) {
        dataStore.edit { preferences ->
            if (language != null) {
                preferences[LANGUAGE] = language
            } else {
                preferences.remove(LANGUAGE)
            }
        }
    }

    /**
     * Save word timestamps setting
     */
    suspend fun saveEnableWordTimestamps(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_WORD_TIMESTAMPS] = enable
        }
    }

    /**
     * Save speaker diarization setting
     */
    suspend fun saveEnableSpeakerDiarization(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_SPEAKER_DIARIZATION] = enable
        }
    }

    /**
     * Save min speakers
     */
    suspend fun saveMinSpeakers(min: Int?) {
        dataStore.edit { preferences ->
            if (min != null) {
                preferences[MIN_SPEAKERS] = min
            } else {
                preferences.remove(MIN_SPEAKERS)
            }
        }
    }

    /**
     * Save max speakers
     */
    suspend fun saveMaxSpeakers(max: Int?) {
        dataStore.edit { preferences ->
            if (max != null) {
                preferences[MAX_SPEAKERS] = max
            } else {
                preferences.remove(MAX_SPEAKERS)
            }
        }
    }

    /**
     * Save complete configuration
     */
    suspend fun saveConfig(config: WhisperConfig) {
        dataStore.edit { preferences ->
            preferences[SERVER_URL] = config.serverURL
            preferences[PORT] = config.port
            preferences[PROTOCOL] = config.protocol.name
            if (config.language != null) {
                preferences[LANGUAGE] = config.language
            }
            preferences[ENABLE_WORD_TIMESTAMPS] = config.enableWordTimestamps
            preferences[ENABLE_SPEAKER_DIARIZATION] = config.enableSpeakerDiarization
            if (config.minSpeakers != null) {
                preferences[MIN_SPEAKERS] = config.minSpeakers
            }
            if (config.maxSpeakers != null) {
                preferences[MAX_SPEAKERS] = config.maxSpeakers
            }
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
     * Check if server is configured (has non-default values)
     */
    suspend fun isConfigured(): Boolean {
        var configured = false
        dataStore.data.collect { preferences ->
            val url = preferences[SERVER_URL] ?: DEFAULT_SERVER_URL
            configured = url != DEFAULT_SERVER_URL ||
                        (preferences[PORT] ?: DEFAULT_PORT) != DEFAULT_PORT
        }
        return configured
    }
}
