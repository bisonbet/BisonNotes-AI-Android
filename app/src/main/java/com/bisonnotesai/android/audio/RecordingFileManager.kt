package com.bisonnotesai.android.audio

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio recording files
 * Handles file creation, deletion, and organization
 */
@Singleton
class RecordingFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val RECORDINGS_DIRECTORY = "recordings"
        private const val AUDIO_EXTENSION = ".m4a"
        private const val TEMP_EXTENSION = ".tmp"
    }

    /**
     * Get the recordings directory
     * Creates if doesn't exist
     */
    fun getRecordingsDirectory(): File {
        val dir = File(context.filesDir, RECORDINGS_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Create a new recording file
     * Returns File object for the new recording
     */
    fun createRecordingFile(recordingId: String = UUID.randomUUID().toString()): File {
        val directory = getRecordingsDirectory()
        val filename = generateRecordingFilename(recordingId)
        return File(directory, filename)
    }

    /**
     * Create a temporary recording file
     * Used during recording, then renamed when complete
     */
    fun createTempRecordingFile(recordingId: String): File {
        val directory = getRecordingsDirectory()
        val filename = "$recordingId$TEMP_EXTENSION"
        return File(directory, filename)
    }

    /**
     * Finalize a temporary recording file
     * Renames from .tmp to .m4a
     */
    fun finalizeTempRecording(tempFile: File): File? {
        if (!tempFile.exists()) {
            return null
        }

        val finalFilename = tempFile.nameWithoutExtension + AUDIO_EXTENSION
        val finalFile = File(tempFile.parent, finalFilename)

        return if (tempFile.renameTo(finalFile)) {
            finalFile
        } else {
            null
        }
    }

    /**
     * Generate filename for recording
     * Format: {recordingId}.m4a
     */
    private fun generateRecordingFilename(recordingId: String): String {
        return "$recordingId$AUDIO_EXTENSION"
    }

    /**
     * Generate default recording name based on date/time
     * Format: "Recording YYYY-MM-DD HH:mm"
     */
    fun generateDefaultRecordingName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return "Recording ${dateFormat.format(Date())}"
    }

    /**
     * Get file for existing recording ID
     */
    fun getRecordingFile(recordingId: String): File {
        val directory = getRecordingsDirectory()
        val filename = generateRecordingFilename(recordingId)
        return File(directory, filename)
    }

    /**
     * Check if recording file exists
     */
    fun recordingFileExists(recordingId: String): Boolean {
        return getRecordingFile(recordingId).exists()
    }

    /**
     * Delete recording file
     */
    fun deleteRecordingFile(recordingId: String): Boolean {
        val file = getRecordingFile(recordingId)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Delete recording file by path
     */
    fun deleteRecordingFile(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Get file size in bytes
     */
    fun getFileSize(recordingId: String): Long {
        val file = getRecordingFile(recordingId)
        return if (file.exists()) {
            file.length()
        } else {
            0L
        }
    }

    /**
     * Get file URI for sharing/playback
     */
    fun getFileUri(recordingId: String): Uri {
        return Uri.fromFile(getRecordingFile(recordingId))
    }

    /**
     * Get all recording files
     */
    fun getAllRecordingFiles(): List<File> {
        val directory = getRecordingsDirectory()
        return directory.listFiles { file ->
            file.extension == AUDIO_EXTENSION.removePrefix(".")
        }?.toList() ?: emptyList()
    }

    /**
     * Clean up temporary files
     * Removes any leftover .tmp files
     */
    fun cleanupTempFiles(): Int {
        val directory = getRecordingsDirectory()
        val tempFiles = directory.listFiles { file ->
            file.extension == TEMP_EXTENSION.removePrefix(".")
        } ?: return 0

        var deletedCount = 0
        tempFiles.forEach { file ->
            if (file.delete()) {
                deletedCount++
            }
        }
        return deletedCount
    }

    /**
     * Get total storage used by recordings
     */
    fun getTotalStorageUsed(): Long {
        return getAllRecordingFiles().sumOf { it.length() }
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        val kilobyte = 1024L
        val megabyte = kilobyte * 1024
        val gigabyte = megabyte * 1024

        return when {
            bytes >= gigabyte -> String.format("%.2f GB", bytes.toDouble() / gigabyte)
            bytes >= megabyte -> String.format("%.2f MB", bytes.toDouble() / megabyte)
            bytes >= kilobyte -> String.format("%.2f KB", bytes.toDouble() / kilobyte)
            else -> "$bytes bytes"
        }
    }
}
