package com.bisonnotesai.android.transcription

import com.bisonnotesai.android.domain.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Interface for transcription services
 * Defines contract for converting audio to text
 */
interface TranscriptionService {

    /**
     * Transcribe an audio file
     * Returns Flow of transcription progress and results
     */
    suspend fun transcribe(
        audioFile: File,
        language: String = "en-US"
    ): Flow<TranscriptionResult>

    /**
     * Check if transcription is supported on this device
     */
    fun isSupported(): Boolean

    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<String>

    /**
     * Cancel ongoing transcription
     */
    fun cancel()
}

/**
 * Transcription result sealed class
 */
sealed class TranscriptionResult {
    data class Progress(val percentage: Int, val message: String) : TranscriptionResult()
    data class PartialResult(val text: String, val isFinal: Boolean) : TranscriptionResult()
    data class Success(val segments: List<TranscriptSegment>, val fullText: String) : TranscriptionResult()
    data class Error(val message: String, val throwable: Throwable? = null) : TranscriptionResult()
}

/**
 * Transcription configuration
 */
data class TranscriptionConfig(
    val language: String = "en-US",
    val enablePunctuation: Boolean = true,
    val enableWordTimestamps: Boolean = true,
    val chunkSizeSeconds: Int = 30,
    val maxRetries: Int = 3
)
