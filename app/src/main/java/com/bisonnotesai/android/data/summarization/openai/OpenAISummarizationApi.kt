package com.bisonnotesai.android.data.summarization.openai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI Chat Completions API for summarization
 */
interface OpenAISummarizationApi {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}

/**
 * Chat completion request
 */
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.1,
    val max_tokens: Int = 2048,
    val response_format: ResponseFormat? = null
)

/**
 * Chat message
 */
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

/**
 * Response format
 */
data class ResponseFormat(
    val type: String // "json_object" or "text"
)

/**
 * Chat completion response
 */
data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage?
)

/**
 * Chat choice
 */
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String
)

/**
 * API usage information
 */
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

/**
 * JSON response structure for complete processing
 */
data class CompleteSummarizationResponse(
    val summary: String,
    val tasks: List<TaskResponse>,
    val reminders: List<ReminderResponse>,
    val titles: List<TitleResponse>,
    val content_type: String
)

/**
 * Task in JSON response
 */
data class TaskResponse(
    val text: String,
    val priority: String? = null,
    val assignee: String? = null,
    val due_date: String? = null
)

/**
 * Reminder in JSON response
 */
data class ReminderResponse(
    val text: String,
    val date: String? = null,
    val importance: String? = null
)

/**
 * Title in JSON response
 */
data class TitleResponse(
    val text: String,
    val confidence: Double
)
