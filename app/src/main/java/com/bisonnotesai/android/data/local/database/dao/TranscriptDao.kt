package com.bisonnotesai.android.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bisonnotesai.android.data.local.database.entity.TranscriptEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for TranscriptEntity
 * Maps to iOS CoreDataManager transcript operations
 */
@Dao
interface TranscriptDao {

    /**
     * Get all transcripts ordered by creation date (newest first)
     * Maps to: getAllTranscripts()
     */
    @Query("SELECT * FROM transcripts ORDER BY createdAt DESC")
    fun getAllTranscripts(): Flow<List<TranscriptEntity>>

    /**
     * Get transcript by ID
     */
    @Query("SELECT * FROM transcripts WHERE id = :id")
    suspend fun getTranscript(id: String): TranscriptEntity?

    /**
     * Get transcript by ID as Flow
     */
    @Query("SELECT * FROM transcripts WHERE id = :id")
    fun getTranscriptFlow(id: String): Flow<TranscriptEntity?>

    /**
     * Get transcript for a specific recording (most recent if multiple)
     * Maps to: getTranscript(for recordingId: UUID)
     */
    @Query("SELECT * FROM transcripts WHERE recordingId = :recordingId ORDER BY lastModified DESC LIMIT 1")
    suspend fun getTranscriptForRecording(recordingId: String): TranscriptEntity?

    /**
     * Get transcript for a specific recording as Flow
     */
    @Query("SELECT * FROM transcripts WHERE recordingId = :recordingId ORDER BY lastModified DESC LIMIT 1")
    fun getTranscriptForRecordingFlow(recordingId: String): Flow<TranscriptEntity?>

    /**
     * Get all transcripts for a recording (supports multiple versions)
     */
    @Query("SELECT * FROM transcripts WHERE recordingId = :recordingId ORDER BY createdAt DESC")
    fun getAllTranscriptsForRecording(recordingId: String): Flow<List<TranscriptEntity>>

    /**
     * Get transcripts by engine
     */
    @Query("SELECT * FROM transcripts WHERE engine = :engine ORDER BY createdAt DESC")
    fun getTranscriptsByEngine(engine: String): Flow<List<TranscriptEntity>>

    /**
     * Insert a new transcript
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcript: TranscriptEntity): Long

    /**
     * Update an existing transcript
     */
    @Update
    suspend fun update(transcript: TranscriptEntity)

    /**
     * Delete a transcript
     * Maps to: deleteTranscript(id: UUID?)
     */
    @Delete
    suspend fun delete(transcript: TranscriptEntity)

    /**
     * Delete transcript by ID
     */
    @Query("DELETE FROM transcripts WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete all transcripts for a recording
     */
    @Query("DELETE FROM transcripts WHERE recordingId = :recordingId")
    suspend fun deleteTranscriptsForRecording(recordingId: String)

    /**
     * Get count of all transcripts
     */
    @Query("SELECT COUNT(*) FROM transcripts")
    suspend fun getTranscriptCount(): Int

    /**
     * Get transcripts with high confidence (>= threshold)
     */
    @Query("SELECT * FROM transcripts WHERE confidence >= :threshold ORDER BY createdAt DESC")
    fun getHighConfidenceTranscripts(threshold: Double = 0.8): Flow<List<TranscriptEntity>>
}
