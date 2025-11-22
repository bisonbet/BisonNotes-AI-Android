package com.bisonnotesai.android.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisonnotesai.android.data.preferences.OpenAIPreferences
import com.bisonnotesai.android.data.transcription.openai.OpenAITranscribeModel
import com.bisonnotesai.android.data.transcription.openai.OpenAIWhisperEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for OpenAI settings screen
 */
@HiltViewModel
class OpenAISettingsViewModel @Inject constructor(
    private val preferences: OpenAIPreferences,
    private val whisperEngine: OpenAIWhisperEngine
) : ViewModel() {

    companion object {
        private const val TAG = "OpenAISettingsViewModel"
    }

    // UI State
    private val _uiState = MutableStateFlow(OpenAISettingsUiState())
    val uiState: StateFlow<OpenAISettingsUiState> = _uiState.asStateFlow()

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
                        apiKey = config.apiKey,
                        selectedModel = config.model,
                        baseURL = config.baseURL
                    )
                }
            }
        }
    }

    /**
     * Update API key
     */
    fun updateApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
        viewModelScope.launch {
            preferences.saveApiKey(apiKey)
        }
    }

    /**
     * Update selected model
     */
    fun updateModel(model: OpenAITranscribeModel) {
        _uiState.update { it.copy(selectedModel = model) }
        viewModelScope.launch {
            preferences.saveModel(model)
        }
    }

    /**
     * Update base URL
     */
    fun updateBaseURL(baseURL: String) {
        _uiState.update { it.copy(baseURL = baseURL) }
        viewModelScope.launch {
            preferences.saveBaseURL(baseURL)
        }
    }

    /**
     * Test connection to OpenAI API
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
                    Log.d(TAG, "Connection test successful: $message")
                    _uiState.update { state ->
                        state.copy(
                            isTestingConnection = false,
                            connectionTestResult = message,
                            isConnectionSuccessful = true
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Connection test failed", error)
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
                apiKey = "",
                selectedModel = OpenAITranscribeModel.WHISPER_1,
                baseURL = "https://api.openai.com/v1",
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
 * UI state for OpenAI settings screen
 */
data class OpenAISettingsUiState(
    val apiKey: String = "",
    val selectedModel: OpenAITranscribeModel = OpenAITranscribeModel.GPT_4O_MINI_TRANSCRIBE,
    val baseURL: String = "https://api.openai.com/v1",
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String = "",
    val isConnectionSuccessful: Boolean = false
)
