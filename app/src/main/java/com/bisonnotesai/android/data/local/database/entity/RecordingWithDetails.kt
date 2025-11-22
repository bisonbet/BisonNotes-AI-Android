package com.bisonnotesai.android.data.local.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class for retrieving a recording with all its related data
 * Matches iOS getRecordingWithTranscriptAndSummary functionality
 */
data class RecordingWithDetails(
    @Embedded
    val recording: RecordingEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "recordingId"
    )
    val transcript: TranscriptEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "recordingId"
    )
    val summary: SummaryEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "recordingId"
    )
    val processingJobs: List<ProcessingJobEntity> = emptyList()
)
