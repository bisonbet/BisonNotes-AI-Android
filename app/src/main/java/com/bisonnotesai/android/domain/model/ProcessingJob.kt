package com.bisonnotesai.android.domain.model

import java.util.Date

/**
 * Domain model for a background processing job
 * Business logic representation
 *
 * Maps to ProcessingJobEntity in data layer
 */
data class ProcessingJob(
    val id: String,
    val recordingId: String?,
    val jobType: JobType,
    val engine: String,
    val status: JobStatus,
    val progress: Double, // 0.0 to 100.0
    val recordingName: String?,
    val recordingUrl: String?,
    val error: String?,
    val startTime: Date,
    val completionTime: Date?,
    val lastModified: Date
) {
    /**
     * Check if job is active (queued or processing)
     */
    fun isActive(): Boolean {
        return status == JobStatus.QUEUED || status == JobStatus.PROCESSING
    }

    /**
     * Check if job is completed (success or failure)
     */
    fun isCompleted(): Boolean {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED
    }

    /**
     * Get processing duration
     */
    fun processingDuration(): Long? {
        return completionTime?.let { it.time - startTime.time }
    }

    /**
     * Get formatted processing time
     */
    fun formattedProcessingTime(): String {
        val duration = processingDuration() ?: return "In progress..."
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${remainingSeconds}s"
        }
    }

    /**
     * Get progress percentage as integer
     */
    fun progressPercentage(): Int = progress.toInt()
}

/**
 * Job type enum
 */
enum class JobType {
    TRANSCRIPTION,
    SUMMARIZATION,
    TRANSLATION,
    EXPORT,
    IMPORT,
    OTHER;

    companion object {
        fun fromString(value: String?): JobType {
            return when (value?.lowercase()) {
                "transcription" -> TRANSCRIPTION
                "summarization" -> SUMMARIZATION
                "translation" -> TRANSLATION
                "export" -> EXPORT
                "import" -> IMPORT
                else -> OTHER
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            TRANSCRIPTION -> "Transcription"
            SUMMARIZATION -> "Summarization"
            TRANSLATION -> "Translation"
            EXPORT -> "Export"
            IMPORT -> "Import"
            OTHER -> "Processing"
        }
    }
}

/**
 * Job status enum
 */
enum class JobStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED;

    companion object {
        fun fromString(value: String?): JobStatus {
            return when (value?.lowercase()) {
                "queued" -> QUEUED
                "processing" -> PROCESSING
                "completed" -> COMPLETED
                "failed" -> FAILED
                else -> QUEUED
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            QUEUED -> "Queued"
            PROCESSING -> "Processing"
            COMPLETED -> "Completed"
            FAILED -> "Failed"
        }
    }
}
