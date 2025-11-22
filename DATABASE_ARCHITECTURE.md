# BisonNotes AI - Android Database Architecture

## Overview

This document describes the Room database architecture for the BisonNotes AI Android app, which is a direct port from the iOS Core Data implementation.

**Created:** 2025-11-22
**Database Version:** 1
**Status:** ✅ Complete - Phase 1

---

## Database Schema

### Entity Relationship Diagram

```
RecordingEntity (1) ──┬──< (0..1) TranscriptEntity
                      │
                      └──< (0..1) SummaryEntity
                      │
                      └──< (0..n) ProcessingJobEntity
```

### Entities

#### 1. RecordingEntity
**Table:** `recordings`
**Primary Key:** `id` (String/UUID)

Main entity for audio recordings. Contains recording metadata, location data, and status tracking.

**Columns:**
- `id`: UUID primary key
- `recordingName`: Display name
- `recordingDate`: When recording was created
- `recordingURL`: Relative file path (resilient to container changes)
- `duration`: Length in seconds
- `fileSize`: File size in bytes
- `audioQuality`: Quality setting used
- `locationLatitude`, `locationLongitude`, `locationAccuracy`: GPS coordinates
- `locationAddress`: Reverse-geocoded address
- `locationTimestamp`: When location was captured
- `transcriptionStatus`: pending|processing|completed|failed
- `summaryStatus`: pending|processing|completed|failed
- `transcriptId`: UUID of associated transcript
- `summaryId`: UUID of associated summary
- `createdAt`, `lastModified`: Timestamps

**Indexes:**
- `idx_recording_date` on `recordingDate`
- `idx_created_at` on `createdAt`

**Relationships:**
- 1:1 with TranscriptEntity (CASCADE delete)
- 1:1 with SummaryEntity (CASCADE delete)
- 1:n with ProcessingJobEntity (CASCADE delete)

---

#### 2. TranscriptEntity
**Table:** `transcripts`
**Primary Key:** `id` (String/UUID)
**Foreign Keys:** `recordingId` → `recordings.id` (CASCADE)

Stores transcription data from various AI engines.

**Columns:**
- `id`: UUID primary key
- `recordingId`: Foreign key to RecordingEntity
- `segments`: JSON array of TranscriptSegment objects
- `speakerMappings`: JSON object for speaker diarization
- `engine`: Transcription engine used (openai, aws, local, etc.)
- `confidence`: Confidence score (0.0-1.0)
- `processingTime`: Time taken in seconds
- `createdAt`, `lastModified`: Timestamps

**Indexes:**
- `idx_transcript_recording_id` on `recordingId`
- `idx_transcript_created_at` on `createdAt`

**Relationships:**
- n:1 with RecordingEntity
- 1:n with SummaryEntity (via transcriptId in SummaryEntity)

---

#### 3. SummaryEntity
**Table:** `summaries`
**Primary Key:** `id` (String/UUID)
**Foreign Keys:**
- `recordingId` → `recordings.id` (CASCADE)
- `transcriptId` → `transcripts.id` (SET NULL)

Stores AI-generated summaries with extracted tasks, reminders, and titles.

**Columns:**
- `id`: UUID primary key
- `recordingId`: Foreign key to RecordingEntity
- `transcriptId`: Foreign key to TranscriptEntity (optional)
- `summary`: Main summary text
- `titles`: JSON array of suggested titles
- `tasks`: JSON array of extracted tasks
- `reminders`: JSON array of extracted reminders
- `contentType`: meeting|lecture|interview|general|etc.
- `aiMethod`: AI engine used (openai, claude, gemini, ollama)
- `confidence`: Confidence score (0.0-1.0)
- `processingTime`: Time taken in seconds
- `originalLength`: Character count of original transcript
- `wordCount`: Word count in summary
- `compressionRatio`: Ratio of summary to original
- `version`: Version number (for regeneration tracking)
- `generatedAt`: When summary was created

**Indexes:**
- `idx_summary_recording_id` on `recordingId`
- `idx_summary_transcript_id` on `transcriptId`
- `idx_summary_generated_at` on `generatedAt`

**Relationships:**
- n:1 with RecordingEntity (CASCADE delete)
- n:1 with TranscriptEntity (SET NULL delete)

---

#### 4. ProcessingJobEntity
**Table:** `processing_jobs`
**Primary Key:** `id` (String/UUID)
**Foreign Keys:** `recordingId` → `recordings.id` (SET NULL)

Tracks background processing jobs for transcription and summarization.

**Columns:**
- `id`: UUID primary key
- `recordingId`: Foreign key to RecordingEntity (optional)
- `jobType`: transcription|summarization|etc.
- `engine`: Engine used for processing
- `status`: queued|processing|completed|failed
- `progress`: Progress percentage (0.0-100.0)
- `recordingName`: Denormalized for job history
- `recordingURL`: Denormalized for job history
- `error`: Error message if failed
- `startTime`: When job started
- `completionTime`: When job finished
- `lastModified`: Last update timestamp

**Indexes:**
- `idx_job_recording_id` on `recordingId`
- `idx_job_status` on `status`
- `idx_job_start_time` on `startTime`

**Relationships:**
- n:1 with RecordingEntity (SET NULL delete - keep job history)

---

## Data Integrity Features

### 1. Foreign Key Constraints

All relationships are enforced at the database level with proper cascade behavior:

- **RecordingEntity deletion**: CASCADE deletes all associated transcripts, summaries, and jobs
- **TranscriptEntity deletion**: SET NULL in summaries (preserves summaries even if transcript is removed)
- **ProcessingJobEntity**: SET NULL on recording deletion (preserves job history)

