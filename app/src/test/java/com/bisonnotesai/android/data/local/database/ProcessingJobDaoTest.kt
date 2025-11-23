package com.bisonnotesai.android.data.local.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bisonnotesai.android.data.local.database.dao.ProcessingJobDao
import com.bisonnotesai.android.data.local.database.dao.RecordingDao
import com.bisonnotesai.android.data.local.database.entity.ProcessingJobEntity
import com.bisonnotesai.android.data.local.database.entity.RecordingEntity
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
 * Tests for ProcessingJobDao
 */
class ProcessingJobDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: BisonNotesDatabase
    private lateinit var processingJobDao: ProcessingJobDao
    private lateinit var recordingDao: RecordingDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BisonNotesDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        processingJobDao = database.processingJobDao()
        recordingDao = database.recordingDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveProcessingJob() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val job = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            jobType = "transcription",
            engine = "openai",
            status = "queued",
            progress = 0.0
        )

        // When
        processingJobDao.insert(job)
        val retrieved = processingJobDao.getProcessingJob(job.id)

        // Then
        assertNotNull(retrieved)
        assertEquals(job.id, retrieved.id)
        assertEquals("transcription", retrieved.jobType)
        assertEquals("openai", retrieved.engine)
        assertEquals("queued", retrieved.status)
    }

    @Test
    fun getActiveProcessingJobs() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val queuedJob = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            jobType = "transcription",
            engine = "openai",
            status = "queued"
        )
        val processingJob = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            jobType = "summarization",
            engine = "gpt-4",
            status = "processing"
        )
        val completedJob = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            jobType = "transcription",
            engine = "aws",
            status = "completed"
        )

        processingJobDao.insert(queuedJob)
        processingJobDao.insert(processingJob)
        processingJobDao.insert(completedJob)

        // When
        val activeJobs = processingJobDao.getActiveProcessingJobs().first()

        // Then
        assertEquals(2, activeJobs.size)
        val statuses = activeJobs.map { it.status }
        assert("queued" in statuses)
        assert("processing" in statuses)
        assert("completed" !in statuses)
    }

    @Test
    fun updateJobStatus() = runTest {
        // Given
        val job = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "openai",
            status = "queued"
        )
        processingJobDao.insert(job)

        // When
        processingJobDao.updateStatus(job.id, "processing")

        // Then
        val updated = processingJobDao.getProcessingJob(job.id)
        assertNotNull(updated)
        assertEquals("processing", updated.status)
    }

    @Test
    fun updateJobProgress() = runTest {
        // Given
        val job = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "openai",
            status = "processing",
            progress = 0.0
        )
        processingJobDao.insert(job)

        // When
        processingJobDao.updateProgress(job.id, 50.0)

        // Then
        val updated = processingJobDao.getProcessingJob(job.id)
        assertNotNull(updated)
        assertEquals(50.0, updated.progress)
    }

    @Test
    fun markJobAsCompleted() = runTest {
        // Given
        val job = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "openai",
            status = "processing"
        )
        processingJobDao.insert(job)

        // When
        processingJobDao.markAsCompleted(job.id)

        // Then
        val updated = processingJobDao.getProcessingJob(job.id)
        assertNotNull(updated)
        assertEquals("completed", updated.status)
        assertEquals(100.0, updated.progress)
        assertNotNull(updated.completionTime)
    }

    @Test
    fun markJobAsFailed() = runTest {
        // Given
        val job = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "openai",
            status = "processing"
        )
        processingJobDao.insert(job)

        // When
        val errorMessage = "Network timeout"
        processingJobDao.markAsFailed(job.id, errorMessage)

        // Then
        val updated = processingJobDao.getProcessingJob(job.id)
        assertNotNull(updated)
        assertEquals("failed", updated.status)
        assertEquals(errorMessage, updated.error)
        assertNotNull(updated.completionTime)
    }

    @Test
    fun deleteCompletedJobs() = runTest {
        // Given
        val completedJob = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "openai",
            status = "completed"
        )
        val failedJob = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "summarization",
            engine = "gpt-4",
            status = "failed"
        )
        val activeJob = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "aws",
            status = "processing"
        )

        processingJobDao.insert(completedJob)
        processingJobDao.insert(failedJob)
        processingJobDao.insert(activeJob)

        // When
        val deletedCount = processingJobDao.deleteCompletedJobs()

        // Then
        assertEquals(2, deletedCount) // completed + failed
        assertNull(processingJobDao.getProcessingJob(completedJob.id))
        assertNull(processingJobDao.getProcessingJob(failedJob.id))
        assertNotNull(processingJobDao.getProcessingJob(activeJob.id))
    }

    @Test
    fun preserveJobHistory_whenRecordingDeleted() = runTest {
        // Given
        val recordingId = UUID.randomUUID().toString()
        val recording = RecordingEntity(id = recordingId, recordingName = "Test")
        recordingDao.insert(recording)

        val job = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            recordingId = recordingId,
            jobType = "transcription",
            engine = "openai",
            status = "completed",
            recordingName = "Test", // Denormalized
            recordingURL = "test.m4a" // Denormalized
        )
        processingJobDao.insert(job)

        // When - Delete recording
        recordingDao.deleteById(recordingId)

        // Then - Job should still exist (SET NULL behavior)
        val retrieved = processingJobDao.getProcessingJob(job.id)
        assertNotNull(retrieved)
        assertNull(retrieved.recordingId) // Should be null now
        // But denormalized data is preserved
        assertEquals("Test", retrieved.recordingName)
        assertEquals("test.m4a", retrieved.recordingURL)
    }

    @Test
    fun getJobsByType() = runTest {
        // Given
        val transcriptionJob = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "openai",
            status = "completed"
        )
        val summarizationJob = ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "summarization",
            engine = "gpt-4",
            status = "completed"
        )

        processingJobDao.insert(transcriptionJob)
        processingJobDao.insert(summarizationJob)

        // When
        val transcriptionJobs = processingJobDao.getJobsByType("transcription").first()

        // Then
        assertEquals(1, transcriptionJobs.size)
        assertEquals(transcriptionJob.id, transcriptionJobs[0].id)
    }

    @Test
    fun getActiveJobCount() = runTest {
        // Given
        processingJobDao.insert(ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "openai",
            status = "queued"
        ))
        processingJobDao.insert(ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "summarization",
            engine = "gpt-4",
            status = "processing"
        ))
        processingJobDao.insert(ProcessingJobEntity(
            id = UUID.randomUUID().toString(),
            jobType = "transcription",
            engine = "aws",
            status = "completed"
        ))

        // When
        val count = processingJobDao.getActiveJobCount()

        // Then
        assertEquals(2, count) // queued + processing
    }
}
