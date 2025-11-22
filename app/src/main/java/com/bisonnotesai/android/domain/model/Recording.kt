package com.bisonnotesai.android.domain.model

import java.util.Date

/**
 * Domain model for a recording
 * Business logic representation (clean of Room/Android dependencies)
 *
 * Maps to RecordingEntity in data layer
 */
data class Recording(
    val id: String,
    val name: String,
    val date: Date,
    val url: String?,
    val duration: Double, // in seconds
    val fileSize: Long, // in bytes
    val audioQuality: String?,

    // Location data
    val location: LocationData? = null,

    // Processing status
    val transcriptionStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val summaryStatus: ProcessingStatus = ProcessingStatus.PENDING,

    // Related entity IDs
    val transcriptId: String? = null,
    val summaryId: String? = null,

    // Timestamps
    val createdAt: Date,
    val lastModified: Date
) {
    /**
     * Returns a human-readable display name
     */
    fun displayName(): String = name.ifEmpty { "Untitled Recording" }

    /**
     * Returns formatted duration (e.g., "2:30" for 150 seconds)
     */
    fun formattedDuration(): String {
        val minutes = (duration / 60).toInt()
        val seconds = (duration % 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * Returns formatted file size (e.g., "15.2 MB")
     */
    fun formattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> String.format("%.1f KB", fileSize / 1024.0)
            fileSize < 1024 * 1024 * 1024 -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Check if recording has been transcribed
     */
    fun hasTranscript(): Boolean = transcriptId != null && transcriptionStatus == ProcessingStatus.COMPLETED

    /**
     * Check if recording has a summary
     */
    fun hasSummary(): Boolean = summaryId != null && summaryStatus == ProcessingStatus.COMPLETED

    /**
     * Check if any processing is in progress
     */
    fun isProcessing(): Boolean = transcriptionStatus == ProcessingStatus.PROCESSING ||
                                   summaryStatus == ProcessingStatus.PROCESSING
}

/**
 * Location data for a recording
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val address: String?,
    val timestamp: Date
) {
    /**
     * Returns display location (address if available, otherwise coordinates)
     */
    fun displayLocation(): String {
        return address ?: String.format("%.4f, %.4f", latitude, longitude)
    }
}

/**
 * Processing status enum
 */
enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    companion object {
        fun fromString(value: String?): ProcessingStatus {
            return when (value?.lowercase()) {
                "pending" -> PENDING
                "processing" -> PROCESSING
                "completed" -> COMPLETED
                "failed" -> FAILED
                else -> PENDING
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            PENDING -> "Pending"
            PROCESSING -> "Processing..."
            COMPLETED -> "Completed"
            FAILED -> "Failed"
        }
    }
}
