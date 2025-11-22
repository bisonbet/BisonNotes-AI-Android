package com.bisonnotesai.android.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bisonnotesai.android.data.local.database.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for SummaryEntity
 * Maps to iOS CoreDataManager summary operations
 */
@Dao
interface SummaryDao {

    /**
     * Get all summaries ordered by generation date (newest first)
     * Maps to: getAllSummaries()
     */
    @Query("SELECT * FROM summaries ORDER BY generatedAt DESC")
    fun getAllSummaries(): Flow<List<SummaryEntity>>

    /**
     * Get summary by ID
     */
    @Query("SELECT * FROM summaries WHERE id = :id")
    suspend fun getSummary(id: String): SummaryEntity?

    /**
     * Get summary by ID as Flow
     */
    @Query("SELECT * FROM summaries WHERE id = :id")
    fun getSummaryFlow(id: String): Flow<SummaryEntity?>

    /**
     * Get summary for a specific recording
     * Maps to: getSummary(for recordingId: UUID)
     */
    @Query("SELECT * FROM summaries WHERE recordingId = :recordingId ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getSummaryForRecording(recordingId: String): SummaryEntity?

    /**
     * Get summary for a specific recording as Flow
     */
    @Query("SELECT * FROM summaries WHERE recordingId = :recordingId ORDER BY generatedAt DESC LIMIT 1")
    fun getSummaryForRecordingFlow(recordingId: String): Flow<SummaryEntity?>

    /**
     * Get all summaries for a recording (supports multiple versions)
     */
    @Query("SELECT * FROM summaries WHERE recordingId = :recordingId ORDER BY version DESC, generatedAt DESC")
    fun getAllSummariesForRecording(recordingId: String): Flow<List<SummaryEntity>>

    /**
     * Get summaries by AI method/engine
     */
    @Query("SELECT * FROM summaries WHERE aiMethod = :aiMethod ORDER BY generatedAt DESC")
    fun getSummariesByMethod(aiMethod: String): Flow<List<SummaryEntity>>

    /**
     * Get summaries by content type
     */
    @Query("SELECT * FROM summaries WHERE contentType = :contentType ORDER BY generatedAt DESC")
    fun getSummariesByContentType(contentType: String): Flow<List<SummaryEntity>>

    /**
     * Get summaries with tasks
     */
    @Query("SELECT * FROM summaries WHERE tasks IS NOT NULL AND tasks != '' ORDER BY generatedAt DESC")
    fun getSummariesWithTasks(): Flow<List<SummaryEntity>>

    /**
     * Get summaries with reminders
     */
    @Query("SELECT * FROM summaries WHERE reminders IS NOT NULL AND reminders != '' ORDER BY generatedAt DESC")
    fun getSummariesWithReminders(): Flow<List<SummaryEntity>>

    /**
     * Insert a new summary
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SummaryEntity): Long

    /**
     * Update an existing summary
     */
    @Update
    suspend fun update(summary: SummaryEntity)

    /**
     * Delete a summary
     * Maps to: deleteSummary(id: UUID?)
     */
    @Delete
    suspend fun delete(summary: SummaryEntity)

    /**
     * Delete summary by ID
     */
    @Query("DELETE FROM summaries WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete all summaries for a recording
     */
    @Query("DELETE FROM summaries WHERE recordingId = :recordingId")
    suspend fun deleteSummariesForRecording(recordingId: String)

    /**
     * Get count of all summaries
     */
    @Query("SELECT COUNT(*) FROM summaries")
    suspend fun getSummaryCount(): Int

    /**
     * Get summaries with high confidence (>= threshold)
     */
    @Query("SELECT * FROM summaries WHERE confidence >= :threshold ORDER BY generatedAt DESC")
    fun getHighConfidenceSummaries(threshold: Double = 0.8): Flow<List<SummaryEntity>>

    /**
     * Find orphaned summaries (recording relationship is null in Room, but recordingId is set)
     * Used for data integrity checks
     */
    @Query("""
        SELECT * FROM summaries
        WHERE recordingId NOT IN (SELECT id FROM recordings)
    """)
    suspend fun getOrphanedSummaries(): List<SummaryEntity>
}
