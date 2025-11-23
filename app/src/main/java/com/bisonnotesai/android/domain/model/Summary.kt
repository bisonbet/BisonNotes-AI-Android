package com.bisonnotesai.android.domain.model

import java.util.Date

/**
 * Domain model for an AI-generated summary
 * Business logic representation
 *
 * Maps to SummaryEntity in data layer
 */
data class Summary(
    val id: String,
    val recordingId: String,
    val transcriptId: String?,
    val text: String,
    val titles: List<TitleSuggestion>,
    val tasks: List<Task>,
    val reminders: List<Reminder>,
    val contentType: ContentType,
    val aiEngine: AIEngine,
    val confidence: Double, // 0.0 to 1.0
    val processingTime: Double, // in seconds
    val statistics: SummaryStatistics,
    val version: Int, // for regeneration tracking
    val generatedAt: Date
) {
    /**
     * Get the best (highest confidence) title
     */
    fun bestTitle(): String {
        return titles.maxByOrNull { it.confidence }?.text ?: "Summary"
    }

    /**
     * Get high priority tasks
     */
    fun highPriorityTasks(): List<Task> {
        return tasks.filter { it.priority == TaskPriority.HIGH }
    }

    /**
     * Get upcoming reminders (not in the past)
     */
    fun upcomingReminders(): List<Reminder> {
        val now = Date()
        return reminders.filter { it.date?.after(now) == true }
    }

    /**
     * Check if summary has actionable items
     */
    fun hasActionableItems(): Boolean {
        return tasks.isNotEmpty() || reminders.isNotEmpty()
    }
}

/**
 * Title suggestion with confidence score
 */
data class TitleSuggestion(
    val text: String,
    val confidence: Double
)

/**
 * Extracted task from summary
 */
data class Task(
    val text: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assignee: String? = null,
    val dueDate: Date? = null
)

/**
 * Task priority levels
 */
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    companion object {
        fun fromString(value: String?): TaskPriority {
            return when (value?.lowercase()) {
                "low" -> LOW
                "medium" -> MEDIUM
                "high" -> HIGH
                "urgent" -> URGENT
                else -> MEDIUM
            }
        }
    }
}

/**
 * Extracted reminder from summary
 */
data class Reminder(
    val text: String,
    val date: Date?,
    val importance: ReminderImportance = ReminderImportance.MEDIUM
)

/**
 * Reminder importance levels
 */
enum class ReminderImportance {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun fromString(value: String?): ReminderImportance {
            return when (value?.lowercase()) {
                "low" -> LOW
                "medium" -> MEDIUM
                "high" -> HIGH
                else -> MEDIUM
            }
        }
    }
}

/**
 * Content type classification
 */
enum class ContentType {
    MEETING,
    LECTURE,
    INTERVIEW,
    CONVERSATION,
    PRESENTATION,
    GENERAL;

    companion object {
        fun fromString(value: String?): ContentType {
            return when (value?.lowercase()) {
                "meeting" -> MEETING
                "lecture" -> LECTURE
                "interview" -> INTERVIEW
                "conversation" -> CONVERSATION
                "presentation" -> PRESENTATION
                else -> GENERAL
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            MEETING -> "Meeting"
            LECTURE -> "Lecture"
            INTERVIEW -> "Interview"
            CONVERSATION -> "Conversation"
            PRESENTATION -> "Presentation"
            GENERAL -> "General"
        }
    }
}

/**
 * AI engine used for summarization
 */
enum class AIEngine {
    OPENAI_GPT4,
    OPENAI_GPT35,
    CLAUDE_OPUS,
    CLAUDE_SONNET,
    CLAUDE_HAIKU,
    GEMINI_PRO,
    GEMINI_ULTRA,
    OLLAMA,
    CUSTOM;

    companion object {
        fun fromString(value: String?): AIEngine {
            return when (value?.lowercase()) {
                "openai", "gpt-4", "gpt4" -> OPENAI_GPT4
                "gpt-3.5", "gpt35" -> OPENAI_GPT35
                "claude-opus", "opus" -> CLAUDE_OPUS
                "claude-sonnet", "sonnet", "claude" -> CLAUDE_SONNET
                "claude-haiku", "haiku" -> CLAUDE_HAIKU
                "gemini-pro", "gemini" -> GEMINI_PRO
                "gemini-ultra" -> GEMINI_ULTRA
                "ollama" -> OLLAMA
                else -> CUSTOM
            }
        }
    }

    fun toDisplayString(): String {
        return when (this) {
            OPENAI_GPT4 -> "GPT-4"
            OPENAI_GPT35 -> "GPT-3.5"
            CLAUDE_OPUS -> "Claude Opus"
            CLAUDE_SONNET -> "Claude Sonnet"
            CLAUDE_HAIKU -> "Claude Haiku"
            GEMINI_PRO -> "Gemini Pro"
            GEMINI_ULTRA -> "Gemini Ultra"
            OLLAMA -> "Ollama"
            CUSTOM -> "Custom"
        }
    }
}

/**
 * Summary statistics
 */
data class SummaryStatistics(
    val originalLength: Int,      // character count of original transcript
    val wordCount: Int,            // word count in summary
    val compressionRatio: Double   // ratio of summary to original
) {
    /**
     * Get compression percentage
     */
    fun compressionPercentage(): Int {
        return (compressionRatio * 100).toInt()
    }
}
