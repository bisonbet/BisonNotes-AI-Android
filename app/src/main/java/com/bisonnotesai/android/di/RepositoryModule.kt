package com.bisonnotesai.android.di

import com.bisonnotesai.android.data.repository.RecordingRepositoryImpl
import com.bisonnotesai.android.domain.repository.RecordingRepository
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

    // Additional repository bindings will be added as we implement them:
    // - TranscriptRepository
    // - SummaryRepository
    // - ProcessingJobRepository
}
