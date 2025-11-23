package com.bisonnotesai.android.data.local.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bisonnotesai.android.data.local.database.dao.RecordingDao
import com.bisonnotesai.android.data.local.database.dao.SummaryDao
import com.bisonnotesai.android.data.local.database.dao.TranscriptDao
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

/**
 * Tests for SummaryDao
 */
class SummaryDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: BisonNotesDatabase
    private lateinit var summaryDao: SummaryDao
    private lateinit var recordingDao: RecordingDao
    private lateinit var transcriptDao: TranscriptDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BisonNotesDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        summaryDao = database.summaryDao()
        recordingDao = database.recordingDao()
        transcriptDao = database.transcriptDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveSummary() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val summary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            summary = "This is a test summary of the recording",
            aiMethod = "openai",
            contentType = "meeting",
            confidence = 0.92
        )

        // When
        summaryDao.insert(summary)
        val retrieved = summaryDao.getSummary(summary.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(summary.id, retrieved.id)
        assertEquals(summary.summary, retrieved.summary)
        assertEquals("openai", retrieved.aiMethod)
        assertEquals("meeting", retrieved.contentType)
    }

    @Test
    fun getSummaryForRecording() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val summary1 = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            summary = "First summary",
            version = 1,
            generatedAt = Date(System.currentTimeMillis() - 10000)
        )
        val summary2 = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            summary = "Second summary (regenerated)",
            version = 2,
            generatedAt = Date()
        )

        summaryDao.insert(summary1)
        summaryDao.insert(summary2)

        // When - Should get most recent
        val retrieved = summaryDao.getSummaryForRecording(recordingId)

        // Then
        assertNotNull(retrieved)
        assertEquals(summary2.id, retrieved.id)
        assertEquals("Second summary (regenerated)", retrieved.summary)
    }

    @Test
    fun deleteSummary_whenRecordingDeleted() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val summary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            summary = "Test summary"
        )
        summaryDao.insert(summary)

        // When - Delete recording
        recordingDao.deleteById(recordingId)

        // Then - Summary should be cascade deleted
        assertNull(summaryDao.getSummary(summary.id))
    }

    @Test
    fun preserveSummary_whenTranscriptDeleted() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val transcriptId = UUID.randomUUID().toString()
        val transcript = TranscriptEntity(
            id = transcriptId,
            recordingId = recordingId
        )
        transcriptDao.insert(transcript)

        val summary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            transcriptId = transcriptId,
            summary = "Summary from transcript"
        )
        summaryDao.insert(summary)

        // When - Delete transcript
        transcriptDao.deleteById(transcriptId)

        // Then - Summary should still exist (SET NULL behavior)
        val retrieved = summaryDao.getSummary(summary.id)
        assertNotNull(retrieved)
        assertNull(retrieved.transcriptId) // Should be null now
    }

    @Test
    fun getSummariesWithTasks() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val summaryWithTasks = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            summary = "Summary",
            tasks = """[{"text":"Call John","priority":"high"}]"""
        )
        val summaryWithoutTasks = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            summary = "Summary",
            tasks = null
        )

        summaryDao.insert(summaryWithTasks)
        summaryDao.insert(summaryWithoutTasks)

        // When
        val summaries = summaryDao.getSummariesWithTasks().first()

        // Then
        assertEquals(1, summaries.size)
        assertEquals(summaryWithTasks.id, summaries[0].id)
    }

    @Test
    fun getSummariesByContentType() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val meetingSummary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            contentType = "meeting"
        )
        val lectureSummary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            contentType = "lecture"
        )

        summaryDao.insert(meetingSummary)
        summaryDao.insert(lectureSummary)

        // When
        val meetings = summaryDao.getSummariesByContentType("meeting").first()

        // Then
        assertEquals(1, meetings.size)
        assertEquals(meetingSummary.id, meetings[0].id)
    }

    @Test
    fun getOrphanedSummaries() = runTest {
        // Given - Create a summary with a recordingId that doesn't exist
        // This simulates data corruption or improper deletion
        val nonExistentRecordingId = UUID.randomUUID().toString()

        // First, create a valid recording and summary
        val validRecordingId = UUID.randomUUID().toString()
        val validRecording = RecordingEntity(id = validRecordingId, recordingName = "Valid")
        recordingDao.insert(validRecording)

        val validSummary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = validRecordingId,
            summary = "Valid summary"
        )

        // Note: We can't actually create an orphaned summary due to foreign key constraints
        // This test verifies that the query works correctly when there are no orphans
        summaryDao.insert(validSummary)

        // When
        val orphaned = summaryDao.getOrphanedSummaries()

        // Then - Should be empty because foreign keys prevent orphans
        assertEquals(0, orphaned.size)
    }
}
