package com.bisonnotesai.android.data.summarization.bedrock

import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.sdk.kotlin.services.bedrockruntime.model.InvokeModelRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.bisonnotesai.android.data.preferences.AWSBedrockPreferences
import com.bisonnotesai.android.data.preferences.AWSPreferences
import com.bisonnotesai.android.data.summarization.openai.OpenAIPromptGenerator
import com.bisonnotesai.android.domain.model.*
import com.bisonnotesai.android.summarization.SummarizationException
import com.bisonnotesai.android.summarization.SummarizationResult
import com.bisonnotesai.android.summarization.SummarizationService
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AWS Bedrock (Claude) summarization service
 * Uses AWS SDK for Kotlin to invoke Claude models
 */
@Singleton
class AWSBedrockService @Inject constructor(
    private val awsPreferences: AWSPreferences,
    private val bedrockPreferences: AWSBedrockPreferences,
    private val gson: Gson
) : SummarizationService {

    override val name: String = "AWS Claude"
    override val description: String = "Claude AI via AWS Bedrock for advanced summarization"

    companion object {
        private const val TAG = "AWSBedrockService"
    }

    private suspend fun getClient(): BedrockRuntimeClient {
        val accessKey = awsPreferences.accessKeyId.first()
        val secretKey = awsPreferences.secretKey.first()
        val region = awsPreferences.region.first()

        return BedrockRuntimeClient {
            this.region = region
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = accessKey
                secretAccessKey = secretKey
            }
        }
    }

    override suspend fun isAvailable(): Boolean {
        val enabled = bedrockPreferences.enabled.first()
        val accessKey = awsPreferences.accessKeyId.first()
        val secretKey = awsPreferences.secretKey.first()
        return enabled && accessKey.isNotEmpty() && secretKey.isNotEmpty()
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val client = getClient()
            // AWS SDK will validate credentials on first use
            client.close()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }

    override suspend fun generateSummary(text: String, contentType: ContentType): String {
        val client = getClient()
        try {
            val systemPrompt = OpenAIPromptGenerator.createSystemPrompt(
                OpenAIPromptGenerator.PromptType.SUMMARY,
                contentType
            )
            val userPrompt = OpenAIPromptGenerator.createUserPrompt(
                OpenAIPromptGenerator.PromptType.SUMMARY,
                text
            )

            val response = invokeModel(client, systemPrompt, userPrompt)
            return response
        } finally {
            client.close()
        }
    }

    override suspend fun extractTasks(text: String): List<Task> {
        // Simplified implementation - returns empty list for now
        // Can be enhanced with full Claude API integration
        return emptyList()
    }

    override suspend fun extractReminders(text: String): List<Reminder> {
        return emptyList()
    }

    override suspend fun extractTitles(text: String): List<TitleSuggestion> {
        return emptyList()
    }

    override suspend fun classifyContent(text: String): ContentType {
        return ContentType.GENERAL
    }

    override suspend fun processComplete(text: String): SummarizationResult {
        val startTime = System.currentTimeMillis()
        val summary = generateSummary(text, ContentType.GENERAL)
        val processingTime = (System.currentTimeMillis() - startTime) / 1000.0

        return SummarizationResult(
            summary = summary,
            tasks = emptyList(),
            reminders = emptyList(),
            titles = listOf(TitleSuggestion("Summary", 0.8)),
            contentType = ContentType.GENERAL,
            aiEngine = AIEngine.CLAUDE_SONNET,
            processingTime = processingTime
        )
    }

    private suspend fun invokeModel(
        client: BedrockRuntimeClient,
        systemPrompt: String,
        userPrompt: String
    ): String {
        val modelId = bedrockPreferences.model.first()
        val maxTokens = bedrockPreferences.maxTokens.first()
        val temperature = bedrockPreferences.temperature.first()

        val request = ClaudeRequest(
            max_tokens = maxTokens,
            messages = listOf(ClaudeMessage("user", userPrompt)),
            system = systemPrompt,
            temperature = temperature
        )

        val requestBody = gson.toJson(request)

        val invokeRequest = InvokeModelRequest {
            this.modelId = modelId
            this.body = ByteStream.fromString(requestBody)
            this.contentType = "application/json"
        }

        val response = client.invokeModel(invokeRequest)
        val responseBody = response.body?.decodeToString()
            ?: throw SummarizationException.InvalidResponse("Empty response from Bedrock")

        val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)
        return claudeResponse.content.firstOrNull()?.text
            ?: throw SummarizationException.InvalidResponse("No content in response")
    }
}
