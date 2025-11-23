package com.bisonnotesai.android.domain.repository

import com.bisonnotesai.android.domain.model.Recording
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Recording operations
 * Clean abstraction over data layer
 */
interface RecordingRepository {

    /**
     * Get all recordings as Flow (reactive)
     */
    fun getAllRecordings(): Flow<List<Recording>>

    /**
     * Get a single recording by ID
     */
    suspend fun getRecording(id: String): Recording?

    /**
     * Get recording by ID as Flow
     */
    fun getRecordingFlow(id: String): Flow<Recording?>

    /**
     * Get recordings with transcripts
     */
    fun getRecordingsWithTranscripts(): Flow<List<Recording>>

    /**
     * Get recordings with summaries
     */
    fun getRecordingsWithSummaries(): Flow<List<Recording>>

    /**
     * Save a new recording or update existing
     */
    suspend fun saveRecording(recording: Recording): Result<String>

    /**
     * Update an existing recording
     */
    suspend fun updateRecording(recording: Recording): Result<Unit>

    /**
     * Delete a recording by ID
     */
    suspend fun deleteRecording(id: String): Result<Unit>

    /**
     * Update recording name
     */
    suspend fun updateRecordingName(id: String, newName: String): Result<Unit>

    /**
     * Update transcription status
     */
    suspend fun updateTranscriptionStatus(id: String, status: String, transcriptId: String?): Result<Unit>

    /**
     * Update summary status
     */
    suspend fun updateSummaryStatus(id: String, status: String, summaryId: String?): Result<Unit>

    /**
     * Get count of all recordings
     */
    suspend fun getRecordingCount(): Int

    /**
     * Clean up orphaned recordings
     */
    suspend fun cleanupOrphanedRecordings(): Int
}
