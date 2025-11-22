package com.bisonnotesai.android.data.transcription.openai

import com.google.gson.annotations.SerializedName

/**
 * OpenAI Whisper transcription models
 */
enum class OpenAITranscribeModel(val modelId: String) {
    GPT_4O_TRANSCRIBE("gpt-4o-transcribe"),
    GPT_4O_MINI_TRANSCRIBE("gpt-4o-mini-transcribe"),
    WHISPER_1("whisper-1");

    val displayName: String
        get() = when (this) {
            GPT_4O_TRANSCRIBE -> "GPT-4o Transcribe"
            GPT_4O_MINI_TRANSCRIBE -> "GPT-4o Mini Transcribe"
            WHISPER_1 -> "Whisper-1"
        }

    val description: String
        get() = when (this) {
            GPT_4O_TRANSCRIBE -> "Most robust transcription with GPT-4o model"
            GPT_4O_MINI_TRANSCRIBE -> "Cheapest and fastest transcription with GPT-4o Mini model"
            WHISPER_1 -> "Legacy transcription with Whisper V2 model"
        }

    val supportsStreaming: Boolean
        get() = when (this) {
            GPT_4O_TRANSCRIBE, GPT_4O_MINI_TRANSCRIBE -> true
            WHISPER_1 -> false
        }

    companion object {
        fun fromModelId(modelId: String): OpenAITranscribeModel {
            return values().find { it.modelId == modelId } ?: WHISPER_1
        }
    }
}

/**
 * Configuration for OpenAI transcription service
 */
data class OpenAIConfig(
    val apiKey: String,
    val model: OpenAITranscribeModel = OpenAITranscribeModel.GPT_4O_MINI_TRANSCRIBE,
    val baseURL: String = "https://api.openai.com/v1",
    val temperature: Double = 0.0,
    val language: String? = "en",
    val timeout: Long = 1800000L // 30 minutes in milliseconds
) {
    companion object {
        val DEFAULT = OpenAIConfig(
            apiKey = "",
            model = OpenAITranscribeModel.GPT_4O_MINI_TRANSCRIBE,
            baseURL = "https://api.openai.com/v1"
        )
    }
}

/**
 * OpenAI transcription response
 */
data class OpenAITranscribeResponse(
    @SerializedName("text")
    val text: String,

    @SerializedName("usage")
    val usage: OpenAIUsage? = null
)

/**
 * Usage statistics from OpenAI API
 */
data class OpenAIUsage(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("input_tokens")
    val inputTokens: Int? = null,

    @SerializedName("input_token_details")
    val inputTokenDetails: OpenAIInputTokenDetails? = null,

    @SerializedName("output_tokens")
    val outputTokens: Int? = null,

    @SerializedName("total_tokens")
    val totalTokens: Int? = null
)

/**
 * Detailed input token breakdown
 */
data class OpenAIInputTokenDetails(
    @SerializedName("text_tokens")
    val textTokens: Int? = null,

    @SerializedName("audio_tokens")
    val audioTokens: Int? = null
)

/**
 * OpenAI error response
 */
data class OpenAIErrorResponse(
    @SerializedName("error")
    val error: OpenAIError
)

/**
 * OpenAI error details
 */
data class OpenAIError(
    @SerializedName("message")
    val message: String,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("code")
    val code: String? = null
)

/**
 * Response from models list endpoint (for connection testing)
 */
data class OpenAIModelsListResponse(
    @SerializedName("data")
    val data: List<OpenAIModelInfo>,

    @SerializedName("object")
    val `object`: String? = null
)

/**
 * Information about an OpenAI model
 */
data class OpenAIModelInfo(
    @SerializedName("id")
    val id: String,

    @SerializedName("object")
    val `object`: String,

    @SerializedName("created")
    val created: Long? = null,

    @SerializedName("owned_by")
    val ownedBy: String? = null
)

/**
 * OpenAI-specific exceptions
 */
sealed class OpenAIException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ConfigurationMissing : OpenAIException("OpenAI API key is missing. Please configure your API key in settings.")

    class FileNotFound : OpenAIException("Audio file not found or inaccessible.")

    class FileTooLarge(size: Long) : OpenAIException("File size ${size / 1024 / 1024}MB exceeds 25MB limit")

    class AuthenticationFailed(details: String) : OpenAIException("Authentication failed: $details. Please check your API key.")

    class APIError(message: String) : OpenAIException("OpenAI API error: $message")

    class InvalidResponse(message: String) : OpenAIException("Invalid response from OpenAI: $message")

    class NetworkError(cause: Throwable) : OpenAIException("Network error: ${cause.message}", cause)
}
