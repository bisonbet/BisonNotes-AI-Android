package com.bisonnotesai.android.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bisonnotesai.android.data.local.database.entity.ProcessingJobEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for ProcessingJobEntity
 * Maps to iOS CoreDataManager processing job operations
 */
@Dao
interface ProcessingJobDao {

    /**
     * Get all processing jobs ordered by start time (newest first)
     * Maps to: getAllProcessingJobs()
     */
    @Query("SELECT * FROM processing_jobs ORDER BY startTime DESC")
    fun getAllProcessingJobs(): Flow<List<ProcessingJobEntity>>

    /**
     * Get processing job by ID
     * Maps to: getProcessingJob(id: UUID)
     */
    @Query("SELECT * FROM processing_jobs WHERE id = :id")
    suspend fun getProcessingJob(id: String): ProcessingJobEntity?

    /**
     * Get processing job by ID as Flow
     */
    @Query("SELECT * FROM processing_jobs WHERE id = :id")
    fun getProcessingJobFlow(id: String): Flow<ProcessingJobEntity?>

    /**
     * Get active processing jobs (queued or processing)
     * Maps to: getActiveProcessingJobs()
     */
    @Query("""
        SELECT * FROM processing_jobs
        WHERE status IN ('queued', 'processing')
        ORDER BY startTime ASC
    """)
    fun getActiveProcessingJobs(): Flow<List<ProcessingJobEntity>>

    /**
     * Get processing jobs for a specific recording
     */
    @Query("SELECT * FROM processing_jobs WHERE recordingId = :recordingId ORDER BY startTime DESC")
    fun getJobsForRecording(recordingId: String): Flow<List<ProcessingJobEntity>>

    /**
     * Get processing jobs by status
     */
    @Query("SELECT * FROM processing_jobs WHERE status = :status ORDER BY startTime DESC")
    fun getJobsByStatus(status: String): Flow<List<ProcessingJobEntity>>

    /**
     * Get processing jobs by type
     */
    @Query("SELECT * FROM processing_jobs WHERE jobType = :jobType ORDER BY startTime DESC")
    fun getJobsByType(jobType: String): Flow<List<ProcessingJobEntity>>

    /**
     * Get completed jobs
     */
    @Query("""
        SELECT * FROM processing_jobs
        WHERE status IN ('completed', 'failed')
        ORDER BY completionTime DESC
    """)
    fun getCompletedJobs(): Flow<List<ProcessingJobEntity>>

    /**
     * Get failed jobs
     */
    @Query("SELECT * FROM processing_jobs WHERE status = 'failed' ORDER BY startTime DESC")
    fun getFailedJobs(): Flow<List<ProcessingJobEntity>>

    /**
     * Insert a new processing job
     * Maps to: createProcessingJob(...)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: ProcessingJobEntity): Long

    /**
     * Update an existing processing job
     * Maps to: updateProcessingJob(_ job: ProcessingJobEntry)
     */
    @Update
    suspend fun update(job: ProcessingJobEntity)

    /**
     * Delete a processing job
     * Maps to: deleteProcessingJob(_ job: ProcessingJobEntry)
     */
    @Delete
    suspend fun delete(job: ProcessingJobEntity)

    /**
     * Delete processing job by ID
     */
    @Query("DELETE FROM processing_jobs WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete completed processing jobs
     * Maps to: deleteCompletedProcessingJobs()
     */
    @Query("DELETE FROM processing_jobs WHERE status IN ('completed', 'failed')")
    suspend fun deleteCompletedJobs(): Int

    /**
     * Delete all jobs for a recording
     */
    @Query("DELETE FROM processing_jobs WHERE recordingId = :recordingId")
    suspend fun deleteJobsForRecording(recordingId: String)

    /**
     * Update job status
     */
    @Query("""
        UPDATE processing_jobs
        SET status = :status,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun updateStatus(id: String, status: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Update job progress
     */
    @Query("""
        UPDATE processing_jobs
        SET progress = :progress,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun updateProgress(id: String, progress: Double, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark job as completed
     */
    @Query("""
        UPDATE processing_jobs
        SET status = 'completed',
            progress = 100.0,
            completionTime = :completionTime,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun markAsCompleted(
        id: String,
        completionTime: Long = System.currentTimeMillis(),
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Mark job as failed
     */
    @Query("""
        UPDATE processing_jobs
        SET status = 'failed',
            error = :error,
            completionTime = :completionTime,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun markAsFailed(
        id: String,
        error: String,
        completionTime: Long = System.currentTimeMillis(),
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get count of active jobs
     */
    @Query("SELECT COUNT(*) FROM processing_jobs WHERE status IN ('queued', 'processing')")
    suspend fun getActiveJobCount(): Int

    /**
     * Get count of all jobs
     */
    @Query("SELECT COUNT(*) FROM processing_jobs")
    suspend fun getJobCount(): Int
}
