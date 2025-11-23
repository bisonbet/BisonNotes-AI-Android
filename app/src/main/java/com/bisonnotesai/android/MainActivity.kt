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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bisonnotesai.android.ui.navigation.Screen
import com.bisonnotesai.android.ui.screen.RecordingsScreen
import com.bisonnotesai.android.ui.screen.TranscriptDetailScreen
import com.bisonnotesai.android.ui.screen.TranscriptsScreen
import com.bisonnotesai.android.ui.theme.BisonNotesTheme
import com.bisonnotesai.android.ui.viewmodel.TranscriptWithRecording
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for BisonNotes AI
 * Handles permission requests and hosts the main UI with navigation
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
                        MainScreen()
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        val navController = rememberNavController()
        var selectedTranscript by remember { mutableStateOf<TranscriptWithRecording?>(null) }

        Scaffold(
            bottomBar = {
                // Only show bottom nav when not viewing transcript detail
                if (selectedTranscript == null) {
                    NavigationBar {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        Screen.bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Recordings.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Recordings.route) {
                    RecordingsScreen()
                }
                composable(Screen.Transcripts.route) {
                    if (selectedTranscript != null) {
                        TranscriptDetailScreen(
                            transcriptWithRecording = selectedTranscript!!,
                            onBackClick = { selectedTranscript = null }
                        )
                    } else {
                        TranscriptsScreen(
                            onTranscriptClick = { selectedTranscript = it }
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
