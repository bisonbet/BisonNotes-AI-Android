package com.bisonnotesai.android.data.summarization.gemini

import android.util.Log
import com.bisonnotesai.android.data.preferences.GeminiPreferences
import com.bisonnotesai.android.data.summarization.openai.OpenAIPromptGenerator
import com.bisonnotesai.android.domain.model.*
import com.bisonnotesai.android.summarization.SummarizationException
import com.bisonnotesai.android.summarization.SummarizationResult
import com.bisonnotesai.android.summarization.SummarizationService
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Gemini summarization service
 */
@Singleton
class GeminiService @Inject constructor(
    private val preferences: GeminiPreferences
) : SummarizationService {

    override val name: String = "Google Gemini"
    override val description: String = "Google's Gemini AI for summarization"

    companion object {
        private const val TAG = "GeminiService"
    }

    private suspend fun getModel(): GenerativeModel {
        val apiKey = preferences.apiKey.first()
        val modelName = preferences.model.first()

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
        )

        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            safetySettings = safetySettings
        )
    }

    override suspend fun isAvailable(): Boolean {
        val enabled = preferences.enabled.first()
        val apiKey = preferences.apiKey.first()
        return enabled && apiKey.isNotEmpty()
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val model = getModel()
            val response = model.generateContent("Test")
            response.text != null
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }

    override suspend fun generateSummary(text: String, contentType: ContentType): String {
        return try {
            val model = getModel()
            val prompt = """
                ${OpenAIPromptGenerator.createSystemPrompt(OpenAIPromptGenerator.PromptType.SUMMARY, contentType)}

                ${OpenAIPromptGenerator.createUserPrompt(OpenAIPromptGenerator.PromptType.SUMMARY, text)}
            """.trimIndent()

            val response = model.generateContent(prompt)
            response.text ?: throw SummarizationException.InvalidResponse("Empty response from Gemini")
        } catch (e: Exception) {
            throw SummarizationException.ApiError("Gemini error: ${e.message}", e)
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
            titles = listOf(TitleSuggestion("Gemini Summary", 0.8)),
            contentType = ContentType.GENERAL,
            aiEngine = AIEngine.GEMINI_PRO,
            processingTime = processingTime
        )
    }
}
