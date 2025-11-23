package com.bisonnotesai.android.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bisonnotesai.android.data.transcription.whisper.WhisperProtocol
import com.bisonnotesai.android.ui.viewmodel.WhisperSettingsViewModel

/**
 * Local Whisper Server Settings Screen
 * Configuration for self-hosted Whisper server (privacy-focused)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhisperSettingsScreen(
    viewModel: WhisperSettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showProtocolPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Whisper Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
        ) {
            // Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy-Focused Transcription",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Connect to your self-hosted Whisper server for completely private transcription. Your audio never leaves your local network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Server Configuration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Server Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.serverURL,
                        onValueChange = viewModel::updateServerURL,
                        label = { Text("Server URL") },
                        placeholder = { Text("http://localhost or http://192.168.1.100") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Computer, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.port.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { viewModel.updatePort(it) }
                        },
                        label = { Text("Port") },
                        placeholder = { Text("9000 for REST, 10300 for Wyoming") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Protocol Picker
                    OutlinedTextField(
                        value = uiState.selectedProtocol.protocolName,
                        onValueChange = {},
                        label = { Text("Protocol") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showProtocolPicker = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Select protocol")
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Api, contentDescription = null)
                        }
                    )

                    if (uiState.selectedProtocol == WhisperProtocol.WYOMING) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wyoming protocol support is planned for a future update. Please use REST protocol for now.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Example: http://localhost:9000 for REST API",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Advanced Options
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Advanced Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Word Timestamps",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Include timing for each word",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.enableWordTimestamps,
                            onCheckedChange = viewModel::updateEnableWordTimestamps
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Speaker Diarization",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Identify different speakers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.enableSpeakerDiarization,
                            onCheckedChange = viewModel::updateEnableSpeakerDiarization
                        )
                    }

                    if (uiState.enableSpeakerDiarization) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.minSpeakers?.toString() ?: "",
                                onValueChange = { value ->
                                    viewModel.updateMinSpeakers(value.toIntOrNull())
                                },
                                label = { Text("Min Speakers") },
                                placeholder = { Text("Auto") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = uiState.maxSpeakers?.toString() ?: "",
                                onValueChange = { value ->
                                    viewModel.updateMaxSpeakers(value.toIntOrNull())
                                },
                                label = { Text("Max Speakers") },
                                placeholder = { Text("Auto") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Connection Test
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Connection Test",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = viewModel::testConnection,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isTestingConnection
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Connection")
                        }
                    }

                    if (uiState.connectionTestResult.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isConnectionSuccessful) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (uiState.isConnectionSuccessful) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (uiState.isConnectionSuccessful) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.connectionTestResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isConnectionSuccessful) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Setup Instructions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Setup Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InstructionRow(
                        number = "1",
                        title = "Install Whisper Server",
                        description = "Set up a local Whisper server using faster-whisper-server or wyoming-faster-whisper"
                    )

                    InstructionRow(
                        number = "2",
                        title = "Start Server",
                        description = "Run the server on your local machine or network"
                    )

                    InstructionRow(
                        number = "3",
                        title = "Configure Connection",
                        description = "Enter server URL and port (default: localhost:9000)"
                    )

                    InstructionRow(
                        number = "4",
                        title = "Test Connection",
                        description = "Verify the server is reachable"
                    )

                    InstructionRow(
                        number = "5",
                        title = "Start Transcribing",
                        description = "Enjoy private, local transcription!"
                    )
                }
            }

            // Reset Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    TextButton(
                        onClick = viewModel::resetToDefaults,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset to Defaults")
                    }
                }
            }
        }
    }

    // Protocol Picker Dialog
    if (showProtocolPicker) {
        AlertDialog(
            onDismissRequest = { showProtocolPicker = false },
            title = { Text("Select Protocol") },
            text = {
                Column {
                    WhisperProtocol.values().forEach { protocol ->
                        Card(
                            onClick = {
                                viewModel.updateProtocol(protocol)
                                showProtocolPicker = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.selectedProtocol == protocol) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedProtocol == protocol,
                                    onClick = {
                                        viewModel.updateProtocol(protocol)
                                        showProtocolPicker = false
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = protocol.protocolName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = when (protocol) {
                                            WhisperProtocol.REST -> "HTTP multipart uploads (Port 9000)"
                                            WhisperProtocol.WYOMING -> "WebSocket streaming (Port 10300) - Coming soon"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProtocolPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}
