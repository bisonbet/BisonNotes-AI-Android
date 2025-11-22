package com.bisonnotesai.android.transcription

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for coordinating transcription jobs
 * Handles starting, monitoring, and canceling transcription work
 */
@Singleton
class TranscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Start transcription for a recording
     * Returns UUID of the work request for tracking
     */
    fun startTranscription(
        recordingId: String,
        audioFilePath: String,
        language: String = "en-US"
    ): UUID {
        // Create input data
        val inputData = workDataOf(
            TranscriptionWorker.KEY_RECORDING_ID to recordingId,
            TranscriptionWorker.KEY_AUDIO_FILE_PATH to audioFilePath,
            TranscriptionWorker.KEY_LANGUAGE to language
        )

        // Create work constraints
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Create work request
        val workRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("transcription")
            .addTag("recording_$recordingId")
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        // Enqueue work
        workManager.enqueueUniqueWork(
            "transcription_$recordingId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return workRequest.id
    }

    /**
     * Get transcription progress for a work request
     */
    fun getTranscriptionProgress(workId: UUID): Flow<WorkInfo?> {
        return workManager.getWorkInfoByIdFlow(workId)
    }

    /**
     * Get transcription progress by recording ID
     */
    fun getTranscriptionProgressByRecording(recordingId: String): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagFlow("recording_$recordingId")
    }

    /**
     * Cancel transcription for a recording
     */
    fun cancelTranscription(recordingId: String) {
        workManager.cancelUniqueWork("transcription_$recordingId")
    }

    /**
     * Cancel transcription by work ID
     */
    fun cancelTranscription(workId: UUID) {
        workManager.cancelWorkById(workId)
    }

    /**
     * Get all active transcription jobs
     */
    fun getActiveTranscriptions(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagFlow("transcription")
            .map { workInfos ->
                workInfos.filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            }
    }

    /**
     * Check if transcription is in progress for a recording
     */
    fun isTranscriptionInProgress(recordingId: String): Flow<Boolean> {
        return getTranscriptionProgressByRecording(recordingId)
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            }
    }

    /**
     * Get transcription result from work info
     */
    fun getTranscriptionResult(workInfo: WorkInfo): TranscriptionJobResult {
        return when (workInfo.state) {
            WorkInfo.State.SUCCEEDED -> {
                val transcriptId = workInfo.outputData.getString(TranscriptionWorker.KEY_TRANSCRIPT_ID)
                TranscriptionJobResult.Success(transcriptId ?: "")
            }
            WorkInfo.State.FAILED -> {
                val errorMessage = workInfo.outputData.getString(TranscriptionWorker.KEY_ERROR_MESSAGE)
                TranscriptionJobResult.Failed(errorMessage ?: "Unknown error")
            }
            WorkInfo.State.RUNNING -> {
                val progress = workInfo.progress.getInt(TranscriptionWorker.KEY_PROGRESS, 0)
                TranscriptionJobResult.InProgress(progress)
            }
            WorkInfo.State.ENQUEUED -> {
                TranscriptionJobResult.Queued
            }
            WorkInfo.State.CANCELLED -> {
                TranscriptionJobResult.Cancelled
            }
            else -> {
                TranscriptionJobResult.Unknown
            }
        }
    }

    /**
     * Retry failed transcription
     */
    fun retryTranscription(recordingId: String, audioFilePath: String, language: String = "en-US"): UUID {
        // Cancel any existing work
        cancelTranscription(recordingId)
        // Start new transcription
        return startTranscription(recordingId, audioFilePath, language)
    }
}

/**
 * Transcription job result
 */
sealed class TranscriptionJobResult {
    object Queued : TranscriptionJobResult()
    data class InProgress(val progress: Int) : TranscriptionJobResult()
    data class Success(val transcriptId: String) : TranscriptionJobResult()
    data class Failed(val errorMessage: String) : TranscriptionJobResult()
    object Cancelled : TranscriptionJobResult()
    object Unknown : TranscriptionJobResult()
}
