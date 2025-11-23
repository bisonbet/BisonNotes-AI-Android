package com.bisonnotesai.android.data.transcription.whisper

import com.google.gson.annotations.SerializedName

/**
 * Local Whisper server protocol
 */
enum class WhisperProtocol(val protocolName: String) {
    REST("REST API"),
    WYOMING("Wyoming Protocol");

    companion object {
        fun fromString(name: String): WhisperProtocol {
            return when (name.uppercase()) {
                "REST" -> REST
                "WYOMING" -> WYOMING
                else -> REST
            }
        }
    }
}

/**
 * Local Whisper server configuration
 */
data class WhisperConfig(
    val serverURL: String,
    val port: Int,
    val protocol: WhisperProtocol,
    val language: String? = null,
    val enableWordTimestamps: Boolean = false,
    val enableSpeakerDiarization: Boolean = false,
    val minSpeakers: Int? = null,
    val maxSpeakers: Int? = null
) {
    /**
     * Full base URL with protocol and port
     */
    val baseURL: String
        get() {
            val scheme = when (protocol) {
                WhisperProtocol.REST -> if (serverURL.startsWith("https")) "https" else "http"
                WhisperProtocol.WYOMING -> if (serverURL.startsWith("wss")) "wss" else "ws"
            }

            // Extract hostname from URL if it contains scheme
            val hostname = serverURL
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("ws://")
                .removePrefix("wss://")

            return "$scheme://$hostname:$port"
        }

    /**
     * REST API base URL (always HTTP/HTTPS)
     */
    val restAPIBaseURL: String
        get() {
            val scheme = if (serverURL.startsWith("https")) "https" else "http"
            val hostname = serverURL
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("ws://")
                .removePrefix("wss://")

            // Use different port for REST vs Wyoming
            val restPort = if (protocol == WhisperProtocol.WYOMING) 9000 else port
            return "$scheme://$hostname:$restPort"
        }

    companion object {
        val DEFAULT_REST = WhisperConfig(
            serverURL = "http://localhost",
            port = 9000,
            protocol = WhisperProtocol.REST
        )

        val DEFAULT_WYOMING = WhisperConfig(
            serverURL = "ws://localhost",
            port = 10300,
            protocol = WhisperProtocol.WYOMING
        )
    }
}

/**
 * Whisper REST API transcription request
 */
data class WhisperTranscribeRequest(
    val output: String = "json",
    val task: String = "transcribe",
    val language: String? = null,
    val wordTimestamps: Boolean? = false,
    val vadFilter: Boolean? = false,
    val encode: Boolean? = true,
    val diarize: Boolean? = false,
    val minSpeakers: Int? = null,
    val maxSpeakers: Int? = null
)

/**
 * Whisper REST API transcription response
 */
data class WhisperTranscribeResponse(
    @SerializedName("text")
    val text: String,

    @SerializedName("segments")
    val segments: List<WhisperSegment>? = null,

    @SerializedName("language")
    val language: String? = null
)

/**
 * Whisper transcript segment
 */
data class WhisperSegment(
    @SerializedName("id")
    val id: Int,

    @SerializedName("start")
    val start: Double,

    @SerializedName("end")
    val end: Double,

    @SerializedName("text")
    val text: String,

    @SerializedName("avg_logprob")
    val avgLogprob: Double? = null,

    @SerializedName("compression_ratio")
    val compressionRatio: Double? = null,

    @SerializedName("no_speech_prob")
    val noSpeechProb: Double? = null,

    @SerializedName("speaker")
    val speaker: String? = null
)

/**
 * Language detection response
 */
data class LanguageDetectionResponse(
    @SerializedName("detected_language")
    val detectedLanguage: String,

    @SerializedName("language_code")
    val languageCode: String,

    @SerializedName("confidence")
    val confidence: Double
)

/**
 * Wyoming protocol message types
 */
enum class WyomingMessageType(val type: String) {
    INFO("info"),
    DESCRIBE("describe"),
    TRANSCRIBE("transcribe"),
    TRANSCRIPT("transcript"),
    ERROR("error"),
    AUDIO_START("audio-start"),
    AUDIO_CHUNK("audio-chunk"),
    AUDIO_STOP("audio-stop")
}

/**
 * Wyoming protocol message
 */
data class WyomingMessage(
    val type: String,
    val data: Map<String, Any>? = null
)

/**
 * Local Whisper exceptions
 */
sealed class LocalWhisperException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotConnected : LocalWhisperException("Not connected to Whisper service")

    class ServerError(message: String) : LocalWhisperException("Server error: $message")

    class AudioProcessingFailed(message: String) : LocalWhisperException("Audio processing failed: $message")

    class InvalidResponse(message: String) : LocalWhisperException("Invalid response: $message")

    class ConnectionTimeout : LocalWhisperException("Connection timeout - server not responding")

    class NetworkError(cause: Throwable) : LocalWhisperException("Network error: ${cause.message}", cause)

    class UnsupportedProtocol(protocol: String) : LocalWhisperException("Unsupported protocol: $protocol")
}
