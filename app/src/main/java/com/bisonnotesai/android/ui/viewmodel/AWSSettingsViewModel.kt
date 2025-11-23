package com.bisonnotesai.android.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisonnotesai.android.data.preferences.AWSPreferences
import com.bisonnotesai.android.data.transcription.aws.AWSRegion
import com.bisonnotesai.android.data.transcription.aws.AWSTranscribeEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AWS settings screen
 */
@HiltViewModel
class AWSSettingsViewModel @Inject constructor(
    private val preferences: AWSPreferences,
    private val transcribeEngine: AWSTranscribeEngine
) : ViewModel() {

    companion object {
        private const val TAG = "AWSSettingsViewModel"
    }

    // UI State
    private val _uiState = MutableStateFlow(AWSSettingsUiState())
    val uiState: StateFlow<AWSSettingsUiState> = _uiState.asStateFlow()

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
                        accessKeyId = config.credentials.accessKeyId,
                        secretAccessKey = config.credentials.secretAccessKey,
                        selectedRegion = AWSRegion.fromRegionId(config.credentials.region),
                        bucketName = config.bucketName,
                        languageCode = config.languageCode
                    )
                }
            }
        }
    }

    /**
     * Update access key ID
     */
    fun updateAccessKeyId(accessKeyId: String) {
        _uiState.update { it.copy(accessKeyId = accessKeyId) }
        viewModelScope.launch {
            preferences.saveAccessKeyId(accessKeyId)
        }
    }

    /**
     * Update secret access key
     */
    fun updateSecretAccessKey(secretAccessKey: String) {
        _uiState.update { it.copy(secretAccessKey = secretAccessKey) }
        viewModelScope.launch {
            preferences.saveSecretAccessKey(secretAccessKey)
        }
    }

    /**
     * Update region
     */
    fun updateRegion(region: AWSRegion) {
        _uiState.update { it.copy(selectedRegion = region) }
        viewModelScope.launch {
            preferences.saveRegion(region.regionId)
        }
    }

    /**
     * Update bucket name
     */
    fun updateBucketName(bucketName: String) {
        _uiState.update { it.copy(bucketName = bucketName) }
        viewModelScope.launch {
            preferences.saveBucketName(bucketName)
        }
    }

    /**
     * Update language code
     */
    fun updateLanguageCode(languageCode: String) {
        _uiState.update { it.copy(languageCode = languageCode) }
        viewModelScope.launch {
            preferences.saveLanguageCode(languageCode)
        }
    }

    /**
     * Test connection to AWS
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

            val result = transcribeEngine.testConnection()

            result.fold(
                onSuccess = { message ->
                    Log.d(TAG, "AWS connection test successful: $message")
                    _uiState.update { state ->
                        state.copy(
                            isTestingConnection = false,
                            connectionTestResult = message,
                            isConnectionSuccessful = true
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "AWS connection test failed", error)
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
                accessKeyId = "",
                secretAccessKey = "",
                selectedRegion = AWSRegion.US_EAST_1,
                bucketName = "",
                languageCode = "en-US",
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
 * UI state for AWS settings screen
 */
data class AWSSettingsUiState(
    val accessKeyId: String = "",
    val secretAccessKey: String = "",
    val selectedRegion: AWSRegion = AWSRegion.US_EAST_1,
    val bucketName: String = "",
    val languageCode: String = "en-US",
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String = "",
    val isConnectionSuccessful: Boolean = false
) {
    val isConfigurationValid: Boolean
        get() = accessKeyId.isNotBlank() &&
                secretAccessKey.isNotBlank() &&
                bucketName.isNotBlank()
}
