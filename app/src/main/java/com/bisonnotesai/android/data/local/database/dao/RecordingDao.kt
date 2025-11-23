package com.bisonnotesai.android.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bisonnotesai.android.data.local.database.entity.RecordingEntity
import com.bisonnotesai.android.data.local.database.entity.RecordingWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * DAO for RecordingEntity
 * Maps to iOS CoreDataManager recording operations
 */
@Dao
interface RecordingDao {

    /**
     * Get all recordings ordered by recording date (newest first)
     * Maps to: getAllRecordings()
     */
    @Query("SELECT * FROM recordings ORDER BY recordingDate DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    /**
     * Get a single recording by ID
     * Maps to: getRecording(id: UUID)
     */
    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecording(id: String): RecordingEntity?

    /**
     * Get a single recording by ID as Flow
     */
    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecordingFlow(id: String): Flow<RecordingEntity?>

    /**
     * Get recording by name
     * Maps to: getRecording(name: String)
     */
    @Query("SELECT * FROM recordings WHERE recordingName = :name LIMIT 1")
    suspend fun getRecordingByName(name: String): RecordingEntity?

    /**
     * Get recording by URL (searches by filename)
     * Maps to: getRecording(url: URL)
     */
    @Query("SELECT * FROM recordings WHERE recordingURL LIKE '%' || :filename")
    suspend fun getRecordingByFilename(filename: String): RecordingEntity?

    /**
     * Get recording with all related details (transcript, summary, jobs)
     * Maps to: getRecordingWithTranscriptAndSummary(id: String)
     */
    @Transaction
    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingWithDetails(id: String): RecordingWithDetails?

    /**
     * Get all recordings with their details
     * Maps to: getAllRecordingsWithData()
     */
    @Transaction
    @Query("SELECT * FROM recordings ORDER BY recordingDate DESC")
    fun getAllRecordingsWithDetails(): Flow<List<RecordingWithDetails>>

    /**
     * Get recordings that have transcripts
     * Maps to: getRecordingsWithTranscripts()
     */
    @Transaction
    @Query("""
        SELECT * FROM recordings
        WHERE id IN (SELECT DISTINCT recordingId FROM transcripts)
        ORDER BY recordingDate DESC
    """)
    fun getRecordingsWithTranscripts(): Flow<List<RecordingWithDetails>>

    /**
     * Get recordings that have summaries
     */
    @Transaction
    @Query("""
        SELECT * FROM recordings
        WHERE id IN (SELECT DISTINCT recordingId FROM summaries)
        ORDER BY recordingDate DESC
    """)
    fun getRecordingsWithSummaries(): Flow<List<RecordingWithDetails>>

    /**
     * Insert a new recording
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity): Long

    /**
     * Update an existing recording
     */
    @Update
    suspend fun update(recording: RecordingEntity)

    /**
     * Delete a recording (will cascade delete transcript, summary, and jobs)
     * Maps to: deleteRecording(id: UUID)
     */
    @Delete
    suspend fun delete(recording: RecordingEntity)

    /**
     * Delete recording by ID
     */
    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Update recording name
     * Maps to: updateRecordingName(for recordingId: UUID, newName: String)
     */
    @Query("UPDATE recordings SET recordingName = :newName, lastModified = :timestamp WHERE id = :id")
    suspend fun updateRecordingName(id: String, newName: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update recording URL (for path migration)
     * Maps to: updateRecordingURL(recording:newURL:)
     */
    @Query("UPDATE recordings SET recordingURL = :newUrl, lastModified = :timestamp WHERE id = :id")
    suspend fun updateRecordingURL(id: String, newUrl: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update transcription status
     */
    @Query("""
        UPDATE recordings
        SET transcriptionStatus = :status,
            transcriptId = :transcriptId,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun updateTranscriptionStatus(
        id: String,
        status: String,
        transcriptId: String?,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Update summary status
     */
    @Query("""
        UPDATE recordings
        SET summaryStatus = :status,
            summaryId = :summaryId,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun updateSummaryStatus(
        id: String,
        status: String,
        summaryId: String?,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get count of all recordings
     */
    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun getRecordingCount(): Int

    /**
     * Clean up orphaned recordings (no URL, no transcript, no summary)
     * Maps to: cleanupOrphanedRecordings()
     */
    @Query("""
        DELETE FROM recordings
        WHERE recordingURL IS NULL
        AND id NOT IN (SELECT recordingId FROM transcripts WHERE recordingId IS NOT NULL)
        AND id NOT IN (SELECT recordingId FROM summaries WHERE recordingId IS NOT NULL)
    """)
    suspend fun cleanupOrphanedRecordings(): Int

    /**
     * Get recordings with missing files (for cleanup)
     * Returns recordings that have a URL set but might not exist on disk
     */
    @Query("SELECT * FROM recordings WHERE recordingURL IS NOT NULL AND recordingURL != ''")
    suspend fun getRecordingsWithURLs(): List<RecordingEntity>
}
