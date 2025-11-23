package com.bisonnotesai.android.data.local.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bisonnotesai.android.data.local.database.dao.RecordingDao
import com.bisonnotesai.android.data.local.database.dao.TranscriptDao
import com.bisonnotesai.android.data.local.database.entity.RecordingEntity
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
 * Tests for TranscriptDao
 */
class TranscriptDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: BisonNotesDatabase
    private lateinit var transcriptDao: TranscriptDao
    private lateinit var recordingDao: RecordingDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BisonNotesDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        transcriptDao = database.transcriptDao()
        recordingDao = database.recordingDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTranscript() = runTest {
        // Given - Create a recording first
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(
            id = recordingId,
            recordingName = "Test Recording"
        )
        recordingDao.insert(recording)

        val transcript = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            engine = "openai",
            confidence = 0.95,
            segments = """[{"text":"Hello world","start":0.0,"end":1.5}]"""
        )

        // When
        transcriptDao.insert(transcript)
        val retrieved = transcriptDao.getTranscript(transcript.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(transcript.id, retrieved.id)
        assertEquals(transcript.recordingId, retrieved.recordingId)
        assertEquals(0.95, retrieved.confidence)
        assertEquals("openai", retrieved.engine)
    }

    @Test
    fun getTranscriptForRecording() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val transcript1 = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            engine = "openai",
            createdAt = Date(System.currentTimeMillis() - 10000)
        )
        val transcript2 = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            engine = "aws",
            createdAt = Date()
        )

        transcriptDao.insert(transcript1)
        transcriptDao.insert(transcript2)

        // When - Should get most recent
        val retrieved = transcriptDao.getTranscriptForRecording(recordingId)

        // Then
        assertNotNull(retrieved)
        assertEquals(transcript2.id, retrieved.id) // Most recent one
    }

    @Test
    fun deleteTranscript_whenRecordingDeleted() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val transcript = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            engine = "openai"
        )
        transcriptDao.insert(transcript)

        // When - Delete recording
        recordingDao.deleteById(recordingId)

        // Then - Transcript should be cascade deleted
        val retrieved = transcriptDao.getTranscript(transcript.id)
        assertNull(retrieved)
    }

    @Test
    fun getAllTranscripts_orderedByDate() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val transcript1 = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            createdAt = Date(System.currentTimeMillis() - 1000000)
        )
        val transcript2 = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            createdAt = Date()
        )

        transcriptDao.insert(transcript1)
        transcriptDao.insert(transcript2)

        // When
        val transcripts = transcriptDao.getAllTranscripts().first()

        // Then - Should be ordered by createdAt DESC
        assertEquals(2, transcripts.size)
        assertEquals(transcript2.id, transcripts[0].id) // Newest first
        assertEquals(transcript1.id, transcripts[1].id)
    }

    @Test
    fun getTranscriptsByEngine() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val openAITranscript = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            engine = "openai"
        )
        val awsTranscript = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            engine = "aws"
        )

        transcriptDao.insert(openAITranscript)
        transcriptDao.insert(awsTranscript)

        // When
        val openAITranscripts = transcriptDao.getTranscriptsByEngine("openai").first()

        // Then
        assertEquals(1, openAITranscripts.size)
        assertEquals(openAITranscript.id, openAITranscripts[0].id)
    }

    @Test
    fun getHighConfidenceTranscripts() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val highConfidence = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            confidence = 0.95
        )
        val lowConfidence = TranscriptEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            confidence = 0.65
        )

        transcriptDao.insert(highConfidence)
        transcriptDao.insert(lowConfidence)

        // When
        val transcripts = transcriptDao.getHighConfidenceTranscripts(0.8).first()

        // Then
        assertEquals(1, transcripts.size)
        assertEquals(highConfidence.id, transcripts[0].id)
    }
}
