package com.bisonnotesai.android.di

import com.bisonnotesai.android.data.preferences.OpenAISummarizationPreferences
import com.bisonnotesai.android.data.summarization.openai.OpenAISummarizationApi
import com.bisonnotesai.android.data.summarization.openai.OpenAISummarizationService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for AI summarization services
 */
@Module
@InstallIn(SingletonComponent::class)
object SummarizationModule {

    @Provides
    @Singleton
    @Named("openai_summarization")
    fun provideOpenAISummarizationOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAISummarizationApi(
        @Named("openai_summarization") okHttpClient: OkHttpClient,
        gson: Gson,
        preferences: OpenAISummarizationPreferences
    ): OpenAISummarizationApi {
        // Get base URL from preferences
        val baseUrl = runBlocking {
            preferences.baseUrl.first().let { url ->
                if (url.endsWith("/")) url else "$url/"
            }
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OpenAISummarizationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenAISummarizationService(
        api: OpenAISummarizationApi,
        preferences: OpenAISummarizationPreferences,
        gson: Gson
    ): OpenAISummarizationService {
        return OpenAISummarizationService(api, preferences, gson)
    }
}
