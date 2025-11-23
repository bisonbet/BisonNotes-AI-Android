package com.bisonnotesai.android

import com.bisonnotesai.android.data.local.database.entity.ProcessingJobEntity
import com.bisonnotesai.android.data.local.database.entity.RecordingEntity
import com.bisonnotesai.android.data.local.database.entity.SummaryEntity
import com.bisonnotesai.android.data.local.database.entity.TranscriptEntity
import java.util.Date
import java.util.UUID

/**
 * Test utilities and factory methods for creating test data
 */
object TestUtils {

    /**
     * Create a test RecordingEntity with optional parameters
     */
    fun createTestRecording(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Recording",
        url: String = "test_${UUID.randomUUID()}.m4a",
        duration: Double = 120.0,
        fileSize: Long = 1024L,
        date: Date = Date()
    ): RecordingEntity {
        return RecordingEntity(
            id = id,
            recordingName = name,
            recordingURL = url,
            duration = duration,
            fileSize = fileSize,
            recordingDate = date,
            createdAt = date
        )
    }

    /**
     * Create a test TranscriptEntity with optional parameters
     */
    fun createTestTranscript(
        id: String = UUID.randomUUID().toString(),
        recordingId: String,
        engine: String = "openai",
        confidence: Double = 0.95,
        segments: String = """[{"text":"Test transcript","start":0.0,"end":1.0}]"""
    ): TranscriptEntity {
        return TranscriptEntity(
            id = id,
            recordingId = recordingId,
            engine = engine,
            confidence = confidence,
            segments = segments,
            createdAt = Date()
        )
    }

    /**
     * Create a test SummaryEntity with optional parameters
     */
    fun createTestSummary(
        id: String = UUID.randomUUID().toString(),
        recordingId: String,
        transcriptId: String? = null,
        summary: String = "Test summary",
        aiMethod: String = "openai",
        contentType: String = "general"
    ): SummaryEntity {
        return SummaryEntity(
            id = id,
            recordingId = recordingId,
            transcriptId = transcriptId,
            summary = summary,
            aiMethod = aiMethod,
            contentType = contentType,
            generatedAt = Date()
        )
    }

    /**
     * Create a test ProcessingJobEntity with optional parameters
     */
    fun createTestJob(
        id: String = UUID.randomUUID().toString(),
        recordingId: String? = null,
        jobType: String = "transcription",
        engine: String = "openai",
        status: String = "queued",
        progress: Double = 0.0
    ): ProcessingJobEntity {
        return ProcessingJobEntity(
            id = id,
            recordingId = recordingId,
            jobType = jobType,
            engine = engine,
            status = status,
            progress = progress,
            startTime = Date()
        )
    }

    /**
     * Create a complete recording with transcript and summary
     */
    data class CompleteRecordingData(
        val recording: RecordingEntity,
        val transcript: TranscriptEntity,
        val summary: SummaryEntity
    )

    fun createCompleteRecording(
        recordingId: String = UUID.randomUUID().toString(),
        recordingName: String = "Test Recording"
    ): CompleteRecordingData {
        val transcriptId = UUID.randomUUID().toString()

        return CompleteRecordingData(
            recording = createTestRecording(
                id = recordingId,
                name = recordingName
            ),
            transcript = createTestTranscript(
                id = transcriptId,
                recordingId = recordingId
            ),
            summary = createTestSummary(
                recordingId = recordingId,
                transcriptId = transcriptId
            )
        )
    }
}
