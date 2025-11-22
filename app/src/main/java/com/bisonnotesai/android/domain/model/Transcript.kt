package com.bisonnotesai.android.domain.model

import java.util.Date

/**
 * Domain model for a transcript
 * Business logic representation
 *
 * Maps to TranscriptEntity in data layer
 */
data class Transcript(
    val id: String,
    val recordingId: String,
    val segments: List<TranscriptSegment>,
    val speakerMappings: Map<String, String>, // speaker ID -> name
    val engine: TranscriptionEngine,
    val confidence: Double, // 0.0 to 1.0
    val processingTime: Double, // in seconds
    val createdAt: Date,
    val lastModified: Date
) {
    /**
     * Get full text of transcript
     */
    fun fullText(): String {
        return segments.joinToString(" ") { it.text }
    }

    /**
     * Get transcript with speaker labels
     */
    fun formattedText(): String {
        return segments.joinToString("\n") { segment ->
            val speakerName = segment.speaker?.let { speakerMappings[it] ?: "Speaker $it" }
            if (speakerName != null) {
                "$speakerName: ${segment.text}"
            } else {
                segment.text
            }
        }
    }

    /**
     * Get word count
     */
    fun wordCount(): Int {
        return fullText().split("\\s+".toRegex()).size
    }

    /**
     * Get confidence level description
     */
    fun confidenceLevel(): String {
        return when {
            confidence >= 0.9 -> "High"
            confidence >= 0.7 -> "Medium"
            else -> "Low"
        }
    }

    /**
     * Get total duration from segments
     */
    fun totalDuration(): Double {
        return segments.maxOfOrNull { it.end } ?: 0.0
    }
}

/**
 * Individual segment of transcript with timing
 */
data class TranscriptSegment(
    val text: String,
    val start: Double, // start time in seconds
    val end: Double,   // end time in seconds
    val speaker: String? = null, // speaker ID
    val confidence: Double? = null // optional per-segment confidence
) {
    /**
     * Duration of this segment
     */
    fun duration(): Double = end - start

    /**
     * Formatted timestamp for display
     */
    fun formattedTimestamp(): String {
        val minutes = (start / 60).toInt()
        val seconds = (start % 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Transcription engine enum
 */
enum class TranscriptionEngine {
    ANDROID_SPEECH,  // Built-in Android SpeechRecognizer
    OPENAI_WHISPER,  // OpenAI Whisper API
    AWS_TRANSCRIBE,  // AWS Transcribe
    LOCAL_WHISPER,   // Local Whisper server
    GOOGLE_SPEECH,   // Google Cloud Speech-to-Text
    CUSTOM;          // Custom/other engine

    companion object {
        fun fromString(value: String?): TranscriptionEngine {
            return when (value?.lowercase()) {
                "android", "android_speech" -> ANDROID_SPEECH
                "openai", "whisper", "openai_whisper" -> OPENAI_WHISPER
                "aws", "aws_transcribe" -> AWS_TRANSCRIBE
                "local", "local_whisper" -> LOCAL_WHISPER
                "google", "google_speech" -> GOOGLE_SPEECH
                else -> CUSTOM
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            ANDROID_SPEECH -> "Android Speech"
            OPENAI_WHISPER -> "OpenAI Whisper"
            AWS_TRANSCRIBE -> "AWS Transcribe"
            LOCAL_WHISPER -> "Local Whisper"
            GOOGLE_SPEECH -> "Google Speech"
            CUSTOM -> "Custom"
        }
    }
}
