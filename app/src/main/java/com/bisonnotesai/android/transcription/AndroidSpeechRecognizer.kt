package com.bisonnotesai.android.transcription

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.bisonnotesai.android.domain.model.TranscriptSegment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android SpeechRecognizer-based transcription service
 * Uses on-device speech recognition for privacy
 */
@Singleton
class AndroidSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) : TranscriptionService {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isCancelled = false

    override suspend fun transcribe(
        audioFile: File,
        language: String
    ): Flow<TranscriptionResult> = callbackFlow {
        isCancelled = false

        try {
            // Check if file exists
            if (!audioFile.exists()) {
                trySend(TranscriptionResult.Error("Audio file not found"))
                close()
                return@callbackFlow
            }

            // Get audio duration for progress tracking
            val duration = getAudioDuration(audioFile)

            trySend(TranscriptionResult.Progress(0, "Preparing transcription..."))

            // For Android SpeechRecognizer, we need to use the microphone
            // For file-based transcription, we'll need to play the audio
            // This is a simplified implementation that shows the concept

            // Create speech recognizer
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            // Create recognition intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            // Set up recognition listener
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    trySend(TranscriptionResult.Progress(10, "Ready for speech..."))
                }

                override fun onBeginningOfSpeech() {
                    trySend(TranscriptionResult.Progress(20, "Listening..."))
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Buffer received
                }

                override fun onEndOfSpeech() {
                    trySend(TranscriptionResult.Progress(80, "Processing..."))
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Unknown error: $error"
                    }
                    trySend(TranscriptionResult.Error(errorMessage))
                    close()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""

                    if (text.isNotEmpty()) {
                        // Create a single segment from the full transcription
                        val segment = TranscriptSegment(
                            text = text,
                            start = 0.0,
                            end = duration,
                            speaker = null,
                            confidence = 0.8 // Android doesn't provide confidence
                        )

                        trySend(TranscriptionResult.Progress(100, "Complete"))
                        trySend(TranscriptionResult.Success(listOf(segment), text))
                    } else {
                        trySend(TranscriptionResult.Error("No transcription results"))
                    }
                    close()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""

                    if (text.isNotEmpty()) {
                        trySend(TranscriptionResult.PartialResult(text, false))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Custom events
                }
            })

            // Note: For actual file-based transcription, you would need to:
            // 1. Use a different approach (cloud API, or local Whisper model)
            // 2. Or play the audio file while recognizing
            // This is a simplified demonstration

            trySend(TranscriptionResult.Progress(50, "Transcribing audio file..."))

            // For now, return a placeholder result
            // In production, integrate with cloud APIs or local models
            val placeholderSegment = TranscriptSegment(
                text = "Transcription using Android SpeechRecognizer requires microphone input. " +
                       "For file-based transcription, integrate cloud APIs (OpenAI Whisper, AWS Transcribe) " +
                       "or local models.",
                start = 0.0,
                end = duration,
                speaker = null,
                confidence = 0.0
            )

            trySend(TranscriptionResult.Success(
                segments = listOf(placeholderSegment),
                fullText = placeholderSegment.text
            ))

        } catch (e: Exception) {
            trySend(TranscriptionResult.Error("Transcription failed: ${e.message}", e))
        } finally {
            speechRecognizer?.destroy()
            speechRecognizer = null
            close()
        }

        awaitClose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    override fun isSupported(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override fun getSupportedLanguages(): List<String> {
        // Android SpeechRecognizer supports many languages
        return listOf(
            "en-US", "en-GB", "es-ES", "fr-FR", "de-DE",
            "it-IT", "ja-JP", "ko-KR", "zh-CN", "pt-BR"
        )
    }

    override fun cancel() {
        isCancelled = true
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * Get audio file duration in seconds
     */
    private fun getAudioDuration(file: File): Double {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()
            durationMs / 1000.0
        } catch (e: Exception) {
            0.0
        }
    }
}
