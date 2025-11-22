package com.bisonnotesai.android.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bisonnotesai.android.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Foreground service for audio recording
 * Allows recording to continue in background
 */
@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var fileManager: RecordingFileManager

    private val binder = RecordingServiceBinder()
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var currentRecordingId: String? = null
    private var recordingFile: File? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"
        private const val CHANNEL_NAME = "Audio Recording"

        const val ACTION_START_RECORDING = "action_start_recording"
        const val ACTION_STOP_RECORDING = "action_stop_recording"
        const val ACTION_PAUSE_RECORDING = "action_pause_recording"
        const val ACTION_RESUME_RECORDING = "action_resume_recording"

        const val EXTRA_RECORDING_ID = "recording_id"

        fun startRecording(context: Context, recordingId: String) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_RECORDING_ID, recordingId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val recordingId = intent.getStringExtra(EXTRA_RECORDING_ID)
                    ?: java.util.UUID.randomUUID().toString()
                startRecording(recordingId)
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
            ACTION_PAUSE_RECORDING -> {
                pauseRecording()
            }
            ACTION_RESUME_RECORDING -> {
                resumeRecording()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun startRecording(recordingId: String) {
        currentRecordingId = recordingId

        val result = audioRecorder.startRecording(recordingId)
        result.onSuccess { file ->
            recordingFile = file
            _recordingState.value = RecordingState.Recording(recordingId, 0)
            startForeground(NOTIFICATION_ID, createNotification("Recording..."))
            startTimer()
        }.onFailure { error ->
            _recordingState.value = RecordingState.Error(error.message ?: "Failed to start recording")
            stopSelf()
        }
    }

    private fun stopRecording() {
        timerJob?.cancel()
        timerJob = null

        val result = audioRecorder.stopRecording()
        result.onSuccess { file ->
            recordingFile = file
            _recordingState.value = RecordingState.Completed(currentRecordingId ?: "", file)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }.onFailure { error ->
            _recordingState.value = RecordingState.Error(error.message ?: "Failed to stop recording")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioRecorder.pauseRecording()
            timerJob?.cancel()
            _recordingState.value = RecordingState.Paused(
                currentRecordingId ?: "",
                audioRecorder.getCurrentDuration()
            )
            updateNotification("Recording paused")
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioRecorder.resumeRecording()
            startTimer()
            _recordingState.value = RecordingState.Recording(
                currentRecordingId ?: "",
                audioRecorder.getCurrentDuration()
            )
            updateNotification("Recording...")
        }
    }

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000) // Update every second
                audioRecorder.updateDuration()
                val duration = audioRecorder.getCurrentDuration()
                _recordingState.value = RecordingState.Recording(currentRecordingId ?: "", duration)
                updateNotification("Recording - ${formatDuration(duration)}")
            }
        }
    }

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for audio recording"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        // TODO: Create pending intent to open app when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BisonNotes AI")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You'll need to add this icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        audioRecorder.release()
    }

    /**
     * Binder for clients
     */
    inner class RecordingServiceBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    /**
     * Recording state sealed class
     */
    sealed class RecordingState {
        object Idle : RecordingState()
        data class Recording(val recordingId: String, val duration: Long) : RecordingState()
        data class Paused(val recordingId: String, val duration: Long) : RecordingState()
        data class Completed(val recordingId: String, val file: File) : RecordingState()
        data class Error(val message: String) : RecordingState()
    }
}
