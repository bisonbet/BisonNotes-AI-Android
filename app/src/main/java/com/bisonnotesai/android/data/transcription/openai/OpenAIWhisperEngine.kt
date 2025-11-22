package com.bisonnotesai.android.data.transcription.openai

import android.util.Log
import com.bisonnotesai.android.domain.model.TranscriptSegment
import com.bisonnotesai.android.domain.model.TranscriptionEngine
import com.bisonnotesai.android.transcription.TranscriptionResult
import com.bisonnotesai.android.transcription.TranscriptionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI Whisper transcription engine
 * Implements transcription using OpenAI's Whisper API
 */
@Singleton
class OpenAIWhisperEngine @Inject constructor(
    private val api: OpenAIApi,
    private val config: OpenAIConfig
) : TranscriptionService {

    companion object {
        private const val TAG = "OpenAIWhisperEngine"
        private const val MAX_FILE_SIZE = 25 * 1024 * 1024L // 25MB
        private const val RESPONSE_FORMAT = "json"
    }

    private var isCancelled = false

    override suspend fun transcribe(
        audioFile: File,
        language: String
    ): Flow<TranscriptionResult> = flow {
        Log.d(TAG, "Starting OpenAI transcription for: ${audioFile.name}")
        isCancelled = false

        try {
            // Validate configuration
            if (config.apiKey.isBlank()) {
                Log.e(TAG, "API key is missing")
                emit(TranscriptionResult.Error("OpenAI API key is not configured", OpenAIException.ConfigurationMissing()))
                return@flow
            }

            // Validate file
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: ${audioFile.absolutePath}")
                emit(TranscriptionResult.Error("Audio file not found", OpenAIException.FileNotFound()))
                return@flow
            }

            val fileSize = audioFile.length()
            Log.d(TAG, "Audio file size: ${fileSize / 1024 / 1024}MB")

            if (fileSize > MAX_FILE_SIZE) {
                Log.e(TAG, "File too large: ${fileSize / 1024 / 1024}MB exceeds 25MB limit")
                emit(TranscriptionResult.Error(
                    "File size ${fileSize / 1024 / 1024}MB exceeds 25MB limit",
                    OpenAIException.FileTooLarge(fileSize)
                ))
                return@flow
            }

            // Emit progress
            emit(TranscriptionResult.Progress(10, "Preparing audio file..."))

            if (isCancelled) {
                emit(TranscriptionResult.Error("Transcription cancelled"))
                return@flow
            }

            // Create multipart body
            val contentType = getContentType(audioFile.name)
            Log.d(TAG, "Using content type: $contentType")

            val requestFile = audioFile.asRequestBody(contentType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

            val modelPart = config.model.modelId.toRequestBody("text/plain".toMediaTypeOrNull())
            val responseFormatPart = RESPONSE_FORMAT.toRequestBody("text/plain".toMediaTypeOrNull())
            val languagePart = (config.language ?: "en").toRequestBody("text/plain".toMediaTypeOrNull())
            val temperaturePart = config.temperature.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            emit(TranscriptionResult.Progress(20, "Sending to OpenAI..."))
            Log.d(TAG, "Using model: ${config.model.displayName}")

            if (isCancelled) {
                emit(TranscriptionResult.Error("Transcription cancelled"))
                return@flow
            }

            // Send request
            val startTime = System.currentTimeMillis()
            val response = api.transcribeAudio(
                file = filePart,
                model = modelPart,
                responseFormat = responseFormatPart,
                language = languagePart,
                temperature = temperaturePart
            )

            emit(TranscriptionResult.Progress(80, "Processing results..."))

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API error (${response.code()}): $errorBody")

                // Try to parse error response
                val errorMessage = try {
                    val gson = com.google.gson.Gson()
                    val errorResponse = gson.fromJson(errorBody, OpenAIErrorResponse::class.java)
                    errorResponse.error.message
                } catch (e: Exception) {
                    "HTTP ${response.code()}: ${errorBody ?: "Unknown error"}"
                }

                when (response.code()) {
                    401, 403 -> {
                        emit(TranscriptionResult.Error(
                            "Authentication failed: $errorMessage",
                            OpenAIException.AuthenticationFailed(errorMessage)
                        ))
                    }
                    else -> {
                        emit(TranscriptionResult.Error(
                            "API error: $errorMessage",
                            OpenAIException.APIError(errorMessage)
                        ))
                    }
                }
                return@flow
            }

            val transcribeResponse = response.body()
            if (transcribeResponse == null) {
                Log.e(TAG, "Response body is null")
                emit(TranscriptionResult.Error(
                    "Invalid response from OpenAI",
                    OpenAIException.InvalidResponse("Response body is null")
                ))
                return@flow
            }

            val processingTime = (System.currentTimeMillis() - startTime) / 1000.0
            Log.d(TAG, "Transcription completed in ${processingTime}s")
            Log.d(TAG, "Transcript length: ${transcribeResponse.text.length} characters")

            if (transcribeResponse.usage != null) {
                Log.d(TAG, "Token usage - Input: ${transcribeResponse.usage.inputTokens}, " +
                        "Output: ${transcribeResponse.usage.outputTokens}, " +
                        "Total: ${transcribeResponse.usage.totalTokens}")
            }

            // Create segments (OpenAI doesn't provide timestamps in basic response)
            val segments = listOf(
                TranscriptSegment(
                    text = transcribeResponse.text,
                    start = 0.0,
                    end = 0.0,
                    speaker = "Speaker",
                    confidence = null
                )
            )

            emit(TranscriptionResult.Progress(100, "Transcription complete"))
            emit(TranscriptionResult.Success(
                segments = segments,
                fullText = transcribeResponse.text
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            if (!isCancelled) {
                emit(TranscriptionResult.Error(
                    "Network error: ${e.message}",
                    OpenAIException.NetworkError(e)
                ))
            }
        }
    }

    override fun isSupported(): Boolean {
        // OpenAI Whisper is always supported (network-based)
        return true
    }

    override fun getSupportedLanguages(): List<String> {
        // OpenAI Whisper supports 99+ languages
        // Return common ones for now
        return listOf(
            "en", "es", "fr", "de", "it", "pt", "nl", "pl", "ru",
            "zh", "ja", "ko", "ar", "hi", "tr", "vi", "th", "id"
        )
    }

    override fun cancel() {
        Log.d(TAG, "Cancelling transcription")
        isCancelled = true
    }

    /**
     * Test connection to OpenAI API
     */
    suspend fun testConnection(): Result<String> {
        return try {
            Log.d(TAG, "Testing OpenAI API connection...")

            if (config.apiKey.isBlank()) {
                return Result.failure(OpenAIException.ConfigurationMissing())
            }

            val response = api.getModels()

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Connection test failed (${response.code()}): $errorBody")

                when (response.code()) {
                    401, 403 -> {
                        return Result.failure(
                            OpenAIException.AuthenticationFailed("HTTP ${response.code()}")
                        )
                    }
                    else -> {
                        return Result.failure(
                            OpenAIException.APIError("HTTP ${response.code()}: $errorBody")
                        )
                    }
                }
            }

            Log.d(TAG, "Connection test successful")
            Result.success("Connection successful! API key is valid.")

        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            Result.failure(OpenAIException.NetworkError(e))
        }
    }

    /**
     * Get content type for audio file based on extension
     */
    private fun getContentType(fileName: String): String {
        return when (fileName.substringAfterLast('.').lowercase()) {
            "mp3" -> "audio/mpeg"
            "mp4", "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "webm" -> "audio/webm"
            else -> "audio/mp4" // Default fallback
        }
    }
}
