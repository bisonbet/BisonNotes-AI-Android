package com.bisonnotesai.android.data.repository

import com.bisonnotesai.android.data.local.database.dao.RecordingDao
import com.bisonnotesai.android.data.mapper.RecordingMapper
import com.bisonnotesai.android.domain.model.Recording
import com.bisonnotesai.android.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of RecordingRepository
 * Bridges domain layer with data layer using DAOs and mappers
 */
class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    private val mapper: RecordingMapper
) : RecordingRepository {

    override fun getAllRecordings(): Flow<List<Recording>> {
        return recordingDao.getAllRecordings()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun getRecording(id: String): Recording? {
        return recordingDao.getRecording(id)?.let { mapper.toDomain(it) }
    }

    override fun getRecordingFlow(id: String): Flow<Recording?> {
        return recordingDao.getRecordingFlow(id)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }

    override fun getRecordingsWithTranscripts(): Flow<List<Recording>> {
        return recordingDao.getRecordingsWithTranscripts()
            .map { detailsList ->
                detailsList.map { mapper.toDomain(it.recording) }
            }
    }

    override fun getRecordingsWithSummaries(): Flow<List<Recording>> {
        return recordingDao.getRecordingsWithSummaries()
            .map { detailsList ->
                detailsList.map { mapper.toDomain(it.recording) }
            }
    }

    override suspend fun saveRecording(recording: Recording): Result<String> {
        return try {
            val entity = mapper.toEntity(recording)
            recordingDao.insert(entity)
            Result.success(recording.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecording(recording: Recording): Result<Unit> {
        return try {
            val entity = mapper.toEntity(recording)
            recordingDao.update(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecording(id: String): Result<Unit> {
        return try {
            recordingDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecordingName(id: String, newName: String): Result<Unit> {
        return try {
            recordingDao.updateRecordingName(id, newName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTranscriptionStatus(
        id: String,
        status: String,
        transcriptId: String?
    ): Result<Unit> {
        return try {
            recordingDao.updateTranscriptionStatus(id, status, transcriptId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSummaryStatus(
        id: String,
        status: String,
        summaryId: String?
    ): Result<Unit> {
        return try {
            recordingDao.updateSummaryStatus(id, status, summaryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecordingCount(): Int {
        return recordingDao.getRecordingCount()
    }

    override suspend fun cleanupOrphanedRecordings(): Int {
        return recordingDao.cleanupOrphanedRecordings()
    }
}
