package com.bisonnotesai.android.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Processing Job entity - Tracks background processing tasks
 * Maps to iOS ProcessingJobEntry in Core Data
 *
 * Relationships:
 * - Many:1 with RecordingEntity (foreign key with SET NULL delete)
 */
@Entity(
    tableName = "processing_jobs",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.SET_NULL,  // Keep job history even if recording is deleted
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recordingId"], name = "idx_job_recording_id"),
        Index(value = ["status"], name = "idx_job_status"),
        Index(value = ["startTime"], name = "idx_job_start_time")
    ]
)
data class ProcessingJobEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    // Foreign Key to Recording (nullable)
    @ColumnInfo(name = "recordingId")
    val recordingId: String? = null,

    // Job Details
    @ColumnInfo(name = "jobType")
    val jobType: String,  // transcription, summarization, etc.

    @ColumnInfo(name = "engine")
    val engine: String,  // openai, aws, local, etc.

    @ColumnInfo(name = "status")
    val status: String,  // queued, processing, completed, failed

    @ColumnInfo(name = "progress")
    val progress: Double = 0.0,  // 0.0 to 100.0

    // Recording Info (denormalized for job history)
    @ColumnInfo(name = "recordingName")
    val recordingName: String? = null,

    @ColumnInfo(name = "recordingURL")
    val recordingURL: String? = null,

    // Error Handling
    @ColumnInfo(name = "error", typeAffinity = ColumnInfo.TEXT)
    val error: String? = null,  // Error message if failed

    // Timestamps
    @ColumnInfo(name = "startTime")
    val startTime: Date = Date(),

    @ColumnInfo(name = "completionTime")
    val completionTime: Date? = null,

    @ColumnInfo(name = "lastModified")
    val lastModified: Date = Date()
)
