package com.bisonnotesai.android.transcription

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bisonnotesai.android.domain.model.ProcessingStatus
import com.bisonnotesai.android.domain.model.Transcript
import com.bisonnotesai.android.domain.model.TranscriptionEngine
import com.bisonnotesai.android.domain.repository.RecordingRepository
import com.bisonnotesai.android.domain.repository.TranscriptRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.last
import java.io.File
import java.util.Date
import java.util.UUID

/**
 * WorkManager worker for background transcription
 * Runs transcription as a background job
 */
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptionService: TranscriptionService,
    private val transcriptRepository: TranscriptRepository,
    private val recordingRepository: RecordingRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_RECORDING_ID = "recording_id"
        const val KEY_AUDIO_FILE_PATH = "audio_file_path"
        const val KEY_LANGUAGE = "language"
        const val KEY_TRANSCRIPT_ID = "transcript_id"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun doWork(): Result {
        val recordingId = inputData.getString(KEY_RECORDING_ID) ?: return Result.failure()
        val audioFilePath = inputData.getString(KEY_AUDIO_FILE_PATH) ?: return Result.failure()
        val language = inputData.getString(KEY_LANGUAGE) ?: "en-US"

        return try {
            // Update recording status to processing
            recordingRepository.updateTranscriptionStatus(
                id = recordingId,
                status = ProcessingStatus.PROCESSING.name.lowercase(),
                transcriptId = null
            )

            // Get audio file
            val audioFile = File(audioFilePath)
            if (!audioFile.exists()) {
                recordingRepository.updateTranscriptionStatus(
                    id = recordingId,
                    status = ProcessingStatus.FAILED.name.lowercase(),
                    transcriptId = null
                )
                return Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Audio file not found")
                )
            }

            // Transcribe the audio
            var finalResult: TranscriptionResult? = null
            var lastProgress = 0

            transcriptionService.transcribe(audioFile, language).collect { result ->
                when (result) {
                    is TranscriptionResult.Progress -> {
                        lastProgress = result.percentage
                        setProgress(workDataOf(KEY_PROGRESS to result.percentage))
                    }
                    is TranscriptionResult.PartialResult -> {
                        // Can store partial results if needed
                    }
                    is TranscriptionResult.Success -> {
                        finalResult = result
                    }
                    is TranscriptionResult.Error -> {
                        finalResult = result
                    }
                }
            }

            // Process the final result
            when (val result = finalResult) {
                is TranscriptionResult.Success -> {
                    // Create transcript entity
                    val transcriptId = UUID.randomUUID().toString()
                    val transcript = Transcript(
                        id = transcriptId,
                        recordingId = recordingId,
                        segments = result.segments,
                        speakerMappings = emptyMap(), // No speaker diarization yet
                        engine = TranscriptionEngine.ANDROID_SPEECH,
                        confidence = result.segments.firstOrNull()?.confidence ?: 0.0,
                        processingTime = 0.0, // TODO: Track actual processing time
                        createdAt = Date(),
                        lastModified = Date()
                    )

                    // Save transcript
                    transcriptRepository.saveTranscript(transcript)
                        .onSuccess {
                            // Update recording status
                            recordingRepository.updateTranscriptionStatus(
                                id = recordingId,
                                status = ProcessingStatus.COMPLETED.name.lowercase(),
                                transcriptId = transcriptId
                            )
                        }
                        .onFailure { error ->
                            recordingRepository.updateTranscriptionStatus(
                                id = recordingId,
                                status = ProcessingStatus.FAILED.name.lowercase(),
                                transcriptId = null
                            )
                            return Result.failure(
                                workDataOf(KEY_ERROR_MESSAGE to error.message)
                            )
                        }

                    Result.success(
                        workDataOf(KEY_TRANSCRIPT_ID to transcriptId)
                    )
                }
                is TranscriptionResult.Error -> {
                    recordingRepository.updateTranscriptionStatus(
                        id = recordingId,
                        status = ProcessingStatus.FAILED.name.lowercase(),
                        transcriptId = null
                    )
                    Result.failure(
                        workDataOf(KEY_ERROR_MESSAGE to result.message)
                    )
                }
                else -> {
                    recordingRepository.updateTranscriptionStatus(
                        id = recordingId,
                        status = ProcessingStatus.FAILED.name.lowercase(),
                        transcriptId = null
                    )
                    Result.failure(
                        workDataOf(KEY_ERROR_MESSAGE to "Unknown transcription error")
                    )
                }
            }
        } catch (e: Exception) {
            recordingRepository.updateTranscriptionStatus(
                id = recordingId,
                status = ProcessingStatus.FAILED.name.lowercase(),
                transcriptId = null
            )
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to e.message)
            )
        }
    }
}
