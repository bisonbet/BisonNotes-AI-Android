package com.bisonnotesai.android.data.repository

import com.bisonnotesai.android.data.local.database.dao.ProcessingJobDao
import com.bisonnotesai.android.data.mapper.ProcessingJobMapper
import com.bisonnotesai.android.domain.model.JobStatus
import com.bisonnotesai.android.domain.model.JobType
import com.bisonnotesai.android.domain.model.ProcessingJob
import com.bisonnotesai.android.domain.repository.ProcessingJobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of ProcessingJobRepository
 * Bridges domain layer with data layer using DAOs and mappers
 */
class ProcessingJobRepositoryImpl @Inject constructor(
    private val processingJobDao: ProcessingJobDao,
    private val mapper: ProcessingJobMapper
) : ProcessingJobRepository {

    override fun getAllProcessingJobs(): Flow<List<ProcessingJob>> {
        return processingJobDao.getAllProcessingJobs()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun getProcessingJob(id: String): ProcessingJob? {
        return processingJobDao.getProcessingJob(id)?.let { mapper.toDomain(it) }
    }

    override fun getProcessingJobFlow(id: String): Flow<ProcessingJob?> {
        return processingJobDao.getProcessingJobFlow(id)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }

    override fun getActiveProcessingJobs(): Flow<List<ProcessingJob>> {
        return processingJobDao.getActiveProcessingJobs()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getJobsForRecording(recordingId: String): Flow<List<ProcessingJob>> {
        return processingJobDao.getJobsForRecording(recordingId)
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getJobsByStatus(status: JobStatus): Flow<List<ProcessingJob>> {
        return processingJobDao.getJobsByStatus(status.name.lowercase())
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getJobsByType(jobType: JobType): Flow<List<ProcessingJob>> {
        return processingJobDao.getJobsByType(jobType.name.lowercase())
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getCompletedJobs(): Flow<List<ProcessingJob>> {
        return processingJobDao.getCompletedJobs()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getFailedJobs(): Flow<List<ProcessingJob>> {
        return processingJobDao.getFailedJobs()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun saveProcessingJob(job: ProcessingJob): Result<String> {
        return try {
            val entity = mapper.toEntity(job)
            processingJobDao.insert(entity)
            Result.success(job.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProcessingJob(job: ProcessingJob): Result<Unit> {
        return try {
            val entity = mapper.toEntity(job)
            processingJobDao.update(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProcessingJob(id: String): Result<Unit> {
        return try {
            processingJobDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCompletedJobs(): Int {
        return processingJobDao.deleteCompletedJobs()
    }

    override suspend fun deleteJobsForRecording(recordingId: String): Result<Unit> {
        return try {
            processingJobDao.deleteJobsForRecording(recordingId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStatus(id: String, status: JobStatus): Result<Unit> {
        return try {
            processingJobDao.updateStatus(id, status.name.lowercase())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProgress(id: String, progress: Double): Result<Unit> {
        return try {
            processingJobDao.updateProgress(id, progress)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsCompleted(id: String): Result<Unit> {
        return try {
            processingJobDao.markAsCompleted(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsFailed(id: String, error: String): Result<Unit> {
        return try {
            processingJobDao.markAsFailed(id, error)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveJobCount(): Int {
        return processingJobDao.getActiveJobCount()
    }

    override suspend fun getJobCount(): Int {
        return processingJobDao.getJobCount()
    }
}
