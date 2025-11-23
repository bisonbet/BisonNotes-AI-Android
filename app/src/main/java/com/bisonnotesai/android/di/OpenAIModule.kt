package com.bisonnotesai.android.di

import android.content.Context
import com.bisonnotesai.android.data.preferences.OpenAIPreferences
import com.bisonnotesai.android.data.transcription.openai.OpenAIApi
import com.bisonnotesai.android.data.transcription.openai.OpenAIConfig
import com.bisonnotesai.android.data.transcription.openai.OpenAIWhisperEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Dagger Hilt module for OpenAI dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object OpenAIModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OpenAIRetrofit

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class OpenAIOkHttp

    /**
     * Provide OpenAI preferences
     */
    @Provides
    @Singleton
    fun provideOpenAIPreferences(@ApplicationContext context: Context): OpenAIPreferences {
        return OpenAIPreferences(context)
    }

    /**
     * Provide OpenAI configuration from preferences
     */
    @Provides
    @Singleton
    fun provideOpenAIConfig(preferences: OpenAIPreferences): OpenAIConfig {
        // Get config from preferences synchronously for DI
        // In actual use, the config flow should be collected
        return runBlocking {
            preferences.config.first()
        }
    }

    /**
     * Provide auth interceptor for OpenAI API
     */
    @Provides
    @Singleton
    fun provideOpenAIAuthInterceptor(config: OpenAIConfig): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "multipart/form-data")
                .build()
            chain.proceed(request)
        }
    }

    /**
     * Provide logging interceptor for debugging
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    /**
     * Provide OkHttp client for OpenAI with custom timeout
     */
    @Provides
    @Singleton
    @OpenAIOkHttp
    fun provideOpenAIOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        config: OpenAIConfig
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(config.timeout, TimeUnit.MILLISECONDS) // 30 minutes for long audio files
            .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Provide Retrofit instance for OpenAI
     */
    @Provides
    @Singleton
    @OpenAIRetrofit
    fun provideOpenAIRetrofit(
        @OpenAIOkHttp okHttpClient: OkHttpClient,
        config: OpenAIConfig
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(config.baseURL + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provide OpenAI API interface
     */
    @Provides
    @Singleton
    fun provideOpenAIApi(@OpenAIRetrofit retrofit: Retrofit): OpenAIApi {
        return retrofit.create(OpenAIApi::class.java)
    }

    /**
     * Provide OpenAI Whisper Engine
     */
    @Provides
    @Singleton
    fun provideOpenAIWhisperEngine(
        api: OpenAIApi,
        config: OpenAIConfig
    ): OpenAIWhisperEngine {
        return OpenAIWhisperEngine(api, config)
    }
}
