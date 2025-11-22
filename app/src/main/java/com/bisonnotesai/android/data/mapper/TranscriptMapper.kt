package com.bisonnotesai.android.data.mapper

import com.bisonnotesai.android.data.local.database.entity.TranscriptEntity
import com.bisonnotesai.android.domain.model.Transcript
import com.bisonnotesai.android.domain.model.TranscriptSegment
import com.bisonnotesai.android.domain.model.TranscriptionEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

/**
 * Mapper between TranscriptEntity (data layer) and Transcript (domain layer)
 */
class TranscriptMapper @Inject constructor(
    private val gson: Gson
) {

    /**
     * Convert TranscriptEntity to Transcript domain model
     */
    fun toDomain(entity: TranscriptEntity): Transcript {
        return Transcript(
            id = entity.id,
            recordingId = entity.recordingId,
            segments = parseSegments(entity.segments),
            speakerMappings = parseSpeakerMappings(entity.speakerMappings),
            engine = TranscriptionEngine.fromString(entity.engine),
            confidence = entity.confidence,
            processingTime = entity.processingTime,
            createdAt = entity.createdAt,
            lastModified = entity.lastModified
        )
    }

    /**
     * Convert Transcript domain model to TranscriptEntity
     */
    fun toEntity(domain: Transcript): TranscriptEntity {
        return TranscriptEntity(
            id = domain.id,
            recordingId = domain.recordingId,
            segments = serializeSegments(domain.segments),
            speakerMappings = serializeSpeakerMappings(domain.speakerMappings),
            engine = domain.engine.name.lowercase(),
            confidence = domain.confidence,
            processingTime = domain.processingTime,
            createdAt = domain.createdAt,
            lastModified = domain.lastModified
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<TranscriptEntity>): List<Transcript> {
        return entities.map { toDomain(it) }
    }

    /**
     * Parse JSON segments string to list of TranscriptSegment
     */
    private fun parseSegments(json: String?): List<TranscriptSegment> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<SegmentJson>>() {}.type
            val segments: List<SegmentJson> = gson.fromJson(json, type)
            segments.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Serialize segments to JSON string
     */
    private fun serializeSegments(segments: List<TranscriptSegment>): String {
        val segmentJsonList = segments.map { SegmentJson.fromDomain(it) }
        return gson.toJson(segmentJsonList)
    }

    /**
     * Parse JSON speaker mappings to Map
     */
    private fun parseSpeakerMappings(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()

        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Serialize speaker mappings to JSON string
     */
    private fun serializeSpeakerMappings(mappings: Map<String, String>): String {
        return gson.toJson(mappings)
    }

    /**
     * JSON representation of TranscriptSegment for serialization
     */
    private data class SegmentJson(
        val text: String,
        val start: Double,
        val end: Double,
        val speaker: String? = null,
        val confidence: Double? = null
    ) {
        fun toDomain() = TranscriptSegment(
            text = text,
            start = start,
            end = end,
            speaker = speaker,
            confidence = confidence
        )

        companion object {
            fun fromDomain(segment: TranscriptSegment) = SegmentJson(
                text = segment.text,
                start = segment.start,
                end = segment.end,
                speaker = segment.speaker,
                confidence = segment.confidence
            )
        }
    }
}
