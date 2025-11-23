package com.bisonnotesai.android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bisonnotesai.android.domain.model.Summary
import com.bisonnotesai.android.domain.model.Task
import com.bisonnotesai.android.domain.model.Reminder
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryDetailScreen(
    summary: Summary,
    onBack: () -> Unit,
    onExport: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(summary.bestTitle()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.Download, "Export")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metadata card
            MetadataCard(summary)

            // Markdown summary content
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RichText(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Markdown(summary.text)
                    }
                }
            }

            // Tasks section
            if (summary.tasks.isNotEmpty()) {
                TasksCard(summary.tasks)
            }

            // Reminders section
            if (summary.reminders.isNotEmpty()) {
                RemindersCard(summary.reminders)
            }

            // Alternative titles
            if (summary.titles.size > 1) {
                AlternativeTitlesCard(summary.titles.drop(1))
            }
        }
    }
}

@Composable
private fun MetadataCard(summary: Summary) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MetadataRow("AI Engine", summary.aiEngine.toDisplayString())
            MetadataRow("Content Type", summary.contentType.toDisplayString())
            MetadataRow("Generated", formatDateTime(summary.generatedAt))
            MetadataRow("Processing Time", String.format("%.2f seconds", summary.processingTime))
            MetadataRow("Confidence", String.format("%.1f%%", summary.confidence * 100))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TasksCard(tasks: List<Task>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Text(
                    "Tasks (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            tasks.forEach { task ->
                TaskItem(task)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TaskItem(task: Task) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Circle,
            contentDescription = null,
            modifier = Modifier.size(8.dp).padding(top = 6.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.text,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(task.priority.name) }
                )
                task.assignee?.let { assignee ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(assignee) }
                    )
                }
                task.dueDate?.let { date ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(formatDate(date)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RemindersCard(reminders: List<Reminder>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Text(
                    "Reminders (${reminders.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            reminders.forEach { reminder ->
                ReminderItem(reminder)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReminderItem(reminder: Reminder) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                reminder.text,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(reminder.importance.name) }
                )
                reminder.date?.let { date ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(formatDateTime(date)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlternativeTitlesCard(titles: List<com.bisonnotesai.android.domain.model.TitleSuggestion>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Alternative Titles",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            titles.forEach { title ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        title.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        String.format("%.1f%%", title.confidence * 100),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(date)
}

private fun formatDateTime(date: Date): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(date)
}
