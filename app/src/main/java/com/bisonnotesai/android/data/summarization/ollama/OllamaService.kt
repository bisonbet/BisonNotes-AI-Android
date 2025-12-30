package com.bisonnotesai.android.data.summarization.ollama

import android.util.Log
import com.bisonnotesai.android.data.preferences.OllamaPreferences
import com.bisonnotesai.android.data.summarization.openai.OpenAIPromptGenerator
import com.bisonnotesai.android.domain.model.*
import com.bisonnotesai.android.summarization.SummarizationException
import com.bisonnotesai.android.summarization.SummarizationResult
import com.bisonnotesai.android.summarization.SummarizationService
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ollama local AI summarization service
 */
@Singleton
class OllamaService @Inject constructor(
    private val preferences: OllamaPreferences,
    private val gson: Gson,
    private val okHttpClient: OkHttpClient
) : SummarizationService {

    override val name: String = "Ollama"
    override val description: String = "Local AI with Ollama for privacy-focused summarization"

    companion object {
        private const val TAG = "OllamaService"
    }

    override suspend fun isAvailable(): Boolean {
        val enabled = preferences.enabled.first()
        val serverUrl = preferences.serverUrl.first()
        return enabled && serverUrl.isNotEmpty()
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val serverUrl = preferences.serverUrl.first()
            val request = Request.Builder()
                .url("$serverUrl/api/tags")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }

    override suspend fun generateSummary(text: String, contentType: ContentType): String {
        return try {
            val serverUrl = preferences.serverUrl.first()
            val model = preferences.model.first()

            val prompt = """
                ${OpenAIPromptGenerator.createSystemPrompt(OpenAIPromptGenerator.PromptType.SUMMARY, contentType)}

                ${OpenAIPromptGenerator.createUserPrompt(OpenAIPromptGenerator.PromptType.SUMMARY, text)}
            """.trimIndent()

            val requestBody = OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false
            )

            val request = Request.Builder()
                .url("$serverUrl/api/generate")
                .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw SummarizationException.ApiError("Ollama error: ${response.code}")
            }

            val responseBody = response.body?.string()
                ?: throw SummarizationException.InvalidResponse("Empty response")

            val ollamaResponse = gson.fromJson(responseBody, OllamaResponse::class.java)
            ollamaResponse.response

        } catch (e: Exception) {
            throw SummarizationException.ApiError("Ollama error: ${e.message}", e)
        }
    }

    override suspend fun extractTasks(text: String): List<Task> = emptyList()
    override suspend fun extractReminders(text: String): List<Reminder> = emptyList()
    override suspend fun extractTitles(text: String): List<TitleSuggestion> = emptyList()
    override suspend fun classifyContent(text: String): ContentType = ContentType.GENERAL

    override suspend fun processComplete(text: String): SummarizationResult {
        val startTime = System.currentTimeMillis()
        val summary = generateSummary(text, ContentType.GENERAL)
        val processingTime = (System.currentTimeMillis() - startTime) / 1000.0

        return SummarizationResult(
            summary = summary,
            tasks = emptyList(),
            reminders = emptyList(),
            titles = listOf(TitleSuggestion("Ollama Summary", 0.8)),
            contentType = ContentType.GENERAL,
            aiEngine = AIEngine.OLLAMA,
            processingTime = processingTime
        )
    }
}

data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false
)

data class OllamaResponse(
    val model: String,
    val created_at: String,
    val response: String,
    val done: Boolean
)
