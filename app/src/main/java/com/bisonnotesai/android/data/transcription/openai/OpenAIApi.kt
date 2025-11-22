package com.bisonnotesai.android.data.transcription.openai

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for OpenAI API
 */
interface OpenAIApi {

    /**
     * Test connection by fetching models list
     */
    @GET("models")
    suspend fun getModels(): Response<OpenAIModelsListResponse>

    /**
     * Transcribe audio file using OpenAI Whisper
     * https://platform.openai.com/docs/api-reference/audio/createTranscription
     */
    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") responseFormat: RequestBody,
        @Part("language") language: RequestBody? = null,
        @Part("temperature") temperature: RequestBody? = null
    ): Response<OpenAITranscribeResponse>
}
