package com.bisonnotesai.android.summarization

import com.bisonnotesai.android.domain.model.*

/**
 * Summarization service interface
 * All AI engines must implement this interface
 */
interface SummarizationService {
    /**
     * Name of the AI engine (e.g., "OpenAI GPT-4", "Claude Sonnet")
     */
    val name: String

    /**
     * Description of the AI engine
     */
    val description: String

    /**
     * Whether this engine is currently available (credentials configured, etc.)
     */
    suspend fun isAvailable(): Boolean

    /**
     * Test connection to the AI service
     */
    suspend fun testConnection(): Boolean

    /**
     * Generate a summary from the given text
     * @param text The text to summarize (transcript)
     * @param contentType The type of content (meeting, lecture, etc.)
     * @return The generated summary
     */
    suspend fun generateSummary(text: String, contentType: ContentType): String

    /**
     * Extract tasks from the given text
     * @param text The text to analyze
     * @return List of extracted tasks
     */
    suspend fun extractTasks(text: String): List<Task>

    /**
     * Extract reminders from the given text
     * @param text The text to analyze
     * @return List of extracted reminders
     */
    suspend fun extractReminders(text: String): List<Reminder>

    /**
     * Extract title suggestions from the given text
     * @param text The text to analyze
     * @return List of title suggestions with confidence scores
     */
    suspend fun extractTitles(text: String): List<TitleSuggestion>

    /**
     * Classify the content type of the given text
     * @param text The text to classify
     * @return The detected content type
     */
    suspend fun classifyContent(text: String): ContentType

    /**
     * Process complete - generate summary, extract tasks, reminders, titles, and classify content
     * This is more efficient than calling each method separately
     * @param text The text to process
     * @return Complete processing result
     */
    suspend fun processComplete(text: String): SummarizationResult
}

/**
 * Result of complete summarization processing
 */
data class SummarizationResult(
    val summary: String,
    val tasks: List<Task>,
    val reminders: List<Reminder>,
    val titles: List<TitleSuggestion>,
    val contentType: ContentType,
    val aiEngine: AIEngine,
    val processingTime: Double, // in seconds
    val confidence: Double = 0.85
)

/**
 * Summarization exceptions
 */
sealed class SummarizationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ServiceUnavailable(service: String) : SummarizationException("AI service unavailable: $service")
    class ApiError(message: String, cause: Throwable? = null) : SummarizationException(message, cause)
    class InvalidResponse(message: String) : SummarizationException("Invalid AI response: $message")
    class Timeout : SummarizationException("Summarization request timed out")
    class QuotaExceeded : SummarizationException("API quota exceeded")
    class RateLimitExceeded : SummarizationException("Rate limit exceeded, please try again later")
    class NetworkError(cause: Throwable) : SummarizationException("Network error occurred", cause)
}