### 2. Indexes

Strategic indexes are placed on:
- All foreign keys
- Date columns used for sorting
- Status columns used for filtering
- Primary keys (automatic in Room)

### 3. Type Safety

- All IDs are String (UUID format) for consistency with iOS
- Dates are properly converted using TypeConverter
- JSON data is stored as String and validated during serialization/deserialization

### 4. Data Validation

- NOT NULL constraints on critical fields (id, recordingId in child tables)
- Default values for numeric fields (duration, fileSize, confidence)
- Status fields use String for flexibility but should be validated in code

---

## Migration from iOS Core Data

### Direct Mapping

| iOS Entity | Android Entity | Notes |
|------------|----------------|-------|
| RecordingEntry | RecordingEntity | 1:1 field mapping |
| TranscriptEntry | TranscriptEntity | 1:1 field mapping |
| SummaryEntry | SummaryEntity | 1:1 field mapping |
| ProcessingJobEntry | ProcessingJobEntity | 1:1 field mapping |

### Key Differences

1. **IDs**: iOS uses UUID type natively, Android stores as String
2. **Relationships**: iOS uses `@Relationship`, Android uses `@ForeignKey` + `@Relation`
3. **JSON Storage**: iOS uses separate attributes, Android stores as JSON strings
4. **Cascade Behavior**: Implemented differently but functionally equivalent

### URL Storage Strategy

Following iOS pattern of storing **relative paths** instead of absolute URLs to handle:
- App container ID changes (iOS issue)
- Installation directory changes (Android equivalent)
- Storage location changes

---

## DAO Operations

### RecordingDao
- Full CRUD operations
- Relationship queries (with transcript, with summary)
- URL migration helpers
- Cleanup operations for orphaned records

### TranscriptDao
- CRUD operations
- Recording relationship queries
- Engine-specific queries
- Confidence filtering

### SummaryDao
- CRUD operations
- Recording and transcript relationship queries
- Content type and AI method filtering
- Task and reminder queries
- Orphan detection

### ProcessingJobDao
- CRUD operations
- Status tracking and updates
- Active job queries
- Cleanup operations for completed jobs

---

## Usage Example

```kotlin
@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val recordingDao: RecordingDao,
    private val transcriptDao: TranscriptDao
) : ViewModel() {

    val recordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    suspend fun createRecording(name: String, url: String) {
        val recording = RecordingEntity(
            id = UUID.randomUUID().toString(),
            recordingName = name,
            recordingURL = url,
            recordingDate = Date(),
            transcriptionStatus = "pending"
        )
        recordingDao.insert(recording)
    }

    suspend fun getRecordingWithDetails(id: String): RecordingWithDetails? {
        return recordingDao.getRecordingWithDetails(id)
    }
}
```

---

## Data Integrity Checks

### Automatic (Database Level)
- ✅ Foreign key constraints prevent orphaned records
- ✅ CASCADE delete ensures referential integrity
- ✅ Indexes optimize query performance
- ✅ Type converters ensure data consistency

### Manual (Application Level)
- Orphaned summary detection: `summaryDao.getOrphanedSummaries()`
- Missing file cleanup: `recordingDao.cleanupOrphanedRecordings()`
- Completed job cleanup: `processingJobDao.deleteCompletedJobs()`

---

## Performance Considerations

### Optimized Queries
- All list queries return `Flow` for reactive updates
- Indexes on frequently queried columns
- Transaction support for complex operations

### Memory Management
- JSON data stored as TEXT to avoid memory overhead
- Large fields (segments, summary) only loaded when needed
- Pagination support available through Paging 3 library

### Background Operations
- All database operations are suspend functions
- Should be called from coroutine context
- Heavy operations should use IO dispatcher

---

## Testing Strategy

### Unit Tests
- DAO operations with in-memory database
- Foreign key constraint validation
- Cascade delete behavior
- Query correctness

### Integration Tests
- End-to-end recording workflow
- Transcript and summary creation
- Data integrity after operations
- Migration testing (when added)

---

## Future Enhancements

### Phase 2+
- [ ] Database migrations (currently uses fallbackToDestructiveMigration)
- [ ] Cloud sync support (sync status fields)
- [ ] Encryption for sensitive data
- [ ] Full-text search on transcripts and summaries
- [ ] Backup and restore functionality

---

## Comparison with iOS

### Strengths of Android Implementation
✅ Explicit foreign keys (more robust than Core Data relationships)
✅ Compile-time query validation
✅ Better migration tooling
✅ Flow-based reactive updates (cleaner than NSFetchedResultsController)
✅ Type-safe queries

### Equivalent Features
✅ Cascade delete behavior
✅ Relationship querying
✅ Index support
✅ Type conversion
✅ Background processing integration

---

## Version History

### Version 1 (2025-11-22)
- Initial database schema
- 4 entities: Recording, Transcript, Summary, ProcessingJob
- Foreign key relationships with CASCADE/SET NULL
- Indexes on key columns
- Type converters for Date
- DAO interfaces with comprehensive operations

---

## References

- iOS Core Data Model: `BisonNotes_AI.xcdatamodeld/BisonNotes_AI.xcdatamodel/contents`
- iOS CoreDataManager: `BisonNotes AI/Models/CoreDataManager.swift`
- Android Room Documentation: https://developer.android.com/training/data-storage/room
- Architecture Plan: `ANDROID_ARCHITECTURE_PLAN.md`

---

**Status:** ✅ Production Ready (Phase 1)
**Next Steps:** Repository layer implementation (Phase 1 continued)
