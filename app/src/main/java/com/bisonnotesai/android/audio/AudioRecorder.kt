package com.bisonnotesai.android.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio recorder wrapper around MediaRecorder
 * Handles recording audio to M4A files
 */
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileManager: RecordingFileManager,
    private val sessionManager: AudioSessionManager
) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    /**
     * Start recording audio
     * Returns the recording file if successful, null if failed
     */
    fun startRecording(recordingId: String): Result<File> {
        return try {
            // Request audio focus
            val hasFocus = sessionManager.requestAudioFocusForRecording(
                onFocusLost = { stopRecording() },
                onFocusGained = { /* Resume if paused */ }
            )

            if (!hasFocus) {
                return Result.failure(Exception("Failed to gain audio focus"))
            }

            // Create temporary recording file
            val tempFile = fileManager.createTempRecordingFile(recordingId)
            currentRecordingFile = tempFile

            // Initialize MediaRecorder
            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(tempFile.absolutePath)

                try {
                    prepare()
                    start()
                } catch (e: IOException) {
                    release()
                    mediaRecorder = null
                    tempFile.delete()
                    return Result.failure(e)
                }
            }

            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true

            Result.success(tempFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop recording
     * Returns the finalized recording file
     */
    fun stopRecording(): Result<File> {
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                    release()
                } catch (e: Exception) {
                    // MediaRecorder may throw if stopped too quickly
                    release()
                }
            }
            mediaRecorder = null

            _isRecording.value = false
            _recordingDuration.value = 0

            // Abandon audio focus
            sessionManager.abandonAudioFocus()

            // Finalize the recording file
            val tempFile = currentRecordingFile
            currentRecordingFile = null

            if (tempFile != null && tempFile.exists()) {
                val finalFile = fileManager.finalizeTempRecording(tempFile)
                if (finalFile != null) {
                    Result.success(finalFile)
                } else {
                    Result.failure(Exception("Failed to finalize recording file"))
                }
            } else {
                Result.failure(Exception("Recording file not found"))
            }
        } catch (e: Exception) {
            _isRecording.value = false
            currentRecordingFile?.delete()
            currentRecordingFile = null
            sessionManager.abandonAudioFocus()
            Result.failure(e)
        }
    }

    /**
     * Pause recording (API 24+)
     */
    fun pauseRecording(): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Pause not supported on this Android version"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume recording (API 24+)
     */
    fun resumeRecording(): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Resume not supported on this Android version"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current recording duration in milliseconds
     */
    fun getCurrentDuration(): Long {
        return if (_isRecording.value) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0L
        }
    }

    /**
     * Update recording duration (call this periodically from a timer)
     */
    fun updateDuration() {
        if (_isRecording.value) {
            _recordingDuration.value = getCurrentDuration()
        }
    }

    /**
     * Get max amplitude (for waveform visualization)
     * Returns 0-32767
     */
    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Cancel recording and delete file
     */
    fun cancelRecording(): Result<Unit> {
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                    release()
                } catch (e: Exception) {
                    release()
                }
            }
            mediaRecorder = null

            _isRecording.value = false
            _recordingDuration.value = 0

            // Delete the recording file
            currentRecordingFile?.delete()
            currentRecordingFile = null

            // Abandon audio focus
            sessionManager.abandonAudioFocus()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Release resources
     */
    fun release() {
        if (_isRecording.value) {
            stopRecording()
        }
        mediaRecorder?.release()
        mediaRecorder = null
        sessionManager.abandonAudioFocus()
    }

    /**
     * Create MediaRecorder instance
     * Handles API level differences
     */
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    /**
     * Check if recording is supported
     */
    fun isRecordingSupported(): Boolean {
        return try {
            val recorder = createMediaRecorder()
            recorder.release()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get recording quality settings
     */
    data class RecordingQuality(
        val bitRate: Int,
        val sampleRate: Int,
        val name: String
    ) {
        companion object {
            val LOW = RecordingQuality(64000, 22050, "Low")
            val MEDIUM = RecordingQuality(128000, 44100, "Medium")
            val HIGH = RecordingQuality(256000, 48000, "High")
            val VERY_HIGH = RecordingQuality(320000, 48000, "Very High")
        }
    }
}
