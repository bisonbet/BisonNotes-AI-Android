package com.bisonnotesai.android.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Transcript entity - Stores transcription data
 * Maps to iOS TranscriptEntry in Core Data
 *
 * Relationships:
 * - Many:1 with RecordingEntity (foreign key with CASCADE delete)
 * - 1:Many with SummaryEntity (via transcriptId in SummaryEntity)
 */
@Entity(
    tableName = "transcripts",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE,  // Delete transcript when recording is deleted
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recordingId"], name = "idx_transcript_recording_id"),
        Index(value = ["createdAt"], name = "idx_transcript_created_at")
    ]
)
data class TranscriptEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    // Foreign Key to Recording
    @ColumnInfo(name = "recordingId")
    val recordingId: String,

    // Transcript Content (stored as JSON string)
    @ColumnInfo(name = "segments", typeAffinity = ColumnInfo.TEXT)
    val segments: String? = null,  // JSON array of TranscriptSegment objects

    @ColumnInfo(name = "speakerMappings", typeAffinity = ColumnInfo.TEXT)
    val speakerMappings: String? = null,  // JSON object mapping speaker IDs to names

    // Transcription Metadata
    @ColumnInfo(name = "engine")
    val engine: String? = null,  // openai, aws, local, etc.

    @ColumnInfo(name = "confidence")
    val confidence: Double = 0.0,  // 0.0 to 1.0

    @ColumnInfo(name = "processingTime")
    val processingTime: Double = 0.0,  // in seconds

    // Timestamps
    @ColumnInfo(name = "createdAt")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "lastModified")
    val lastModified: Date = Date()
)
