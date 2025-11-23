package com.bisonnotesai.android.data.transcription.whisper

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for local Whisper server REST API
 */
interface WhisperApi {

    /**
     * Test connection by checking ASR endpoint
     */
    @GET("asr")
    suspend fun testConnection(): Response<Unit>

    /**
     * Transcribe audio file using Whisper
     */
    @Multipart
    @POST("asr")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Query("output") output: String = "json",
        @Query("task") task: String = "transcribe",
        @Query("language") language: String? = null,
        @Query("word_timestamps") wordTimestamps: Boolean? = false,
        @Query("vad_filter") vadFilter: Boolean? = false,
        @Query("encode") encode: Boolean? = true,
        @Query("diarize") diarize: Boolean? = false,
        @Query("min_speakers") minSpeakers: Int? = null,
        @Query("max_speakers") maxSpeakers: Int? = null
    ): Response<WhisperTranscribeResponse>

    /**
     * Detect language of audio file
     */
    @Multipart
    @POST("detect-language")
    suspend fun detectLanguage(
        @Part file: MultipartBody.Part
    ): Response<LanguageDetectionResponse>
}
