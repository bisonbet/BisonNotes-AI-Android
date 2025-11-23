package com.bisonnotesai.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for BisonNotes AI
 * Initializes Hilt dependency injection
 */
@HiltAndroidApp
class BisonNotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
}
