package com.bisonnotesai.android.di

import android.content.Context
import androidx.room.Room
import com.bisonnotesai.android.data.local.database.BisonNotesDatabase
import com.bisonnotesai.android.data.local.database.dao.ProcessingJobDao
import com.bisonnotesai.android.data.local.database.dao.RecordingDao
import com.bisonnotesai.android.data.local.database.dao.SummaryDao
import com.bisonnotesai.android.data.local.database.dao.TranscriptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 * Provides singleton instances of database and DAOs
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BisonNotesDatabase {
        return Room.databaseBuilder(
            context,
            BisonNotesDatabase::class.java,
            BisonNotesDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // TODO: Implement proper migrations for production
            .build()
    }

    @Provides
    @Singleton
    fun provideRecordingDao(database: BisonNotesDatabase): RecordingDao {
        return database.recordingDao()
    }

    @Provides
    @Singleton
    fun provideTranscriptDao(database: BisonNotesDatabase): TranscriptDao {
        return database.transcriptDao()
    }

    @Provides
    @Singleton
    fun provideSummaryDao(database: BisonNotesDatabase): SummaryDao {
        return database.summaryDao()
    }

    @Provides
    @Singleton
    fun provideProcessingJobDao(database: BisonNotesDatabase): ProcessingJobDao {
        return database.processingJobDao()
    }
}
