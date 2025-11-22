package com.bisonnotesai.android.data.mapper

import com.bisonnotesai.android.data.local.database.entity.ProcessingJobEntity
import com.bisonnotesai.android.domain.model.JobStatus
import com.bisonnotesai.android.domain.model.JobType
import com.bisonnotesai.android.domain.model.ProcessingJob
import javax.inject.Inject

/**
 * Mapper between ProcessingJobEntity (data layer) and ProcessingJob (domain layer)
 */
class ProcessingJobMapper @Inject constructor() {

    /**
     * Convert ProcessingJobEntity to ProcessingJob domain model
     */
    fun toDomain(entity: ProcessingJobEntity): ProcessingJob {
        return ProcessingJob(
            id = entity.id,
            recordingId = entity.recordingId,
            jobType = JobType.fromString(entity.jobType),
            engine = entity.engine,
            status = JobStatus.fromString(entity.status),
            progress = entity.progress,
            recordingName = entity.recordingName,
            recordingUrl = entity.recordingURL,
            error = entity.error,
            startTime = entity.startTime,
            completionTime = entity.completionTime,
            lastModified = entity.lastModified
        )
    }

    /**
     * Convert ProcessingJob domain model to ProcessingJobEntity
     */
    fun toEntity(domain: ProcessingJob): ProcessingJobEntity {
        return ProcessingJobEntity(
            id = domain.id,
            recordingId = domain.recordingId,
            jobType = domain.jobType.name.lowercase(),
            engine = domain.engine,
            status = domain.status.name.lowercase(),
            progress = domain.progress,
            recordingName = domain.recordingName,
            recordingURL = domain.recordingUrl,
            error = domain.error,
            startTime = domain.startTime,
            completionTime = domain.completionTime,
            lastModified = domain.lastModified
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<ProcessingJobEntity>): List<ProcessingJob> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(domainList: List<ProcessingJob>): List<ProcessingJobEntity> {
        return domainList.map { toEntity(it) }
    }
}
