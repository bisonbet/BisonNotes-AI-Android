package com.bisonnotesai.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bisonnotesai.android.ui.screen.RecordingsScreen
import com.bisonnotesai.android.ui.theme.BisonNotesTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for BisonNotes AI
 * Handles permission requests and hosts the main UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var hasRecordPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(true)

    // Permission launcher for audio recording
    private val recordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
    }

    // Permission launcher for notifications (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check permissions
        checkPermissions()

        setContent {
            BisonNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show permission prompt or main screen
                    if (hasRecordPermission) {
                        RecordingsScreen()
                    } else {
                        PermissionScreen(
                            onRequestPermission = {
                                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        // Check audio recording permission
        hasRecordPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // Request if not granted
        if (!hasRecordPermission) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * Screen shown when permissions are not granted
 */
@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Mic,
                contentDescription = "Microphone",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            androidx.compose.material3.Text(
                text = "Microphone Access Required",
                style = MaterialTheme.typography.titleLarge
            )
            androidx.compose.material3.Text(
                text = "BisonNotes AI needs access to your microphone to record audio",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            androidx.compose.material3.Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                androidx.compose.material3.Text("Grant Permission")
            }
        }
    }
}
