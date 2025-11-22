package com.bisonnotesai.android.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisonnotesai.android.audio.AudioPlayer
import com.bisonnotesai.android.audio.AudioRecorder
import com.bisonnotesai.android.audio.RecordingFileManager
import com.bisonnotesai.android.audio.RecordingService
import com.bisonnotesai.android.domain.model.Recording
import com.bisonnotesai.android.domain.model.ProcessingStatus
import com.bisonnotesai.android.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for audio recording screen
 * Orchestrates audio recording, playback, and database operations
 */
@HiltViewModel
class AudioRecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val fileManager: RecordingFileManager,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    // Recording state
    private val _uiState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    // List of all recordings
    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings.asStateFlow()

    // Currently selected recording for playback
    private val _selectedRecording = MutableStateFlow<Recording?>(null)
    val selectedRecording: StateFlow<Recording?> = _selectedRecording.asStateFlow()

    // Playback state
    val isPlaying = audioPlayer.isPlaying
    val playbackPosition = audioPlayer.currentPosition
    val playbackDuration = audioPlayer.duration
    val playbackSpeed = audioPlayer.playbackSpeed

    // Recording duration
    val recordingDuration = audioRecorder.recordingDuration

    // Current recording ID
    private var currentRecordingId: String? = null

    // Timer job for updating duration
    private var timerJob: Job? = null

    init {
        loadRecordings()
    }

    /**
     * Load all recordings from database
     */
    private fun loadRecordings() {
        viewModelScope.launch {
            recordingRepository.getAllRecordings()
                .collect { recordingList ->
                    _recordings.value = recordingList
                }
        }
    }

    /**
     * Start recording
     */
    fun startRecording() {
        viewModelScope.launch {
            val recordingId = UUID.randomUUID().toString()
            currentRecordingId = recordingId

            _uiState.value = RecordingUiState.Recording(recordingId, 0)

            // Start recording via service for background support
            RecordingService.startRecording(context, recordingId)

            // Start timer to track duration
            startDurationTimer()
        }
    }

    /**
     * Stop recording and save to database
     */
    fun stopRecording() {
        viewModelScope.launch {
            timerJob?.cancel()

            val recordingId = currentRecordingId ?: return@launch

            _uiState.value = RecordingUiState.Saving

            // Stop recording via service
            RecordingService.stopRecording(context)

            // Wait a bit for service to finalize
            delay(500)

            // Get the finalized file
            val file = fileManager.getRecordingFile(recordingId)

            if (file.exists()) {
                // Create recording entity
                val recording = Recording(
                    id = recordingId,
                    name = fileManager.generateDefaultRecordingName(),
                    date = Date(),
                    url = file.absolutePath,
                    duration = audioRecorder.getCurrentDuration().toDouble() / 1000.0, // Convert to seconds
                    fileSize = file.length(),
                    location = null, // TODO: Add location tracking
                    transcriptionStatus = ProcessingStatus.PENDING,
                    summaryStatus = ProcessingStatus.PENDING,
                    transcriptId = null,
                    summaryId = null
                )

                // Save to database
                recordingRepository.saveRecording(recording)
                    .onSuccess {
                        _uiState.value = RecordingUiState.Idle
                        currentRecordingId = null
                    }
                    .onFailure { error ->
                        _uiState.value = RecordingUiState.Error(
                            error.message ?: "Failed to save recording"
                        )
                    }
            } else {
                _uiState.value = RecordingUiState.Error("Recording file not found")
            }
        }
    }

    /**
     * Delete a recording
     */
    fun deleteRecording(recordingId: String) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(recordingId)
                .onSuccess {
                    fileManager.deleteRecordingFile(recordingId)
                }
                .onFailure { error ->
                    _uiState.value = RecordingUiState.Error(
                        error.message ?: "Failed to delete recording"
                    )
                }
        }
    }

    /**
     * Play a recording
     */
    fun playRecording(recording: Recording) {
        viewModelScope.launch {
            val file = File(recording.url ?: return@launch)

            if (!file.exists()) {
                _uiState.value = RecordingUiState.Error("Recording file not found")
                return@launch
            }

            _selectedRecording.value = recording

            audioPlayer.preparePlayer(file)
                .onSuccess {
                    audioPlayer.play()
                    startPlaybackTimer()
                }
                .onFailure { error ->
                    _uiState.value = RecordingUiState.Error(
                        error.message ?: "Failed to play recording"
                    )
                }
        }
    }

    /**
     * Pause playback
     */
    fun pausePlayback() {
        audioPlayer.pause()
        timerJob?.cancel()
    }

    /**
     * Resume playback
     */
    fun resumePlayback() {
        audioPlayer.play()
        startPlaybackTimer()
    }

    /**
     * Stop playback
     */
    fun stopPlayback() {
        audioPlayer.stop()
        timerJob?.cancel()
        _selectedRecording.value = null
    }

    /**
     * Seek to position (0.0 to 1.0)
     */
    fun seekTo(progress: Float) {
        val duration = audioPlayer.getDuration()
        val position = (duration * progress).toLong()
        audioPlayer.seekTo(position)
    }

    /**
     * Set playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
    }

    /**
     * Skip forward
     */
    fun skipForward() {
        audioPlayer.skipForward()
    }

    /**
     * Skip backward
     */
    fun skipBackward() {
        audioPlayer.skipBackward()
    }

    /**
     * Rename recording
     */
    fun renameRecording(recordingId: String, newName: String) {
        viewModelScope.launch {
            recordingRepository.updateRecordingName(recordingId, newName)
                .onFailure { error ->
                    _uiState.value = RecordingUiState.Error(
                        error.message ?: "Failed to rename recording"
                    )
                }
        }
    }

    /**
     * Start timer for duration updates
     */
    private fun startDurationTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(100) // Update every 100ms
                audioRecorder.updateDuration()

                val duration = audioRecorder.getCurrentDuration()
                val recordingId = currentRecordingId ?: ""
                _uiState.value = RecordingUiState.Recording(recordingId, duration)
            }
        }
    }

    /**
     * Start timer for playback position updates
     */
    private fun startPlaybackTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && isPlaying.value) {
                delay(100) // Update every 100ms
                audioPlayer.updatePosition()
            }
        }
    }

    /**
     * Format duration in MM:SS
     */
    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * Dismiss error
     */
    fun dismissError() {
        _uiState.value = RecordingUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioRecorder.release()
        audioPlayer.release()
    }
}

/**
 * UI state for recording screen
 */
sealed class RecordingUiState {
    object Idle : RecordingUiState()
    data class Recording(val recordingId: String, val duration: Long) : RecordingUiState()
    object Saving : RecordingUiState()
    data class Error(val message: String) : RecordingUiState()
}
