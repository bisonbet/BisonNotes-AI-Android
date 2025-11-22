package com.bisonnotesai.android.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bisonnotesai.android.data.local.database.converter.DateConverter
import com.bisonnotesai.android.data.local.database.dao.ProcessingJobDao
import com.bisonnotesai.android.data.local.database.dao.RecordingDao
import com.bisonnotesai.android.data.local.database.dao.SummaryDao
import com.bisonnotesai.android.data.local.database.dao.TranscriptDao
import com.bisonnotesai.android.data.local.database.entity.ProcessingJobEntity
import com.bisonnotesai.android.data.local.database.entity.RecordingEntity
import com.bisonnotesai.android.data.local.database.entity.SummaryEntity
import com.bisonnotesai.android.data.local.database.entity.TranscriptEntity

/**
 * Main Room database for BisonNotes AI Android app
 *
 * Maps to iOS BisonNotes_AI.xcdatamodeld/BisonNotes_AI.xcdatamodel
 *
 * Features:
 * - 4 entities: Recording, Transcript, Summary, ProcessingJob
 * - Proper foreign key constraints with cascade/nullify behavior
 * - Type converters for Date objects
 * - Exportable schema for version control
 *
 * Version History:
 * - Version 1: Initial database schema (2025-11-22)
 */
@Database(
    entities = [
        RecordingEntity::class,
        TranscriptEntity::class,
        SummaryEntity::class,
        ProcessingJobEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class BisonNotesDatabase : RoomDatabase() {

    // DAO Accessors
    abstract fun recordingDao(): RecordingDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun summaryDao(): SummaryDao
    abstract fun processingJobDao(): ProcessingJobDao

    companion object {
        const val DATABASE_NAME = "bisonnotesai.db"
    }
}
