package com.bisonnotesai.android.data.summarization.bedrock

/**
 * AWS Bedrock Claude models
 */
enum class ClaudeModel(
    val modelId: String,
    val displayName: String,
    val contextWindow: Int
) {
    CLAUDE_3_5_SONNET(
        "anthropic.claude-3-5-sonnet-20241022-v2:0",
        "Claude 3.5 Sonnet v2",
        200000
    ),
    CLAUDE_3_OPUS(
        "anthropic.claude-3-opus-20240229-v1:0",
        "Claude 3 Opus",
        200000
    ),
    CLAUDE_3_SONNET(
        "anthropic.claude-3-sonnet-20240229-v1:0",
        "Claude 3 Sonnet",
        200000
    ),
    CLAUDE_3_HAIKU(
        "anthropic.claude-3-haiku-20240307-v1:0",
        "Claude 3 Haiku",
        200000
    );

    companion object {
        fun fromString(value: String?): ClaudeModel {
            return when (value) {
                CLAUDE_3_5_SONNET.modelId -> CLAUDE_3_5_SONNET
                CLAUDE_3_OPUS.modelId -> CLAUDE_3_OPUS
                CLAUDE_3_SONNET.modelId -> CLAUDE_3_SONNET
                CLAUDE_3_HAIKU.modelId -> CLAUDE_3_HAIKU
                else -> CLAUDE_3_5_SONNET // Default
            }
        }
    }
}

/**
 * Claude API request body
 */
data class ClaudeRequest(
    val anthropic_version: String = "bedrock-2023-05-31",
    val max_tokens: Int,
    val messages: List<ClaudeMessage>,
    val system: String? = null,
    val temperature: Double = 0.1
)

/**
 * Claude message
 */
data class ClaudeMessage(
    val role: String, // "user" or "assistant"
    val content: String
)

/**
 * Claude API response
 */
data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stop_reason: String?,
    val usage: Usage?
)

/**
 * Content block in response
 */
data class ContentBlock(
    val type: String, // "text"
    val text: String
)

/**
 * Usage information
 */
data class Usage(
    val input_tokens: Int,
    val output_tokens: Int
)
