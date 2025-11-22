package com.bisonnotesai.android.domain.repository

import com.bisonnotesai.android.domain.model.AIEngine
import com.bisonnotesai.android.domain.model.ContentType
import com.bisonnotesai.android.domain.model.Summary
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Summary operations
 * Clean abstraction over data layer
 */
interface SummaryRepository {

    /**
     * Get all summaries as Flow (reactive)
     */
    fun getAllSummaries(): Flow<List<Summary>>

    /**
     * Get a single summary by ID
     */
    suspend fun getSummary(id: String): Summary?

    /**
     * Get summary by ID as Flow
     */
    fun getSummaryFlow(id: String): Flow<Summary?>

    /**
     * Get summary for a specific recording (most recent if multiple)
     */
    suspend fun getSummaryForRecording(recordingId: String): Summary?

    /**
     * Get summary for a specific recording as Flow
     */
    fun getSummaryForRecordingFlow(recordingId: String): Flow<Summary?>

    /**
     * Get all summaries for a recording (supports multiple versions)
     */
    fun getAllSummariesForRecording(recordingId: String): Flow<List<Summary>>

    /**
     * Get summaries by AI method/engine
     */
    fun getSummariesByMethod(aiEngine: AIEngine): Flow<List<Summary>>

    /**
     * Get summaries by content type
     */
    fun getSummariesByContentType(contentType: ContentType): Flow<List<Summary>>

    /**
     * Get summaries with tasks
     */
    fun getSummariesWithTasks(): Flow<List<Summary>>

    /**
     * Get summaries with reminders
     */
    fun getSummariesWithReminders(): Flow<List<Summary>>

    /**
     * Save a new summary or update existing
     */
    suspend fun saveSummary(summary: Summary): Result<String>

    /**
     * Update an existing summary
     */
    suspend fun updateSummary(summary: Summary): Result<Unit>

    /**
     * Delete a summary by ID
     */
    suspend fun deleteSummary(id: String): Result<Unit>

    /**
     * Delete all summaries for a recording
     */
    suspend fun deleteSummariesForRecording(recordingId: String): Result<Unit>

    /**
     * Get count of all summaries
     */
    suspend fun getSummaryCount(): Int

    /**
     * Get summaries with high confidence (>= threshold)
     */
    fun getHighConfidenceSummaries(threshold: Double = 0.8): Flow<List<Summary>>

    /**
     * Find orphaned summaries (recording relationship is null)
     * Used for data integrity checks
     */
    suspend fun getOrphanedSummaries(): List<Summary>
}
