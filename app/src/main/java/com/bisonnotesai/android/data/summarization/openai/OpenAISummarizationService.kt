package com.bisonnotesai.android.data.summarization.openai

import android.util.Log
import com.bisonnotesai.android.data.preferences.OpenAISummarizationPreferences
import com.bisonnotesai.android.domain.model.*
import com.bisonnotesai.android.summarization.SummarizationException
import com.bisonnotesai.android.summarization.SummarizationResult
import com.bisonnotesai.android.summarization.SummarizationService
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI GPT summarization service implementation
 * Ported from iOS OpenAISummarizationEngine.swift and OpenAISummarizationService.swift
 */
@Singleton
class OpenAISummarizationService @Inject constructor(
    private val api: OpenAISummarizationApi,
    private val preferences: OpenAISummarizationPreferences,
    private val gson: Gson
) : SummarizationService {

    override val name: String = "OpenAI GPT"
    override val description: String = "Advanced AI-powered summaries using OpenAI's GPT models"

    companion object {
        private const val TAG = "OpenAISummarization"
    }

    override suspend fun isAvailable(): Boolean {
        val enabled = preferences.enabled.first()
        val apiKey = preferences.apiKey.first()
        return enabled && apiKey.isNotEmpty() && apiKey.startsWith("sk-")
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val apiKey = preferences.apiKey.first()
            if (apiKey.isEmpty()) return false

            // Simple test request
            val request = ChatCompletionRequest(
                model = preferences.model.first(),
                messages = listOf(
                    ChatMessage(role = "user", content = "Test connection")
                ),
                max_tokens = 10
            )

            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }

    override suspend fun generateSummary(text: String, contentType: ContentType): String {
        val startTime = System.currentTimeMillis()

        try {
            val apiKey = preferences.apiKey.first()
            val model = preferences.model.first()
            val temperature = preferences.temperature.first()
            val maxTokens = preferences.maxTokens.first()
            val timeout = preferences.timeout.first() * 1000L

            val systemPrompt = OpenAIPromptGenerator.createSystemPrompt(
                OpenAIPromptGenerator.PromptType.SUMMARY,
                contentType
            )
            val userPrompt = OpenAIPromptGenerator.createUserPrompt(
                OpenAIPromptGenerator.PromptType.SUMMARY,
                text
            )

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = temperature,
                max_tokens = maxTokens
            )

            val response = withTimeout(timeout) {
                api.createChatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
            }

            if (!response.isSuccessful) {
                throw handleApiError(response.code(), response.message())
            }

            val summary = response.body()?.choices?.firstOrNull()?.message?.content
                ?: throw SummarizationException.InvalidResponse("Empty response from API")

            Log.d(TAG, "Summary generated in ${System.currentTimeMillis() - startTime}ms")
            return summary.trim()

        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    override suspend fun extractTasks(text: String): List<Task> {
        try {
            val apiKey = preferences.apiKey.first()
            val model = preferences.model.first()

            val systemPrompt = OpenAIPromptGenerator.createSystemPrompt(
                OpenAIPromptGenerator.PromptType.TASKS
            )
            val userPrompt = OpenAIPromptGenerator.createUserPrompt(
                OpenAIPromptGenerator.PromptType.TASKS,
                text
            )

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.1,
                max_tokens = 1024,
                response_format = ResponseFormat(type = "json_object")
            )

            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (!response.isSuccessful) {
                throw handleApiError(response.code(), response.message())
            }

            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: return emptyList()

            return parseTasksFromJson(content)

        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    override suspend fun extractReminders(text: String): List<Reminder> {
        try {
            val apiKey = preferences.apiKey.first()
            val model = preferences.model.first()

            val systemPrompt = OpenAIPromptGenerator.createSystemPrompt(
                OpenAIPromptGenerator.PromptType.REMINDERS
            )
            val userPrompt = OpenAIPromptGenerator.createUserPrompt(
                OpenAIPromptGenerator.PromptType.REMINDERS,
                text
            )

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.1,
                max_tokens = 1024,
                response_format = ResponseFormat(type = "json_object")
            )

            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (!response.isSuccessful) {
                throw handleApiError(response.code(), response.message())
            }

            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: return emptyList()

            return parseRemindersFromJson(content)

        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    override suspend fun extractTitles(text: String): List<TitleSuggestion> {
        try {
            val apiKey = preferences.apiKey.first()
            val model = preferences.model.first()

            val systemPrompt = OpenAIPromptGenerator.createSystemPrompt(
                OpenAIPromptGenerator.PromptType.TITLES
            )
            val userPrompt = OpenAIPromptGenerator.createUserPrompt(
                OpenAIPromptGenerator.PromptType.TITLES,
                text
            )

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.3,
                max_tokens = 256,
                response_format = ResponseFormat(type = "json_object")
            )

            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (!response.isSuccessful) {
                throw handleApiError(response.code(), response.message())
            }

            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: return emptyList()

            return parseTitlesFromJson(content)

        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    override suspend fun classifyContent(text: String): ContentType {
        try {
            val apiKey = preferences.apiKey.first()
            val model = preferences.model.first()

            val systemPrompt = OpenAIPromptGenerator.createSystemPrompt(
                OpenAIPromptGenerator.PromptType.CONTENT_CLASSIFICATION
            )
            val userPrompt = OpenAIPromptGenerator.createUserPrompt(
                OpenAIPromptGenerator.PromptType.CONTENT_CLASSIFICATION,
                text.take(1000) // Use first 1000 chars for classification
            )

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.1,
                max_tokens = 50
            )

            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (!response.isSuccessful) {
                return ContentType.GENERAL
            }

            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: return ContentType.GENERAL

            return ContentType.fromString(content.trim().lowercase())

        } catch (e: Exception) {
            Log.e(TAG, "Content classification failed", e)
            return ContentType.GENERAL
        }
    }

    override suspend fun processComplete(text: String): SummarizationResult {
        val startTime = System.currentTimeMillis()

        try {
            val apiKey = preferences.apiKey.first()
            val model = preferences.model.first()
            val temperature = preferences.temperature.first()
            val maxTokens = preferences.maxTokens.first()
            val timeout = preferences.timeout.first() * 1000L

            // First classify content type
            val contentType = classifyContent(text)

            val systemPrompt = OpenAIPromptGenerator.createSystemPrompt(
                OpenAIPromptGenerator.PromptType.COMPLETE,
                contentType
            )
            val userPrompt = OpenAIPromptGenerator.createUserPrompt(
                OpenAIPromptGenerator.PromptType.COMPLETE,
                text
            )

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = temperature,
                max_tokens = maxTokens,
                response_format = ResponseFormat(type = "json_object")
            )

            val response = withTimeout(timeout) {
                api.createChatCompletion(
                    authorization = "Bearer $apiKey",
                    request = request
                )
            }

            if (!response.isSuccessful) {
                throw handleApiError(response.code(), response.message())
            }

            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: throw SummarizationException.InvalidResponse("Empty response from API")

            val processingTime = (System.currentTimeMillis() - startTime) / 1000.0
            return parseCompleteResponse(content, contentType, processingTime)

        } catch (e: Exception) {
            throw handleException(e)
        }
    }

    // MARK: - JSON Parsing

    private fun parseCompleteResponse(
        json: String,
        contentType: ContentType,
        processingTime: Double
    ): SummarizationResult {
        return try {
            val response = gson.fromJson(json, CompleteSummarizationResponse::class.java)

            SummarizationResult(
                summary = response.summary,
                tasks = response.tasks.map { parseTask(it) },
                reminders = response.reminders.map { parseReminder(it) },
                titles = response.titles.map { TitleSuggestion(it.text, it.confidence) },
                contentType = ContentType.fromString(response.content_type) ?: contentType,
                aiEngine = AIEngine.OPENAI_GPT4,
                processingTime = processingTime
            )
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse complete response", e)
            throw SummarizationException.InvalidResponse("Failed to parse JSON response: ${e.message}")
        }
    }

    private fun parseTasksFromJson(json: String): List<Task> {
        return try {
            val wrapper = gson.fromJson(json, TasksWrapper::class.java)
            wrapper.tasks.map { parseTask(it) }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse tasks", e)
            emptyList()
        }
    }

    private fun parseRemindersFromJson(json: String): List<Reminder> {
        return try {
            val wrapper = gson.fromJson(json, RemindersWrapper::class.java)
            wrapper.reminders.map { parseReminder(it) }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse reminders", e)
            emptyList()
        }
    }

    private fun parseTitlesFromJson(json: String): List<TitleSuggestion> {
        return try {
            val wrapper = gson.fromJson(json, TitlesWrapper::class.java)
            wrapper.titles.map { TitleSuggestion(it.text, it.confidence) }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse titles", e)
            emptyList()
        }
    }

    private fun parseTask(task: TaskResponse): Task {
        return Task(
            text = task.text,
            priority = TaskPriority.fromString(task.priority),
            assignee = task.assignee,
            dueDate = task.due_date?.let { parseDateString(it) }
        )
    }

    private fun parseReminder(reminder: ReminderResponse): Reminder {
        return Reminder(
            text = reminder.text,
            date = reminder.date?.let { parseDateString(it) },
            importance = ReminderImportance.fromString(reminder.importance)
        )
    }

    private fun parseDateString(dateStr: String): Date? {
        val formats = listOf(
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                return sdf.parse(dateStr)
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }

    // MARK: - Error Handling

    private fun handleApiError(code: Int, message: String): SummarizationException {
        return when (code) {
            401 -> SummarizationException.ApiError("Invalid API key")
            429 -> SummarizationException.RateLimitExceeded()
            402, 403 -> SummarizationException.QuotaExceeded()
            else -> SummarizationException.ApiError("API error: $code - $message")
        }
    }

    private fun handleException(e: Exception): SummarizationException {
        return when (e) {
            is SummarizationException -> e
            is kotlinx.coroutines.TimeoutCancellationException -> SummarizationException.Timeout()
            is java.net.UnknownHostException, is java.net.ConnectException ->
                SummarizationException.NetworkError(e)
            else -> SummarizationException.ApiError("Unexpected error: ${e.message}", e)
        }
    }

    // MARK: - Wrapper classes for JSON parsing

    private data class TasksWrapper(val tasks: List<TaskResponse>)
    private data class RemindersWrapper(val reminders: List<ReminderResponse>)
    private data class TitlesWrapper(val titles: List<TitleResponse>)
}
