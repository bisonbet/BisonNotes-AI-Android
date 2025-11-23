package com.bisonnotesai.android.domain.repository

import com.bisonnotesai.android.domain.model.JobStatus
import com.bisonnotesai.android.domain.model.JobType
import com.bisonnotesai.android.domain.model.ProcessingJob
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ProcessingJob operations
 * Clean abstraction over data layer
 */
interface ProcessingJobRepository {

    /**
     * Get all processing jobs as Flow (reactive)
     */
    fun getAllProcessingJobs(): Flow<List<ProcessingJob>>

    /**
     * Get a single processing job by ID
     */
    suspend fun getProcessingJob(id: String): ProcessingJob?

    /**
     * Get processing job by ID as Flow
     */
    fun getProcessingJobFlow(id: String): Flow<ProcessingJob?>

    /**
     * Get active processing jobs (queued or processing)
     */
    fun getActiveProcessingJobs(): Flow<List<ProcessingJob>>

    /**
     * Get processing jobs for a specific recording
     */
    fun getJobsForRecording(recordingId: String): Flow<List<ProcessingJob>>

    /**
     * Get processing jobs by status
     */
    fun getJobsByStatus(status: JobStatus): Flow<List<ProcessingJob>>

    /**
     * Get processing jobs by type
     */
    fun getJobsByType(jobType: JobType): Flow<List<ProcessingJob>>

    /**
     * Get completed jobs (success or failure)
     */
    fun getCompletedJobs(): Flow<List<ProcessingJob>>

    /**
     * Get failed jobs
     */
    fun getFailedJobs(): Flow<List<ProcessingJob>>

    /**
     * Save a new processing job or update existing
     */
    suspend fun saveProcessingJob(job: ProcessingJob): Result<String>

    /**
     * Update an existing processing job
     */
    suspend fun updateProcessingJob(job: ProcessingJob): Result<Unit>

    /**
     * Delete a processing job by ID
     */
    suspend fun deleteProcessingJob(id: String): Result<Unit>

    /**
     * Delete completed processing jobs (both success and failed)
     * Returns count of deleted jobs
     */
    suspend fun deleteCompletedJobs(): Int

    /**
     * Delete all jobs for a recording
     */
    suspend fun deleteJobsForRecording(recordingId: String): Result<Unit>

    /**
     * Update job status
     */
    suspend fun updateStatus(id: String, status: JobStatus): Result<Unit>

    /**
     * Update job progress (0.0 to 100.0)
     */
    suspend fun updateProgress(id: String, progress: Double): Result<Unit>

    /**
     * Mark job as completed
     */
    suspend fun markAsCompleted(id: String): Result<Unit>

    /**
     * Mark job as failed with error message
     */
    suspend fun markAsFailed(id: String, error: String): Result<Unit>

    /**
     * Get count of active jobs
     */
    suspend fun getActiveJobCount(): Int

    /**
     * Get count of all jobs
     */
    suspend fun getJobCount(): Int
}
