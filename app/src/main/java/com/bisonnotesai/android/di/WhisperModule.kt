package com.bisonnotesai.android.di

import android.content.Context
import com.bisonnotesai.android.data.preferences.WhisperPreferences
import com.bisonnotesai.android.data.transcription.whisper.LocalWhisperEngine
import com.bisonnotesai.android.data.transcription.whisper.WhisperApi
import com.bisonnotesai.android.data.transcription.whisper.WhisperConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dagger Hilt module for Local Whisper Server dependencies
 * Provides privacy-focused local transcription
 */
@Module
@InstallIn(SingletonComponent::class)
object WhisperModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class WhisperRetrofit

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class WhisperOkHttp

    /**
     * Provide Whisper preferences
     */
    @Provides
    @Singleton
    fun provideWhisperPreferences(@ApplicationContext context: Context): WhisperPreferences {
        return WhisperPreferences(context)
    }

    /**
     * Provide Whisper configuration from preferences
     */
    @Provides
    @Singleton
    fun provideWhisperConfig(preferences: WhisperPreferences): WhisperConfig {
        // Get config from preferences synchronously for DI
        // In actual use, the config flow should be collected for dynamic updates
        return runBlocking {
            preferences.config.first()
        }
    }

    /**
     * Provide logging interceptor for debugging
     */
    @Provides
    @Singleton
    fun provideWhisperLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Provide OkHttp client for local Whisper server
     * No authentication needed - local server only
     */
    @Provides
    @Singleton
    @WhisperOkHttp
    fun provideWhisperOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES) // Generous timeout for large audio files
            .writeTimeout(5, TimeUnit.MINUTES) // Upload can take time
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provide Retrofit instance for local Whisper server
     * Base URL is configured from WhisperConfig (dynamic server address)
     */
    @Provides
    @Singleton
    @WhisperRetrofit
    fun provideWhisperRetrofit(
        @WhisperOkHttp okHttpClient: OkHttpClient,
        config: WhisperConfig
    ): Retrofit {
        // Ensure base URL ends with /
        val baseUrl = config.restAPIBaseURL.let {
            if (it.endsWith("/")) it else "$it/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provide Whisper API interface
     */
    @Provides
    @Singleton
    fun provideWhisperApi(@WhisperRetrofit retrofit: Retrofit): WhisperApi {
        return retrofit.create(WhisperApi::class.java)
    }

    /**
     * Provide Local Whisper Engine
     * Privacy-focused transcription using self-hosted Whisper server
     */
    @Provides
    @Singleton
    fun provideLocalWhisperEngine(
        api: WhisperApi,
        config: WhisperConfig
    ): LocalWhisperEngine {
        return LocalWhisperEngine(api, config)
    }
}
