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
 * Unit tests for RecordingDao
 * Tests CRUD operations, relationships, and data integrity
 */
class RecordingDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: BisonNotesDatabase
    private lateinit var recordingDao: RecordingDao
    private lateinit var transcriptDao: TranscriptDao
    private lateinit var summaryDao: SummaryDao

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
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveRecording() = runTest {
        // Given
        val recording = RecordingEntity(
            id = UUID.randomUUID().toString(),
            recordingName = "Test Recording",
            recordingDate = Date(),
            duration = 120.0,
            fileSize = 1024L
        )

        // When
        recordingDao.insert(recording)
        val retrieved = recordingDao.getRecording(recording.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(recording.id, retrieved.id)
        assertEquals(recording.recordingName, retrieved.recordingName)
        assertEquals(recording.duration, retrieved.duration)
    }

    @Test
    fun deleteRecording_cascadesTranscriptAndSummary() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(
            id = recordingId,
            recordingName = "Test Recording"
        )

        val transcript = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            engine = "openai"
        )

        val summary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            summary = "Test summary"
        )

        // When - Insert all
        recordingDao.insert(recording)
        transcriptDao.insert(transcript)
        summaryDao.insert(summary)

        // Verify they exist
        assertNotNull(recordingDao.getRecording(recordingId))
        assertNotNull(transcriptDao.getTranscript(transcript.id))
        assertNotNull(summaryDao.getSummary(summary.id))

        // When - Delete recording
        recordingDao.deleteById(recordingId)

        // Then - Transcript and summary should be cascade deleted
        assertNull(recordingDao.getRecording(recordingId))
        assertNull(transcriptDao.getTranscript(transcript.id))
        assertNull(summaryDao.getSummary(summary.id))
    }

    @Test
    fun getRecordingWithDetails_includesTranscriptAndSummary() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(
            id = recordingId,
            recordingName = "Test Recording"
        )

        val transcript = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            engine = "openai",
            confidence = 0.95
        )

        val summary = SummaryEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            summary = "Test summary",
            aiMethod = "gpt-4"
        )

        // When
        recordingDao.insert(recording)
        transcriptDao.insert(transcript)
        summaryDao.insert(summary)

        val details = recordingDao.getRecordingWithDetails(recordingId)

        // Then
        assertNotNull(details)
        assertEquals(recordingId, details.recording.id)
        assertNotNull(details.transcript)
        assertEquals(transcript.id, details.transcript?.id)
        assertEquals(0.95, details.transcript?.confidence)
        assertNotNull(details.summary)
        assertEquals(summary.id, details.summary?.id)
        assertEquals("Test summary", details.summary?.summary)
    }

    @Test
    fun getAllRecordings_orderedByDate() = runTest {
        // Given
        val recording1 = RecordingEntity(
            id = UUID.randomUUID().toString(),
            recordingName = "First",
            recordingDate = Date(System.currentTimeMillis() - 1000000)
        )
        val recording2 = RecordingEntity(
            id = UUID.randomUUID().toString(),
            recordingName = "Second",
            recordingDate = Date()
        )

        // When
        recordingDao.insert(recording1)
        recordingDao.insert(recording2)

        val recordings = recordingDao.getAllRecordings().first()

        // Then
        assertEquals(2, recordings.size)
        // Should be ordered by recordingDate DESC (newest first)
        assertEquals(recording2.id, recordings[0].id)
        assertEquals(recording1.id, recordings[1].id)
    }

    @Test
    fun updateRecordingName() = runTest {
        // Given
        val recording = RecordingEntity(
            id = UUID.randomUUID().toString(),
            recordingName = "Original Name"
        )

        recordingDao.insert(recording)

        // When
        val newName = "Updated Name"
        recordingDao.updateRecordingName(recording.id, newName)

        val updated = recordingDao.getRecording(recording.id)

        // Then
        assertNotNull(updated)
        assertEquals(newName, updated.recordingName)
    }

    @Test
    fun cleanupOrphanedRecordings_removesRecordingsWithNoContent() = runTest {
        // Given - Recording with content
        val recordingWithTranscript = RecordingEntity(
            id = UUID.randomUUID().toString(),
            recordingName = "Has Transcript",
            recordingURL = null
        )
        val transcript = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingWithTranscript.id
        )

        // Recording without content (orphaned)
        val orphanedRecording = RecordingEntity(
            id = UUID.randomUUID().toString(),
            recordingName = "Orphaned",
            recordingURL = null
        )

        // When
        recordingDao.insert(recordingWithTranscript)
        transcriptDao.insert(transcript)
        recordingDao.insert(orphanedRecording)

        val cleanedCount = recordingDao.cleanupOrphanedRecordings()

        // Then
        assertEquals(1, cleanedCount)
        assertNotNull(recordingDao.getRecording(recordingWithTranscript.id))
        assertNull(recordingDao.getRecording(orphanedRecording.id))
    }
}
