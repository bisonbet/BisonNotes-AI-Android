package com.bisonnotesai.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the app
 */
sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Recordings : Screen("recordings", "Recordings", Icons.Default.Mic)
    object Transcripts : Screen("transcripts", "Transcripts", Icons.Default.Article)

    companion object {
        val bottomNavItems = listOf(Recordings, Transcripts)
    }
}
