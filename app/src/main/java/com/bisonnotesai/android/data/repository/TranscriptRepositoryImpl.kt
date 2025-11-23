package com.bisonnotesai.android.data.repository

import com.bisonnotesai.android.data.local.database.dao.TranscriptDao
import com.bisonnotesai.android.data.mapper.TranscriptMapper
import com.bisonnotesai.android.domain.model.Transcript
import com.bisonnotesai.android.domain.model.TranscriptionEngine
import com.bisonnotesai.android.domain.repository.TranscriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of TranscriptRepository
 * Bridges domain layer with data layer using DAOs and mappers
 */
class TranscriptRepositoryImpl @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val mapper: TranscriptMapper
) : TranscriptRepository {

    override fun getAllTranscripts(): Flow<List<Transcript>> {
        return transcriptDao.getAllTranscripts()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun getTranscript(id: String): Transcript? {
        return transcriptDao.getTranscript(id)?.let { mapper.toDomain(it) }
    }

    override fun getTranscriptFlow(id: String): Flow<Transcript?> {
        return transcriptDao.getTranscriptFlow(id)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }

    override suspend fun getTranscriptForRecording(recordingId: String): Transcript? {
        return transcriptDao.getTranscriptForRecording(recordingId)?.let { mapper.toDomain(it) }
    }

    override fun getTranscriptForRecordingFlow(recordingId: String): Flow<Transcript?> {
        return transcriptDao.getTranscriptForRecordingFlow(recordingId)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }

    override fun getAllTranscriptsForRecording(recordingId: String): Flow<List<Transcript>> {
        return transcriptDao.getAllTranscriptsForRecording(recordingId)
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getTranscriptsByEngine(engine: TranscriptionEngine): Flow<List<Transcript>> {
        return transcriptDao.getTranscriptsByEngine(engine.name.lowercase())
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun saveTranscript(transcript: Transcript): Result<String> {
        return try {
            val entity = mapper.toEntity(transcript)
            transcriptDao.insert(entity)
            Result.success(transcript.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTranscript(transcript: Transcript): Result<Unit> {
        return try {
            val entity = mapper.toEntity(transcript)
            transcriptDao.update(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTranscript(id: String): Result<Unit> {
        return try {
            transcriptDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTranscriptsForRecording(recordingId: String): Result<Unit> {
        return try {
            transcriptDao.deleteTranscriptsForRecording(recordingId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTranscriptCount(): Int {
        return transcriptDao.getTranscriptCount()
    }

    override fun getHighConfidenceTranscripts(threshold: Double): Flow<List<Transcript>> {
        return transcriptDao.getHighConfidenceTranscripts(threshold)
            .map { entities -> mapper.toDomainList(entities) }
    }
}
