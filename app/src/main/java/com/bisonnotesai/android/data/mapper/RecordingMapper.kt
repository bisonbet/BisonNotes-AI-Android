package com.bisonnotesai.android.data.mapper

import com.bisonnotesai.android.data.local.database.entity.RecordingEntity
import com.bisonnotesai.android.domain.model.LocationData
import com.bisonnotesai.android.domain.model.ProcessingStatus
import com.bisonnotesai.android.domain.model.Recording
import javax.inject.Inject

/**
 * Mapper between RecordingEntity (data layer) and Recording (domain layer)
 */
class RecordingMapper @Inject constructor() {

    /**
     * Convert RecordingEntity to Recording domain model
     */
    fun toDomain(entity: RecordingEntity): Recording {
        return Recording(
            id = entity.id,
            name = entity.recordingName ?: "",
            date = entity.recordingDate ?: entity.createdAt,
            url = entity.recordingURL,
            duration = entity.duration,
            fileSize = entity.fileSize,
            audioQuality = entity.audioQuality,
            location = extractLocationData(entity),
            transcriptionStatus = ProcessingStatus.fromString(entity.transcriptionStatus),
            summaryStatus = ProcessingStatus.fromString(entity.summaryStatus),
            transcriptId = entity.transcriptId,
            summaryId = entity.summaryId,
            createdAt = entity.createdAt,
            lastModified = entity.lastModified
        )
    }

    /**
     * Convert Recording domain model to RecordingEntity
     */
    fun toEntity(domain: Recording): RecordingEntity {
        return RecordingEntity(
            id = domain.id,
            recordingName = domain.name,
            recordingDate = domain.date,
            recordingURL = domain.url,
            duration = domain.duration,
            fileSize = domain.fileSize,
            audioQuality = domain.audioQuality,
            locationLatitude = domain.location?.latitude ?: 0.0,
            locationLongitude = domain.location?.longitude ?: 0.0,
            locationAccuracy = domain.location?.accuracy ?: 0.0,
            locationAddress = domain.location?.address,
            locationTimestamp = domain.location?.timestamp,
            transcriptionStatus = domain.transcriptionStatus.name.lowercase(),
            summaryStatus = domain.summaryStatus.name.lowercase(),
            transcriptId = domain.transcriptId,
            summaryId = domain.summaryId,
            createdAt = domain.createdAt,
            lastModified = domain.lastModified
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<RecordingEntity>): List<Recording> {
        return entities.map { toDomain(it) }
    }

    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(domainList: List<Recording>): List<RecordingEntity> {
        return domainList.map { toEntity(it) }
    }

    /**
     * Extract location data from entity
     */
    private fun extractLocationData(entity: RecordingEntity): LocationData? {
        // Check if location data exists (non-zero coordinates)
        if (entity.locationLatitude == 0.0 && entity.locationLongitude == 0.0) {
            return null
        }

        return LocationData(
            latitude = entity.locationLatitude,
            longitude = entity.locationLongitude,
            accuracy = entity.locationAccuracy,
            address = entity.locationAddress,
            timestamp = entity.locationTimestamp ?: entity.createdAt
        )
    }
}
