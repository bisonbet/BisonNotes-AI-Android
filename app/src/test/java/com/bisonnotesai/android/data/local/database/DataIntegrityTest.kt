package com.bisonnotesai.android.data.local.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bisonnotesai.android.data.local.database.dao.ProcessingJobDao
import com.bisonnotesai.android.data.local.database.dao.RecordingDao
import com.bisonnotesai.android.data.local.database.dao.SummaryDao
import com.bisonnotesai.android.data.local.database.dao.TranscriptDao
import com.bisonnotesai.android.data.local.database.entity.ProcessingJobEntity
import com.bisonnotesai.android.data.local.database.entity.RecordingEntity
import com.bisonnotesai.android.data.local.database.entity.SummaryEntity
import com.bisonnotesai.android.data.local.database.entity.TranscriptEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for data integrity across all entities
 * Tests complete workflows from recording to summary
 */
class DataIntegrityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: BisonNotesDatabase
    private lateinit var recordingDao: RecordingDao
    private lateinit var transcriptDao: TranscriptDao
    private lateinit var summaryDao: SummaryDao
    private lateinit var processingJobDao: ProcessingJobDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BisonNotesDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        recordingDao = database.recordingDao()
        transcriptDao = database.transcriptDao()
        summaryDao = database.summaryDao()
        processingJobDao = database.processingJobDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun completeWorkflow_recordingToSummary() = runTest {
        // STEP 1: Create a recording
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(
            id = recordingId,
            recordingName = "Team Meeting",
            recordingDate = Date(),
            recordingURL = "recordings/meeting_2025.m4a",
            duration = 1800.0, // 30 minutes
            fileSize = 15_000_000, // 15 MB
            locationLatitude = 37.7749,
            locationLongitude = -122.4194,
            locationAddress = "San Francisco, CA",
            transcriptionStatus = "pending",
            summaryStatus = "pending"
        )
        recordingDao.insert(recording)

        // Verify recording exists
        val retrievedRecording = recordingDao.getRecording(recordingId)
        assertNotNull(retrievedRecording)
        assertEquals("Team Meeting", retrievedRecording.recordingName)

        // STEP 2: Create a transcription job
        val transcriptionJobId = UUID.randomUUID().toString()
        val transcriptionJob = ProcessingJobEntity(
            id = transcriptionJobId,
            recordingId = recordingId,
            jobType = "transcription",
            engine = "openai",
            status = "queued",
            recordingName = recording.recordingName,
            recordingURL = recording.recordingURL
        )
        processingJobDao.insert(transcriptionJob)

        // Update job to processing
        processingJobDao.updateStatus(transcriptionJobId, "processing")
        processingJobDao.updateProgress(transcriptionJobId, 50.0)

        // STEP 3: Create transcript
        val transcriptId = UUID.randomUUID().toString()
        val transcript = TranscriptEntity(
            id = transcriptId,
            recordingId = recordingId,
            segments = """[
                {"text":"Welcome to the team meeting","start":0.0,"end":2.5,"speaker":"A"},
                {"text":"Let's discuss the project timeline","start":2.5,"end":5.0,"speaker":"A"},
                {"text":"I think we can finish by next week","start":5.0,"end":7.5,"speaker":"B"}
            ]""",
            speakerMappings = """{"A":"John","B":"Sarah"}""",
            engine = "openai",
            confidence = 0.95,
            processingTime = 45.0,
            createdAt = Date()
        )
        transcriptDao.insert(transcript)

        // Update recording with transcript info
        recordingDao.updateTranscriptionStatus(recordingId, "completed", transcriptId)

        // Mark transcription job as completed
        processingJobDao.markAsCompleted(transcriptionJobId)

        // STEP 4: Create a summarization job
        val summarizationJobId = UUID.randomUUID().toString()
        val summarizationJob = ProcessingJobEntity(
            id = summarizationJobId,
            recordingId = recordingId,
            jobType = "summarization",
            engine = "gpt-4",
            status = "queued",
            recordingName = recording.recordingName,
            recordingURL = recording.recordingURL
        )
        processingJobDao.insert(summarizationJob)

        processingJobDao.updateStatus(summarizationJobId, "processing")

        // STEP 5: Create summary
        val summaryId = UUID.randomUUID().toString()
        val summary = SummaryEntity(
            id = summaryId,
            recordingId = recordingId,
            transcriptId = transcriptId,
            summary = """
                # Team Meeting Summary

                The team discussed the project timeline. Sarah believes the project can be completed by next week.

                ## Key Points
                - Project timeline reviewed
                - Target completion: Next week
            """.trimIndent(),
            titles = """[
                {"text":"Team Meeting - Project Timeline","confidence":0.9},
                {"text":"Weekly Project Update","confidence":0.8},
                {"text":"Project Status Discussion","confidence":0.75}
            ]""",
            tasks = """[
                {"text":"Complete project by next week","priority":"high","assignee":"Team"},
                {"text":"Follow up on timeline","priority":"medium","assignee":"John"}
            ]""",
            reminders = """[
                {"text":"Check project progress","date":"2025-11-29T10:00:00Z","importance":"high"}
            ]""",
            contentType = "meeting",
            aiMethod = "gpt-4",
            confidence = 0.92,
            processingTime = 12.5,
            originalLength = 120,
            wordCount = 45,
            compressionRatio = 0.375,
            version = 1,
            generatedAt = Date()
        )
        summaryDao.insert(summary)

        // Update recording with summary info
        recordingDao.updateSummaryStatus(recordingId, "completed", summaryId)

        // Mark summarization job as completed
        processingJobDao.markAsCompleted(summarizationJobId)

        // STEP 6: Verify complete data structure
        val details = recordingDao.getRecordingWithDetails(recordingId)
        assertNotNull(details)

        // Verify recording
        assertEquals("Team Meeting", details.recording.recordingName)
        assertEquals("completed", details.recording.transcriptionStatus)
        assertEquals("completed", details.recording.summaryStatus)

        // Verify transcript
        assertNotNull(details.transcript)
        assertEquals(transcriptId, details.transcript?.id)
        assertEquals(0.95, details.transcript?.confidence)
        assertEquals("openai", details.transcript?.engine)

        // Verify summary
        assertNotNull(details.summary)
        assertEquals(summaryId, details.summary?.id)
        assertEquals("meeting", details.summary?.contentType)
        assertTrue(details.summary?.summary?.contains("Team Meeting Summary") == true)

        // Verify processing jobs
        assertEquals(2, details.processingJobs.size)
        val completedJobs = details.processingJobs.filter { it.status == "completed" }
        assertEquals(2, completedJobs.size)
    }

    @Test
    fun cascadeDelete_deletesAllRelatedData() = runTest {
        // Given - Complete data structure
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val transcriptId = UUID.randomUUID().toString()
        val transcript = TranscriptEntity(id = transcriptId, recordingId = recordingId)
        transcriptDao.insert(transcript)

        val summaryId = UUID.randomUUID().toString()
        val summary = SummaryEntity(
            id = summaryId,
            recordingId = recordingId,
            transcriptId = transcriptId,
            summary = "Test"
        )
        summaryDao.insert(summary)

        val jobId = UUID.randomUUID().toString()
        val job = ProcessingJobEntity(
            id = jobId,
            recordingId = recordingId,
            jobType = "transcription",
            engine = "openai",
            status = "completed"
        )
        processingJobDao.insert(job)

        // Verify all exist
        assertNotNull(recordingDao.getRecording(recordingId))
        assertNotNull(transcriptDao.getTranscript(transcriptId))
        assertNotNull(summaryDao.getSummary(summaryId))
        assertNotNull(processingJobDao.getProcessingJob(jobId))

        // When - Delete recording
        recordingDao.deleteById(recordingId)

        // Then - All related data should be deleted
        assertNull(recordingDao.getRecording(recordingId))
        assertNull(transcriptDao.getTranscript(transcriptId))
        assertNull(summaryDao.getSummary(summaryId))

        // Job should still exist but recordingId should be null (SET NULL)
        val remainingJob = processingJobDao.getProcessingJob(jobId)
        assertNotNull(remainingJob)
        assertNull(remainingJob.recordingId)
    }

    @Test
    fun multipleRecordings_maintainSeparateData() = runTest {
        // Create two separate recordings with full data
        val recording1Id = UUID.randomUUID().toString()
        val recording1 = RecordingEntity(id = recording1Id, recordingName = "Meeting 1")
        recordingDao.insert(recording1)

        val transcript1 = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recording1Id,
            engine = "openai"
        )
        transcriptDao.insert(transcript1)

        val summary1 = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recording1Id,
            summary = "Summary 1"
        )
        summaryDao.insert(summary1)

        val recording2Id = UUID.randomUUID().toString()
        val recording2 = RecordingEntity(id = recording2Id, recordingName = "Meeting 2")
        recordingDao.insert(recording2)

        val transcript2 = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recording2Id,
            engine = "aws"
        )
        transcriptDao.insert(transcript2)

        val summary2 = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recording2Id,
            summary = "Summary 2"
        )
        summaryDao.insert(summary2)

        // Verify separate data structures
        val details1 = recordingDao.getRecordingWithDetails(recording1Id)
        assertNotNull(details1)
        assertEquals("Meeting 1", details1.recording.recordingName)
        assertEquals("openai", details1.transcript?.engine)
        assertEquals("Summary 1", details1.summary?.summary)

        val details2 = recordingDao.getRecordingWithDetails(recording2Id)
        assertNotNull(details2)
        assertEquals("Meeting 2", details2.recording.recordingName)
        assertEquals("aws", details2.transcript?.engine)
        assertEquals("Summary 2", details2.summary?.summary)

        // Delete one recording shouldn't affect the other
        recordingDao.deleteById(recording1Id)

        assertNull(recordingDao.getRecording(recording1Id))
        assertNotNull(recordingDao.getRecording(recording2Id))
    }

    @Test
    fun databaseCounts_trackCorrectly() = runTest {
        // Initially empty
        assertEquals(0, recordingDao.getRecordingCount())
        assertEquals(0, transcriptDao.getTranscriptCount())
        assertEquals(0, summaryDao.getSummaryCount())
        assertEquals(0, processingJobDao.getJobCount())

        // Add data
        val recordingId = UUID.randomUUID().toString()
        recordingDao.insert(RecordingEntity(id = recordingId, recordingName = "Test"))
        transcriptDao.insert(TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId
        ))
        summaryDao.insert(SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId
        ))
        processingJobDao.insert(ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            jobType = "test",
            engine = "test",
            status = "queued"
        ))

        // Verify counts
        assertEquals(1, recordingDao.getRecordingCount())
        assertEquals(1, transcriptDao.getTranscriptCount())
        assertEquals(1, summaryDao.getSummaryCount())
        assertEquals(1, processingJobDao.getJobCount())

        // Delete recording (cascades to transcript and summary, but not job)
        recordingDao.deleteById(recordingId)

        assertEquals(0, recordingDao.getRecordingCount())
        assertEquals(0, transcriptDao.getTranscriptCount())
        assertEquals(0, summaryDao.getSummaryCount())
        assertEquals(1, processingJobDao.getJobCount()) // Job preserved
    }

    @Test
    fun flowUpdates_reactToDataChanges() = runTest {
        // Collect initial state
        val initialRecordings = recordingDao.getAllRecordings().first()
        assertEquals(0, initialRecordings.size)

        // Add recording
        val recordingId = UUID.randomUUID().toString()
        recordingDao.insert(RecordingEntity(id = recordingId, recordingName = "Test"))

        // Verify flow updated
        val updatedRecordings = recordingDao.getAllRecordings().first()
        assertEquals(1, updatedRecordings.size)
        assertEquals("Test", updatedRecordings[0].recordingName)

        // Update recording
        recordingDao.updateRecordingName(recordingId, "Updated Name")

        // Verify flow updated again
        val finalRecordings = recordingDao.getAllRecordings().first()
        assertEquals(1, finalRecordings.size)
        assertEquals("Updated Name", finalRecordings[0].recordingName)
    }
}
