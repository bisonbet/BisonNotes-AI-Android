package com.bisonnotesai.android.domain.repository

import com.bisonnotesai.android.domain.model.Transcript
import com.bisonnotesai.android.domain.model.TranscriptionEngine
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Transcript operations
 * Clean abstraction over data layer
 */
interface TranscriptRepository {

    /**
     * Get all transcripts as Flow (reactive)
     */
    fun getAllTranscripts(): Flow<List<Transcript>>

    /**
     * Get a single transcript by ID
     */
    suspend fun getTranscript(id: String): Transcript?

    /**
     * Get transcript by ID as Flow
     */
    fun getTranscriptFlow(id: String): Flow<Transcript?>

    /**
     * Get transcript for a specific recording (most recent if multiple)
     */
    suspend fun getTranscriptForRecording(recordingId: String): Transcript?

    /**
     * Get transcript for a specific recording as Flow
     */
    fun getTranscriptForRecordingFlow(recordingId: String): Flow<Transcript?>

    /**
     * Get all transcripts for a recording (supports multiple versions)
     */
    fun getAllTranscriptsForRecording(recordingId: String): Flow<List<Transcript>>

    /**
     * Get transcripts by engine
     */
    fun getTranscriptsByEngine(engine: TranscriptionEngine): Flow<List<Transcript>>

    /**
     * Save a new transcript or update existing
     */
    suspend fun saveTranscript(transcript: Transcript): Result<String>

    /**
     * Update an existing transcript
     */
    suspend fun updateTranscript(transcript: Transcript): Result<Unit>

    /**
     * Delete a transcript by ID
     */
    suspend fun deleteTranscript(id: String): Result<Unit>

    /**
     * Delete all transcripts for a recording
     */
    suspend fun deleteTranscriptsForRecording(recordingId: String): Result<Unit>

    /**
     * Get count of all transcripts
     */
    suspend fun getTranscriptCount(): Int

    /**
     * Get transcripts with high confidence (>= threshold)
     */
    fun getHighConfidenceTranscripts(threshold: Double = 0.8): Flow<List<Transcript>>
}
