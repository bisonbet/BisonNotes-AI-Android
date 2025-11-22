package com.bisonnotesai.android.data.repository

import com.bisonnotesai.android.data.local.database.dao.SummaryDao
import com.bisonnotesai.android.data.mapper.SummaryMapper
import com.bisonnotesai.android.domain.model.AIEngine
import com.bisonnotesai.android.domain.model.ContentType
import com.bisonnotesai.android.domain.model.Summary
import com.bisonnotesai.android.domain.repository.SummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of SummaryRepository
 * Bridges domain layer with data layer using DAOs and mappers
 */
class SummaryRepositoryImpl @Inject constructor(
    private val summaryDao: SummaryDao,
    private val mapper: SummaryMapper
) : SummaryRepository {

    override fun getAllSummaries(): Flow<List<Summary>> {
        return summaryDao.getAllSummaries()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun getSummary(id: String): Summary? {
        return summaryDao.getSummary(id)?.let { mapper.toDomain(it) }
    }

    override fun getSummaryFlow(id: String): Flow<Summary?> {
        return summaryDao.getSummaryFlow(id)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }

    override suspend fun getSummaryForRecording(recordingId: String): Summary? {
        return summaryDao.getSummaryForRecording(recordingId)?.let { mapper.toDomain(it) }
    }

    override fun getSummaryForRecordingFlow(recordingId: String): Flow<Summary?> {
        return summaryDao.getSummaryForRecordingFlow(recordingId)
            .map { entity -> entity?.let { mapper.toDomain(it) } }
    }

    override fun getAllSummariesForRecording(recordingId: String): Flow<List<Summary>> {
        return summaryDao.getAllSummariesForRecording(recordingId)
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getSummariesByMethod(aiEngine: AIEngine): Flow<List<Summary>> {
        return summaryDao.getSummariesByMethod(aiEngine.name.lowercase())
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getSummariesByContentType(contentType: ContentType): Flow<List<Summary>> {
        return summaryDao.getSummariesByContentType(contentType.name.lowercase())
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getSummariesWithTasks(): Flow<List<Summary>> {
        return summaryDao.getSummariesWithTasks()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override fun getSummariesWithReminders(): Flow<List<Summary>> {
        return summaryDao.getSummariesWithReminders()
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun saveSummary(summary: Summary): Result<String> {
        return try {
            val entity = mapper.toEntity(summary)
            summaryDao.insert(entity)
            Result.success(summary.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSummary(summary: Summary): Result<Unit> {
        return try {
            val entity = mapper.toEntity(summary)
            summaryDao.update(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSummary(id: String): Result<Unit> {
        return try {
            summaryDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSummariesForRecording(recordingId: String): Result<Unit> {
        return try {
            summaryDao.deleteSummariesForRecording(recordingId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummaryCount(): Int {
        return summaryDao.getSummaryCount()
    }

    override fun getHighConfidenceSummaries(threshold: Double): Flow<List<Summary>> {
        return summaryDao.getHighConfidenceSummaries(threshold)
            .map { entities -> mapper.toDomainList(entities) }
    }

    override suspend fun getOrphanedSummaries(): List<Summary> {
        return mapper.toDomainList(summaryDao.getOrphanedSummaries())
    }
}
