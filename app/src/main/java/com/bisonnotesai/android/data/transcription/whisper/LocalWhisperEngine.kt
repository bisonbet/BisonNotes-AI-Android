package com.bisonnotesai.android.data.transcription.whisper

import android.util.Log
import com.bisonnotesai.android.domain.model.TranscriptSegment
import com.bisonnotesai.android.transcription.TranscriptionResult
import com.bisonnotesai.android.transcription.TranscriptionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Whisper server transcription engine
 * Connects to self-hosted Whisper server for privacy-focused transcription
 *
 * Supports two protocols:
 * - REST API (port 9000) - HTTP multipart uploads [IMPLEMENTED]
 * - Wyoming Protocol (port 10300) - WebSocket streaming [TODO: Future enhancement]
 *
 * This allows users to run their own Whisper model locally without sending
 * audio data to cloud services.
 */
@Singleton
class LocalWhisperEngine @Inject constructor(
    private val api: WhisperApi,
    private val config: WhisperConfig
) : TranscriptionService {

    companion object {
        private const val TAG = "LocalWhisperEngine"
        private const val CONNECTION_TIMEOUT_MS = 10000L
    }

    private var isCancelled = false

    override suspend fun transcribe(
        audioFile: File,
        language: String
    ): Flow<TranscriptionResult> = flow {
        Log.d(TAG, "Starting local Whisper transcription for: ${audioFile.name}")
        Log.d(TAG, "Server: ${config.baseURL}, Protocol: ${config.protocol.protocolName}")
        isCancelled = false

        try {
            // Route to appropriate protocol handler
            when (config.protocol) {
                WhisperProtocol.REST -> {
                    transcribeWithREST(audioFile, language).collect { result ->
                        emit(result)
                    }
                }
                WhisperProtocol.WYOMING -> {
                    // Wyoming protocol support (WebSocket-based)
                    // TODO: Implement Wyoming protocol client
                    emit(TranscriptionResult.Error(
                        "Wyoming protocol not yet implemented. Please use REST protocol.",
                        LocalWhisperException.UnsupportedProtocol("Wyoming")
                    ))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            if (!isCancelled) {
                emit(TranscriptionResult.Error(
                    e.message ?: "Transcription failed",
                    e
                ))
            }
        }
    }

    /**
     * Transcribe using REST API protocol
     */
    private suspend fun transcribeWithREST(
        audioFile: File,
        language: String
    ): Flow<TranscriptionResult> = flow {
        Log.d(TAG, "Using REST API for transcription")

        // Validate file
        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file not found: ${audioFile.absolutePath}")
            emit(TranscriptionResult.Error(
                "Audio file not found",
                LocalWhisperException.AudioProcessingFailed("File not found")
            ))
            return@flow
        }

        val fileSize = audioFile.length()
        Log.d(TAG, "Audio file size: ${fileSize / 1024 / 1024}MB")

        if (fileSize == 0L) {
            Log.e(TAG, "Audio file is empty")
            emit(TranscriptionResult.Error(
                "Audio file is empty",
                LocalWhisperException.AudioProcessingFailed("File is empty")
            ))
            return@flow
        }

        emit(TranscriptionResult.Progress(10, "Preparing audio file..."))

        if (isCancelled) {
            emit(TranscriptionResult.Error("Transcription cancelled"))
            return@flow
        }

        // Create multipart body
        val requestFile = audioFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("audio_file", audioFile.name, requestFile)

        emit(TranscriptionResult.Progress(20, "Sending to Whisper server..."))
        Log.d(TAG, "Uploading to: ${config.restAPIBaseURL}/asr")

        if (isCancelled) {
            emit(TranscriptionResult.Error("Transcription cancelled"))
            return@flow
        }

        // Send request
        val startTime = System.currentTimeMillis()
        val response = api.transcribeAudio(
            file = filePart,
            output = "json",
            task = "transcribe",
            language = config.language,
            wordTimestamps = config.enableWordTimestamps,
            vadFilter = false,
            encode = true,
            diarize = config.enableSpeakerDiarization,
            minSpeakers = if (config.enableSpeakerDiarization) config.minSpeakers else null,
            maxSpeakers = if (config.enableSpeakerDiarization) config.maxSpeakers else null
        )

        emit(TranscriptionResult.Progress(80, "Processing results..."))

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "Server error (${response.code()}): $errorBody")

            emit(TranscriptionResult.Error(
                "Server error: HTTP ${response.code()}",
                LocalWhisperException.ServerError("HTTP ${response.code()}: ${errorBody ?: "Unknown error"}")
            ))
            return@flow
        }

        val whisperResponse = response.body()
        if (whisperResponse == null) {
            Log.e(TAG, "Response body is null")
            emit(TranscriptionResult.Error(
                "Invalid response from server",
                LocalWhisperException.InvalidResponse("Response body is null")
            ))
            return@flow
        }

        val processingTime = (System.currentTimeMillis() - startTime) / 1000.0
        Log.d(TAG, "Transcription completed in ${processingTime}s")
        Log.d(TAG, "Transcript length: ${whisperResponse.text.length} characters")
        Log.d(TAG, "Segments: ${whisperResponse.segments?.size ?: 0}")
        Log.d(TAG, "Detected language: ${whisperResponse.language ?: "unknown"}")

        // Check for empty transcript
        if (whisperResponse.text.trim().isEmpty()) {
            Log.w(TAG, "Whisper returned empty transcript")
            emit(TranscriptionResult.Error(
                "Empty transcript - audio may contain no clear speech",
                LocalWhisperException.AudioProcessingFailed("No speech detected")
            ))
            return@flow
        }

        // Convert segments to domain model
        val segments = if (whisperResponse.segments.isNullOrEmpty()) {
            // No segments - create single segment
            listOf(
                TranscriptSegment(
                    text = whisperResponse.text,
                    start = 0.0,
                    end = 0.0,
                    speaker = "Speaker",
                    confidence = null
                )
            )
        } else {
            // Consolidate segments to prevent UI fragmentation
            val firstSegment = whisperResponse.segments.first()
            val lastSegment = whisperResponse.segments.last()
            val consolidatedText = whisperResponse.segments.joinToString(" ") { it.text }

            listOf(
                TranscriptSegment(
                    text = consolidatedText,
                    start = firstSegment.start,
                    end = lastSegment.end,
                    speaker = firstSegment.speaker ?: "Speaker",
                    confidence = whisperResponse.segments
                        .mapNotNull { it.avgLogprob }
                        .average()
                        .takeIf { !it.isNaN() }
                )
            )
        }

        emit(TranscriptionResult.Progress(100, "Transcription complete"))
        emit(TranscriptionResult.Success(
            segments = segments,
            fullText = whisperResponse.text
        ))
    }

    override fun isSupported(): Boolean {
        // Local Whisper is always "supported" but requires server to be running
        return true
    }

    override fun getSupportedLanguages(): List<String> {
        // Whisper supports 99+ languages
        return listOf(
            "en", "es", "fr", "de", "it", "pt", "nl", "pl", "ru",
            "zh", "ja", "ko", "ar", "hi", "tr", "vi", "th", "id",
            "uk", "el", "cs", "ro", "da", "fi", "sv", "no"
        )
    }

    override fun cancel() {
        Log.d(TAG, "Cancelling transcription")
        isCancelled = true
    }

    /**
     * Test connection to local Whisper server
     */
    suspend fun testConnection(): Result<String> {
        return try {
            Log.d(TAG, "Testing connection to: ${config.restAPIBaseURL}")

            when (config.protocol) {
                WhisperProtocol.REST -> {
                    val response = api.testConnection()

                    // Even 405 (Method Not Allowed) means server is running
                    val isAvailable = response.code() == 200 || response.code() == 405

                    if (isAvailable) {
                        Log.d(TAG, "Connection test successful (HTTP ${response.code()})")
                        Result.success("Connection successful! Whisper server is running.")
                    } else {
                        Log.e(TAG, "Connection test failed (HTTP ${response.code()})")
                        Result.failure(
                            LocalWhisperException.ServerError("Server returned status ${response.code()}")
                        )
                    }
                }
                WhisperProtocol.WYOMING -> {
                    // TODO: Implement Wyoming connection test
                    Result.failure(
                        LocalWhisperException.UnsupportedProtocol("Wyoming protocol not yet implemented")
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(LocalWhisperException.NetworkError(e))
        }
    }

    /**
     * Check if Whisper server is available
     */
    suspend fun isServerAvailable(): Boolean {
        return testConnection().isSuccess
    }

    /**
     * Get server status message
     */
    suspend fun getServerStatus(): String {
        return testConnection().fold(
            onSuccess = { it },
            onFailure = { "Connection error: ${it.message}" }
        )
    }
}
