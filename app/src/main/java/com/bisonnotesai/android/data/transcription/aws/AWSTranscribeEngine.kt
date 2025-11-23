package com.bisonnotesai.android.data.transcription.aws

import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.transcribe.TranscribeClient
import aws.sdk.kotlin.services.transcribe.model.*
import aws.smithy.kotlin.runtime.content.ByteStream
import com.bisonnotesai.android.domain.model.TranscriptSegment
import com.bisonnotesai.android.transcription.TranscriptionResult
import com.bisonnotesai.android.transcription.TranscriptionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AWS Transcribe engine with S3 upload and job polling
 * Implements asynchronous transcription workflow
 */
@Singleton
class AWSTranscribeEngine @Inject constructor(
    private val config: AWSTranscribeConfig
) : TranscriptionService {

    companion object {
        private const val TAG = "AWSTranscribeEngine"
        private const val POLL_INTERVAL_MS = 5000L // 5 seconds
        private const val MAX_POLL_ATTEMPTS = 360 // 30 minutes max (360 * 5s)
    }

    private var isCancelled = false
    private var currentJobName: String? = null

    override suspend fun transcribe(
        audioFile: File,
        language: String
    ): Flow<TranscriptionResult> = flow {
        Log.d(TAG, "Starting AWS Transcribe for: ${audioFile.name}")
        isCancelled = false

        try {
            // Validate configuration
            if (!config.credentials.isValid) {
                Log.e(TAG, "AWS credentials not configured")
                emit(TranscriptionResult.Error(
                    "AWS credentials are not configured",
                    AWSTranscribeException.ConfigurationMissing()
                ))
                return@flow
            }

            if (config.bucketName.isBlank()) {
                Log.e(TAG, "S3 bucket name not configured")
                emit(TranscriptionResult.Error(
                    "S3 bucket name is not configured",
                    AWSTranscribeException.ConfigurationMissing()
                ))
                return@flow
            }

            // Validate file
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: ${audioFile.absolutePath}")
                emit(TranscriptionResult.Error(
                    "Audio file not found",
                    AWSTranscribeException.JobStartFailed(Exception("File not found"))
                ))
                return@flow
            }

            emit(TranscriptionResult.Progress(10, "Uploading to AWS S3..."))

            // Step 1: Upload to S3
            val s3Key = uploadToS3(audioFile)
            Log.d(TAG, "S3 upload successful: $s3Key")

            if (isCancelled) {
                emit(TranscriptionResult.Error("Transcription cancelled"))
                return@flow
            }

            emit(TranscriptionResult.Progress(30, "Starting transcription job..."))

            // Step 2: Start transcription job
            val jobName = startTranscriptionJob(s3Key)
            currentJobName = jobName
            Log.d(TAG, "Transcription job started: $jobName")

            emit(TranscriptionResult.Progress(40, "Monitoring transcription job..."))

            // Step 3: Monitor job status
            val jobResult = monitorTranscriptionJob(jobName) { progress ->
                emit(TranscriptionResult.Progress(progress, "Transcribing audio..."))
            }

            if (isCancelled) {
                emit(TranscriptionResult.Error("Transcription cancelled"))
                return@flow
            }

            emit(TranscriptionResult.Progress(80, "Downloading transcript..."))

            // Step 4: Download and parse transcript
            val transcriptUri = jobResult.transcriptUri
                ?: throw AWSTranscribeException.NoTranscriptAvailable()

            val transcriptData = downloadTranscript(transcriptUri)
            val parsedTranscript = parseTranscript(transcriptData)

            emit(TranscriptionResult.Progress(95, "Processing results..."))

            // Step 5: Convert to domain segments
            val segments = parsedTranscript.segments.map { awsSegment ->
                TranscriptSegment(
                    text = awsSegment.text,
                    start = awsSegment.startTime,
                    end = awsSegment.endTime,
                    speaker = awsSegment.speaker,
                    confidence = awsSegment.confidence
                )
            }

            emit(TranscriptionResult.Progress(100, "Transcription complete"))
            emit(TranscriptionResult.Success(
                segments = segments,
                fullText = parsedTranscript.text
            ))

            // Cleanup S3 file
            cleanupS3(s3Key)

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            if (!isCancelled) {
                emit(TranscriptionResult.Error(
                    e.message ?: "Transcription failed",
                    e
                ))
            }
        }
    }

    override fun isSupported(): Boolean {
        // AWS Transcribe is always supported (network-based)
        return true
    }

    override fun getSupportedLanguages(): List<String> {
        // AWS Transcribe supports many languages
        return listOf(
            "en-US", "en-GB", "en-AU", "es-US", "es-ES", "fr-FR", "fr-CA",
            "de-DE", "it-IT", "pt-BR", "pt-PT", "ja-JP", "ko-KR", "zh-CN"
        )
    }

    override fun cancel() {
        Log.d(TAG, "Cancelling transcription")
        isCancelled = true
    }

    /**
     * Test connection to AWS
     */
    suspend fun testConnection(): Result<String> {
        return try {
            Log.d(TAG, "Testing AWS connection...")

            if (!config.credentials.isValid) {
                return Result.failure(AWSTranscribeException.ConfigurationMissing())
            }

            // Test S3 access by listing objects
            val s3Client = createS3Client()
            s3Client.use { client ->
                client.listObjectsV2 {
                    bucket = config.bucketName
                    maxKeys = 1
                }
            }

            Log.d(TAG, "AWS connection test successful")
            Result.success("Connection successful! AWS credentials are valid.")

        } catch (e: Exception) {
            Log.e(TAG, "AWS connection test failed", e)
            Result.failure(AWSTranscribeException.UploadFailed(e))
        }
    }

    /**
     * Upload audio file to S3
     */
    private suspend fun uploadToS3(audioFile: File): String {
        val s3Key = "audio-files/${UUID.randomUUID()}-${audioFile.name}"
        Log.d(TAG, "Uploading to S3: $s3Key")

        val s3Client = createS3Client()

        try {
            s3Client.use { client ->
                val contentType = getContentType(audioFile.name)

                client.putObject {
                    bucket = config.bucketName
                    key = s3Key
                    body = ByteStream.fromFile(audioFile)
                    this.contentType = contentType
                }
            }

            Log.d(TAG, "S3 upload successful")
            return s3Key

        } catch (e: Exception) {
            Log.e(TAG, "S3 upload failed", e)
            throw AWSTranscribeException.UploadFailed(e)
        }
    }

    /**
     * Start AWS Transcribe job
     */
    private suspend fun startTranscriptionJob(s3Key: String): String {
        val jobName = "transcription-${UUID.randomUUID()}"
        Log.d(TAG, "Starting transcription job: $jobName")

        val transcribeClient = createTranscribeClient()

        try {
            transcribeClient.use { client ->
                client.startTranscriptionJob {
                    transcriptionJobName = jobName
                    languageCode = LanguageCode.fromValue(config.languageCode)
                    media {
                        mediaFileUri = "s3://${config.bucketName}/$s3Key"
                    }
                    outputBucketName = config.bucketName
                    outputKey = "transcripts/$jobName.json"
                }
            }

            Log.d(TAG, "Transcription job started successfully")
            return jobName

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start transcription job", e)
            throw AWSTranscribeException.JobStartFailed(e)
        }
    }

    /**
     * Monitor transcription job status with polling
     */
    private suspend fun monitorTranscriptionJob(
        jobName: String,
        onProgress: suspend (Int) -> Unit
    ): AWSTranscribeJobStatus {
        Log.d(TAG, "Monitoring job: $jobName")

        val transcribeClient = createTranscribeClient()
        var attempts = 0

        try {
            transcribeClient.use { client ->
                while (attempts < MAX_POLL_ATTEMPTS && !isCancelled) {
                    val response = client.getTranscriptionJob {
                        transcriptionJobName = jobName
                    }

                    val job = response.transcriptionJob
                        ?: throw AWSTranscribeException.JobNotFound()

                    val status = when (job.transcriptionJobStatus) {
                        aws.sdk.kotlin.services.transcribe.model.TranscriptionJobStatus.Queued ->
                            TranscriptionJobStatus.QUEUED
                        aws.sdk.kotlin.services.transcribe.model.TranscriptionJobStatus.InProgress ->
                            TranscriptionJobStatus.IN_PROGRESS
                        aws.sdk.kotlin.services.transcribe.model.TranscriptionJobStatus.Completed ->
                            TranscriptionJobStatus.COMPLETED
                        aws.sdk.kotlin.services.transcribe.model.TranscriptionJobStatus.Failed ->
                            TranscriptionJobStatus.FAILED
                        else -> TranscriptionJobStatus.FAILED
                    }

                    val jobStatus = AWSTranscribeJobStatus(
                        jobName = jobName,
                        status = status,
                        failureReason = job.failureReason,
                        transcriptUri = job.transcript?.transcriptFileUri
                    )

                    Log.d(TAG, "Job status: ${status.name}")

                    when (status) {
                        TranscriptionJobStatus.COMPLETED -> {
                            return jobStatus
                        }
                        TranscriptionJobStatus.FAILED -> {
                            throw AWSTranscribeException.JobFailed(
                                job.failureReason ?: "Unknown error"
                            )
                        }
                        TranscriptionJobStatus.IN_PROGRESS -> {
                            // Calculate progress (40-80% range during processing)
                            val progress = 40 + ((attempts * 40) / MAX_POLL_ATTEMPTS)
                            onProgress(progress.coerceIn(40, 75))
                        }
                        TranscriptionJobStatus.QUEUED -> {
                            onProgress(40)
                        }
                    }

                    // Wait before next poll
                    delay(POLL_INTERVAL_MS)
                    attempts++
                }

                if (isCancelled) {
                    throw AWSTranscribeException.JobFailed("Job cancelled by user")
                }

                if (attempts >= MAX_POLL_ATTEMPTS) {
                    throw AWSTranscribeException.JobFailed("Job timeout - exceeded maximum polling time")
                }

                throw AWSTranscribeException.UnknownJobStatus()
            }

        } catch (e: Exception) {
            if (e is AWSTranscribeException) throw e
            Log.e(TAG, "Job monitoring failed", e)
            throw AWSTranscribeException.JobMonitoringFailed(e)
        }
    }

    /**
     * Download transcript from S3
     */
    private suspend fun downloadTranscript(uri: String): String {
        Log.d(TAG, "Downloading transcript from: $uri")

        try {
            // Extract S3 key from URI
            val url = URL(uri)
            val pathComponents = url.path.split("/").filter { it.isNotBlank() }

            if (pathComponents.size < 2) {
                throw AWSTranscribeException.InvalidTranscriptURI()
            }

            // Skip bucket name, get the key
            val s3Key = pathComponents.drop(1).joinToString("/")
            Log.d(TAG, "Extracted S3 key: $s3Key")

            val s3Client = createS3Client()

            s3Client.use { client ->
                val response = client.getObject {
                    bucket = config.bucketName
                    key = s3Key
                }

                val data = response.body?.readAll()
                    ?: throw AWSTranscribeException.InvalidTranscriptURI()

                val transcriptText = data.decodeToString()
                Log.d(TAG, "Transcript downloaded: ${transcriptText.length} characters")

                return transcriptText
            }

        } catch (e: Exception) {
            if (e is AWSTranscribeException) throw e
            Log.e(TAG, "Failed to download transcript", e)
            throw AWSTranscribeException.InvalidTranscriptURI()
        }
    }

    /**
     * Parse AWS Transcribe JSON response
     */
    private fun parseTranscript(jsonData: String): AWSTranscriptResult {
        Log.d(TAG, "Parsing transcript JSON")

        try {
            val json = JSONObject(jsonData)

            // Get main transcript text
            val results = json.getJSONObject("results")
            val transcripts = results.getJSONArray("transcripts")
            val transcriptText = transcripts.getJSONObject(0).getString("transcript")

            val segments = mutableListOf<AWSTranscriptSegment>()
            var totalConfidence = 0.0
            var confidenceCount = 0

            // Parse speaker segments if available
            if (results.has("speaker_labels")) {
                val speakerLabels = results.getJSONObject("speaker_labels")
                val segmentsData = speakerLabels.getJSONArray("segments")

                for (i in 0 until segmentsData.length()) {
                    val segmentData = segmentsData.getJSONObject(i)
                    val startTime = segmentData.getString("start_time").toDouble()
                    val endTime = segmentData.getString("end_time").toDouble()
                    val speakerLabel = segmentData.getString("speaker_label")
                    val items = segmentData.getJSONArray("items")

                    var segmentText = ""
                    var segmentConfidence = 0.0
                    var itemCount = 0

                    for (j in 0 until items.length()) {
                        val item = items.getJSONObject(j)
                        if (item.has("alternatives")) {
                            val alternatives = item.getJSONArray("alternatives")
                            val alternative = alternatives.getJSONObject(0)
                            val content = alternative.getString("content")
                            val confidence = alternative.optDouble("confidence", 0.0)

                            segmentText += "$content "
                            segmentConfidence += confidence
                            itemCount++
                        }
                    }

                    if (itemCount > 0) {
                        segmentConfidence /= itemCount
                        totalConfidence += segmentConfidence
                        confidenceCount++
                    }

                    segments.add(
                        AWSTranscriptSegment(
                            speaker = speakerLabel,
                            text = segmentText.trim(),
                            startTime = startTime,
                            endTime = endTime,
                            confidence = if (itemCount > 0) segmentConfidence else null
                        )
                    )
                }
            } else {
                // No speaker labels - create single segment
                segments.add(
                    AWSTranscriptSegment(
                        speaker = "Speaker",
                        text = transcriptText,
                        startTime = 0.0,
                        endTime = 0.0,
                        confidence = null
                    )
                )
            }

            val averageConfidence = if (confidenceCount > 0) {
                totalConfidence / confidenceCount
            } else {
                0.0
            }

            Log.d(TAG, "Parsed ${segments.size} segments with average confidence $averageConfidence")

            return AWSTranscriptResult(
                text = transcriptText,
                segments = segments,
                confidence = averageConfidence
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse transcript", e)
            throw AWSTranscribeException.InvalidTranscriptFormat()
        }
    }

    /**
     * Cleanup S3 file after transcription
     */
    private suspend fun cleanupS3(s3Key: String) {
        try {
            Log.d(TAG, "Cleaning up S3 file: $s3Key")

            val s3Client = createS3Client()
            s3Client.use { client ->
                client.deleteObject {
                    bucket = config.bucketName
                    key = s3Key
                }
            }

            Log.d(TAG, "S3 cleanup successful")

        } catch (e: Exception) {
            // Log but don't throw - cleanup failure shouldn't fail the transcription
            Log.w(TAG, "S3 cleanup failed", e)
        }
    }

    /**
     * Create S3 client with credentials
     */
    private fun createS3Client(): S3Client {
        return S3Client {
            region = config.credentials.region
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = config.credentials.accessKeyId
                secretAccessKey = config.credentials.secretAccessKey
            }
        }
    }

    /**
     * Create Transcribe client with credentials
     */
    private fun createTranscribeClient(): TranscribeClient {
        return TranscribeClient {
            region = config.credentials.region
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = config.credentials.accessKeyId
                secretAccessKey = config.credentials.secretAccessKey
            }
        }
    }

    /**
     * Get content type for audio file
     */
    private fun getContentType(fileName: String): String {
        return when (fileName.substringAfterLast('.').lowercase()) {
            "m4a", "mp4" -> "audio/mp4"
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            else -> "audio/mp4" // Default
        }
    }
}
