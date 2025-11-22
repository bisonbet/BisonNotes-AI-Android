package com.bisonnotesai.android.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Recording entity - Main entity for audio recordings
 * Maps to iOS RecordingEntry in Core Data
 *
 * Relationships:
 * - 1:1 with TranscriptEntity (cascade delete)
 * - 1:1 with SummaryEntity (cascade delete)
 * - 1:Many with ProcessingJobEntity (cascade delete)
 */
@Entity(
    tableName = "recordings",
    indices = [
        Index(value = ["recordingDate"], name = "idx_recording_date"),
        Index(value = ["createdAt"], name = "idx_created_at")
    ]
)
data class RecordingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    // Basic Recording Info
    @ColumnInfo(name = "recordingName")
    val recordingName: String? = null,

    @ColumnInfo(name = "recordingDate")
    val recordingDate: Date? = null,

    @ColumnInfo(name = "recordingURL")
    val recordingURL: String? = null,  // Relative path for resilience

    @ColumnInfo(name = "duration")
    val duration: Double = 0.0,  // in seconds

    @ColumnInfo(name = "fileSize")
    val fileSize: Long = 0L,  // in bytes

    @ColumnInfo(name = "audioQuality")
    val audioQuality: String? = null,

    // Location Data (embedded, not separate entity)
    @ColumnInfo(name = "locationLatitude")
    val locationLatitude: Double = 0.0,

    @ColumnInfo(name = "locationLongitude")
    val locationLongitude: Double = 0.0,

    @ColumnInfo(name = "locationAccuracy")
    val locationAccuracy: Double = 0.0,

    @ColumnInfo(name = "locationAddress")
    val locationAddress: String? = null,

    @ColumnInfo(name = "locationTimestamp")
    val locationTimestamp: Date? = null,

    // Processing Status Tracking
    @ColumnInfo(name = "transcriptionStatus")
    val transcriptionStatus: String? = null,  // pending, processing, completed, failed

    @ColumnInfo(name = "summaryStatus")
    val summaryStatus: String? = null,  // pending, processing, completed, failed

    @ColumnInfo(name = "transcriptId")
    val transcriptId: String? = null,  // UUID of associated transcript

    @ColumnInfo(name = "summaryId")
    val summaryId: String? = null,  // UUID of associated summary

    // Timestamps
    @ColumnInfo(name = "createdAt")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "lastModified")
    val lastModified: Date = Date()
)
