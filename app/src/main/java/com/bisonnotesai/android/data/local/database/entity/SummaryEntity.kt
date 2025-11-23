package com.bisonnotesai.android.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Summary entity - Stores AI-generated summaries
 * Maps to iOS SummaryEntry in Core Data
 *
 * Relationships:
 * - Many:1 with RecordingEntity (foreign key with CASCADE delete)
 * - Many:1 with TranscriptEntity (foreign key with SET NULL delete)
 */
@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE,  // Delete summary when recording is deleted
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TranscriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptId"],
            onDelete = ForeignKey.SET_NULL,  // Keep summary if transcript is deleted
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recordingId"], name = "idx_summary_recording_id"),
        Index(value = ["transcriptId"], name = "idx_summary_transcript_id"),
        Index(value = ["generatedAt"], name = "idx_summary_generated_at")
    ]
)
data class SummaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    // Foreign Keys
    @ColumnInfo(name = "recordingId")
    val recordingId: String,

    @ColumnInfo(name = "transcriptId")
    val transcriptId: String? = null,

    // Summary Content
    @ColumnInfo(name = "summary", typeAffinity = ColumnInfo.TEXT)
    val summary: String? = null,

    @ColumnInfo(name = "titles", typeAffinity = ColumnInfo.TEXT)
    val titles: String? = null,  // JSON array of TitleItem objects

    @ColumnInfo(name = "tasks", typeAffinity = ColumnInfo.TEXT)
    val tasks: String? = null,  // JSON array of TaskItem objects

    @ColumnInfo(name = "reminders", typeAffinity = ColumnInfo.TEXT)
    val reminders: String? = null,  // JSON array of ReminderItem objects

    // Summary Metadata
    @ColumnInfo(name = "contentType")
    val contentType: String? = null,  // meeting, lecture, interview, general, etc.

    @ColumnInfo(name = "aiMethod")
    val aiMethod: String? = null,  // openai, claude, gemini, ollama, etc.

    @ColumnInfo(name = "confidence")
    val confidence: Double = 0.0,  // 0.0 to 1.0

    @ColumnInfo(name = "processingTime")
    val processingTime: Double = 0.0,  // in seconds

    // Statistics
    @ColumnInfo(name = "originalLength")
    val originalLength: Int = 0,  // character count of original transcript

    @ColumnInfo(name = "wordCount")
    val wordCount: Int = 0,  // word count in summary

    @ColumnInfo(name = "compressionRatio")
    val compressionRatio: Double = 0.0,  // ratio of summary to original

    @ColumnInfo(name = "version")
    val version: Int = 1,  // version of summary (for regeneration)

    // Timestamps
    @ColumnInfo(name = "generatedAt")
    val generatedAt: Date = Date()
)
