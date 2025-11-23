package com.bisonnotesai.android.ui.screen

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisonnotesai.android.ui.viewmodel.TranscriptWithRecording
import com.bisonnotesai.android.ui.viewmodel.TranscriptsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Detail screen showing full transcript with export options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptDetailScreen(
    transcriptWithRecording: TranscriptWithRecording,
    onBackClick: () -> Unit,
    viewModel: TranscriptsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    val transcript = transcriptWithRecording.transcript
    val recording = transcriptWithRecording.recording

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = recording.displayName(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Transcript",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Share, "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Transcript metadata card
            TranscriptMetadataCard(transcriptWithRecording)

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Full transcript text
            TranscriptContent(transcript)
        }
    }

    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportText = {
                val text = viewModel.exportAsText(transcript)
                shareText(context, text, "transcript.txt")
                showExportDialog = false
            },
            onExportMarkdown = {
                val markdown = viewModel.exportAsMarkdown(transcript, recording.name)
                shareText(context, markdown, "transcript.md")
                showExportDialog = false
            }
        )
    }
}

/**
 * Transcript metadata card
 */
@Composable
fun TranscriptMetadataCard(transcriptWithRecording: TranscriptWithRecording) {
    val transcript = transcriptWithRecording.transcript
    val recording = transcriptWithRecording.recording

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            MetadataRow("Engine", transcript.engine.toDisplayString())
            MetadataRow("Confidence", "${(transcript.confidence * 100).toInt()}%")
            MetadataRow("Word Count", "${transcript.wordCount()} words")
            MetadataRow("Duration", recording.formattedDuration())
            MetadataRow(
                "Created",
                SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                    .format(transcript.createdAt)
            )

            if (transcript.speakerMappings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Speakers",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                transcript.speakerMappings.forEach { (id, name) ->
                    Text(
                        text = "• $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Metadata row
 */
@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Transcript content with speaker labels
 */
@Composable
fun TranscriptContent(transcript: com.bisonnotesai.android.domain.model.Transcript) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Transcript",
            style = MaterialTheme.typography.titleMedium
        )

        transcript.segments.forEach { segment ->
            val speaker = segment.speaker?.let {
                transcript.speakerMappings[it] ?: "Speaker $it"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Speaker label and timestamp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (speaker != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = speaker,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = segment.formattedTimestamp(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Segment text
                    Text(
                        text = segment.text,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Confidence if available
                    segment.confidence?.let { confidence ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Confidence: ${(confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Export dialog with format options
 */
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExportText: () -> Unit,
    onExportMarkdown: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Transcript") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose export format:")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = onExportText
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TextFields, "Plain text")
                        Column {
                            Text(
                                "Plain Text",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Simple text format (.txt)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = onExportMarkdown
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Article, "Markdown")
                        Column {
                            Text(
                                "Markdown",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Formatted markdown (.md)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Share text via Android share intent
 */
private fun shareText(context: Context, text: String, filename: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, filename)
    }

    val shareIntent = Intent.createChooser(sendIntent, "Export transcript")
    context.startActivity(shareIntent)
}
