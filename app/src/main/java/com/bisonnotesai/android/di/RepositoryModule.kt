package com.bisonnotesai.android.di

import com.bisonnotesai.android.data.repository.ProcessingJobRepositoryImpl
import com.bisonnotesai.android.data.repository.RecordingRepositoryImpl
import com.bisonnotesai.android.data.repository.SummaryRepositoryImpl
import com.bisonnotesai.android.data.repository.TranscriptRepositoryImpl
import com.bisonnotesai.android.domain.repository.ProcessingJobRepository
import com.bisonnotesai.android.domain.repository.RecordingRepository
import com.bisonnotesai.android.domain.repository.SummaryRepository
import com.bisonnotesai.android.domain.repository.TranscriptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings
 * Connects interfaces to implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        impl: RecordingRepositoryImpl
    ): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptRepository(
        impl: TranscriptRepositoryImpl
    ): TranscriptRepository

    @Binds
    @Singleton
    abstract fun bindSummaryRepository(
        impl: SummaryRepositoryImpl
    ): SummaryRepository

    @Binds
    @Singleton
    abstract fun bindProcessingJobRepository(
        impl: ProcessingJobRepositoryImpl
    ): ProcessingJobRepository
}
