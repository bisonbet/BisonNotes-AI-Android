package com.bisonnotesai.android.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisonnotesai.android.data.preferences.WhisperPreferences
import com.bisonnotesai.android.data.transcription.whisper.LocalWhisperEngine
import com.bisonnotesai.android.data.transcription.whisper.WhisperProtocol
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Local Whisper Server settings screen
 * Manages configuration for privacy-focused local transcription
 */
@HiltViewModel
class WhisperSettingsViewModel @Inject constructor(
    private val preferences: WhisperPreferences,
    private val whisperEngine: LocalWhisperEngine
) : ViewModel() {

    companion object {
        private const val TAG = "WhisperSettingsViewModel"
    }

    // UI State
    private val _uiState = MutableStateFlow(WhisperSettingsUiState())
    val uiState: StateFlow<WhisperSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Load settings from preferences
     */
    private fun loadSettings() {
        viewModelScope.launch {
            preferences.config.collectLatest { config ->
                _uiState.update { state ->
                    state.copy(
                        serverURL = config.serverURL,
                        port = config.port,
                        selectedProtocol = config.protocol,
                        enableWordTimestamps = config.enableWordTimestamps,
                        enableSpeakerDiarization = config.enableSpeakerDiarization,
                        minSpeakers = config.minSpeakers,
                        maxSpeakers = config.maxSpeakers
                    )
                }
            }
        }
    }

    /**
     * Update server URL
     */
    fun updateServerURL(serverURL: String) {
        _uiState.update { it.copy(serverURL = serverURL) }
        viewModelScope.launch {
            preferences.saveServerURL(serverURL)
        }
    }

    /**
     * Update port
     */
    fun updatePort(port: Int) {
        _uiState.update { it.copy(port = port) }
        viewModelScope.launch {
            preferences.savePort(port)
        }
    }

    /**
     * Update protocol
     */
    fun updateProtocol(protocol: WhisperProtocol) {
        _uiState.update { state ->
            state.copy(
                selectedProtocol = protocol,
                // Update port to protocol default if current port matches old protocol default
                port = when (protocol) {
                    WhisperProtocol.REST -> if (state.port == 10300) 9000 else state.port
                    WhisperProtocol.WYOMING -> if (state.port == 9000) 10300 else state.port
                }
            )
        }
        viewModelScope.launch {
            preferences.saveProtocol(protocol.name)
            // Save updated port if it changed
            _uiState.value.port.let { preferences.savePort(it) }
        }
    }

    /**
     * Update enable word timestamps
     */
    fun updateEnableWordTimestamps(enabled: Boolean) {
        _uiState.update { it.copy(enableWordTimestamps = enabled) }
        viewModelScope.launch {
            preferences.saveEnableWordTimestamps(enabled)
        }
    }

    /**
     * Update enable speaker diarization
     */
    fun updateEnableSpeakerDiarization(enabled: Boolean) {
        _uiState.update { it.copy(enableSpeakerDiarization = enabled) }
        viewModelScope.launch {
            preferences.saveEnableSpeakerDiarization(enabled)
        }
    }

    /**
     * Update minimum speakers
     */
    fun updateMinSpeakers(minSpeakers: Int?) {
        _uiState.update { it.copy(minSpeakers = minSpeakers) }
        viewModelScope.launch {
            preferences.saveMinSpeakers(minSpeakers)
        }
    }

    /**
     * Update maximum speakers
     */
    fun updateMaxSpeakers(maxSpeakers: Int?) {
        _uiState.update { it.copy(maxSpeakers = maxSpeakers) }
        viewModelScope.launch {
            preferences.saveMaxSpeakers(maxSpeakers)
        }
    }

    /**
     * Test connection to local Whisper server
     */
    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isTestingConnection = true,
                    connectionTestResult = "",
                    isConnectionSuccessful = false
                )
            }

            val result = whisperEngine.testConnection()

            result.fold(
                onSuccess = { message ->
                    Log.d(TAG, "Whisper server connection test successful: $message")
                    _uiState.update { state ->
                        state.copy(
                            isTestingConnection = false,
                            connectionTestResult = message,
                            isConnectionSuccessful = true
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Whisper server connection test failed", error)
                    _uiState.update { state ->
                        state.copy(
                            isTestingConnection = false,
                            connectionTestResult = error.message ?: "Connection failed",
                            isConnectionSuccessful = false
                        )
                    }
                }
            )
        }
    }

    /**
     * Reset settings to defaults
     */
    fun resetToDefaults() {
        _uiState.update { state ->
            state.copy(
                serverURL = "http://localhost",
                port = 9000,
                selectedProtocol = WhisperProtocol.REST,
                enableWordTimestamps = false,
                enableSpeakerDiarization = false,
                minSpeakers = null,
                maxSpeakers = null,
                connectionTestResult = "",
                isConnectionSuccessful = false
            )
        }

        viewModelScope.launch {
            preferences.reset()
        }
    }
}

/**
 * UI state for Local Whisper Server settings screen
 */
data class WhisperSettingsUiState(
    val serverURL: String = "http://localhost",
    val port: Int = 9000,
    val selectedProtocol: WhisperProtocol = WhisperProtocol.REST,
    val enableWordTimestamps: Boolean = false,
    val enableSpeakerDiarization: Boolean = false,
    val minSpeakers: Int? = null,
    val maxSpeakers: Int? = null,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String = "",
    val isConnectionSuccessful: Boolean = false
) {
    /**
     * Check if configuration is valid
     */
    val isConfigurationValid: Boolean
        get() = serverURL.isNotBlank() && port > 0

    /**
     * Get full server URL with port
     */
    val fullServerURL: String
        get() = "$serverURL:$port"
}
