package com.bisonnotesai.android.di

import android.content.Context
import com.bisonnotesai.android.data.preferences.AWSPreferences
import com.bisonnotesai.android.data.transcription.aws.AWSTranscribeConfig
import com.bisonnotesai.android.data.transcription.aws.AWSTranscribeEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * Dagger Hilt module for AWS dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AWSModule {

    /**
     * Provide AWS preferences
     */
    @Provides
    @Singleton
    fun provideAWSPreferences(@ApplicationContext context: Context): AWSPreferences {
        return AWSPreferences(context)
    }

    /**
     * Provide AWS Transcribe configuration from preferences
     */
    @Provides
    @Singleton
    fun provideAWSTranscribeConfig(preferences: AWSPreferences): AWSTranscribeConfig {
        // Get config from preferences synchronously for DI
        // In actual use, the config flow should be collected
        return runBlocking {
            preferences.config.first()
        }
    }

    /**
     * Provide AWS Transcribe Engine
     */
    @Provides
    @Singleton
    fun provideAWSTranscribeEngine(config: AWSTranscribeConfig): AWSTranscribeEngine {
        return AWSTranscribeEngine(config)
    }
}
