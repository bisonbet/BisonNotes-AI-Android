package com.bisonnotesai.android.data.mapper

import com.bisonnotesai.android.data.local.database.entity.SummaryEntity
import com.bisonnotesai.android.domain.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Mapper between SummaryEntity (data layer) and Summary (domain layer)
 */
class SummaryMapper @Inject constructor(
    private val gson: Gson
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    /**
     * Convert SummaryEntity to Summary domain model
     */
    fun toDomain(entity: SummaryEntity): Summary {
        return Summary(
            id = entity.id,
            recordingId = entity.recordingId,
            transcriptId = entity.transcriptId,
            text = entity.summary ?: "",
            titles = parseTitles(entity.titles),
            tasks = parseTasks(entity.tasks),
            reminders = parseReminders(entity.reminders),
            contentType = ContentType.fromString(entity.contentType),
            aiEngine = AIEngine.fromString(entity.aiMethod),
            confidence = entity.confidence,
            processingTime = entity.processingTime,
            statistics = SummaryStatistics(
                originalLength = entity.originalLength,
                wordCount = entity.wordCount,
                compressionRatio = entity.compressionRatio
            ),
            version = entity.version,
            generatedAt = entity.generatedAt
        )
    }

    /**
     * Convert Summary domain model to SummaryEntity
     */
    fun toEntity(domain: Summary): SummaryEntity {
        return SummaryEntity(
            id = domain.id,
            recordingId = domain.recordingId,
            transcriptId = domain.transcriptId,
            summary = domain.text,
            titles = serializeTitles(domain.titles),
            tasks = serializeTasks(domain.tasks),
            reminders = serializeReminders(domain.reminders),
            contentType = domain.contentType.name.lowercase(),
            aiMethod = domain.aiEngine.name.lowercase(),
            confidence = domain.confidence,
            processingTime = domain.processingTime,
            originalLength = domain.statistics.originalLength,
            wordCount = domain.statistics.wordCount,
            compressionRatio = domain.statistics.compressionRatio,
            version = domain.version,
            generatedAt = domain.generatedAt
        )
    }

    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<SummaryEntity>): List<Summary> {
        return entities.map { toDomain(it) }
    }

    /**
     * Parse title suggestions from JSON
     */
    private fun parseTitles(json: String?): List<TitleSuggestion> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<TitleJson>>() {}.type
            val titles: List<TitleJson> = gson.fromJson(json, type)
            titles.map { TitleSuggestion(it.text, it.confidence ?: 0.0) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Serialize title suggestions to JSON
     */
    private fun serializeTitles(titles: List<TitleSuggestion>): String {
        val titleJsonList = titles.map { TitleJson(it.text, it.confidence) }
        return gson.toJson(titleJsonList)
    }

    /**
     * Parse tasks from JSON
     */
    private fun parseTasks(json: String?): List<Task> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<TaskJson>>() {}.type
            val tasks: List<TaskJson> = gson.fromJson(json, type)
            tasks.map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Serialize tasks to JSON
     */
    private fun serializeTasks(tasks: List<Task>): String {
        val taskJsonList = tasks.map { TaskJson.fromDomain(it, dateFormat) }
        return gson.toJson(taskJsonList)
    }

    /**
     * Parse reminders from JSON
     */
    private fun parseReminders(json: String?): List<Reminder> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<ReminderJson>>() {}.type
            val reminders: List<ReminderJson> = gson.fromJson(json, type)
            reminders.map { it.toDomain(dateFormat) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Serialize reminders to JSON
     */
    private fun serializeReminders(reminders: List<Reminder>): String {
        val reminderJsonList = reminders.map { ReminderJson.fromDomain(it, dateFormat) }
        return gson.toJson(reminderJsonList)
    }

    // JSON data classes for serialization
    private data class TitleJson(
        val text: String,
        val confidence: Double?
    )

    private data class TaskJson(
        val text: String,
        val priority: String?,
        val assignee: String?,
        val dueDate: String?
    ) {
        fun toDomain(): Task {
            val dueDateParsed = dueDate?.let {
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(it)
                } catch (e: Exception) {
                    null
                }
            }

            return Task(
                text = text,
                priority = TaskPriority.fromString(priority),
                assignee = assignee,
                dueDate = dueDateParsed
            )
        }

        companion object {
            fun fromDomain(task: Task, dateFormat: SimpleDateFormat): TaskJson {
                return TaskJson(
                    text = task.text,
                    priority = task.priority.name.lowercase(),
                    assignee = task.assignee,
                    dueDate = task.dueDate?.let { dateFormat.format(it) }
                )
            }
        }
    }

    private data class ReminderJson(
        val text: String,
        val date: String?,
        val importance: String?
    ) {
        fun toDomain(dateFormat: SimpleDateFormat): Reminder {
            val dateParsed = date?.let {
                try {
                    dateFormat.parse(it)
                } catch (e: Exception) {
                    null
                }
            }

            return Reminder(
                text = text,
                date = dateParsed,
                importance = ReminderImportance.fromString(importance)
            )
        }

        companion object {
            fun fromDomain(reminder: Reminder, dateFormat: SimpleDateFormat): ReminderJson {
                return ReminderJson(
                    text = reminder.text,
                    date = reminder.date?.let { dateFormat.format(it) },
                    importance = reminder.importance.name.lowercase()
                )
            }
        }
    }
}
