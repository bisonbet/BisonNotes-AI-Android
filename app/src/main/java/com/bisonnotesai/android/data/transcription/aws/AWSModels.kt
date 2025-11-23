package com.bisonnotesai.android.data.transcription.aws

/**
 * AWS credentials for authentication
 */
data class AWSCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val region: String
) {
    val isValid: Boolean
        get() = accessKeyId.isNotBlank() && secretAccessKey.isNotBlank() && region.isNotBlank()

    companion object {
        val DEFAULT = AWSCredentials(
            accessKeyId = "",
            secretAccessKey = "",
            region = "us-east-1"
        )
    }
}

/**
 * AWS Transcribe configuration
 */
data class AWSTranscribeConfig(
    val credentials: AWSCredentials,
    val bucketName: String,
    val languageCode: String = "en-US"
) {
    companion object {
        val DEFAULT = AWSTranscribeConfig(
            credentials = AWSCredentials.DEFAULT,
            bucketName = ""
        )
    }
}

/**
 * AWS Transcription job status
 */
enum class TranscriptionJobStatus {
    QUEUED,
    IN_PROGRESS,
    COMPLETED,
    FAILED;

    companion object {
        fun fromString(status: String): TranscriptionJobStatus {
            return when (status.uppercase()) {
                "QUEUED" -> QUEUED
                "IN_PROGRESS" -> IN_PROGRESS
                "COMPLETED" -> COMPLETED
                "FAILED" -> FAILED
                else -> FAILED
            }
        }
    }
}

/**
 * AWS Transcription job information
 */
data class AWSTranscribeJobStatus(
    val jobName: String,
    val status: TranscriptionJobStatus,
    val failureReason: String? = null,
    val transcriptUri: String? = null
) {
    val isCompleted: Boolean
        get() = status == TranscriptionJobStatus.COMPLETED

    val isFailed: Boolean
        get() = status == TranscriptionJobStatus.FAILED

    val isInProgress: Boolean
        get() = status == TranscriptionJobStatus.IN_PROGRESS
}

/**
 * AWS Transcribe transcript result
 */
data class AWSTranscriptResult(
    val text: String,
    val segments: List<AWSTranscriptSegment>,
    val confidence: Double
)

/**
 * AWS Transcript segment with speaker labels
 */
data class AWSTranscriptSegment(
    val speaker: String,
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val confidence: Double? = null
)

/**
 * AWS-specific exceptions
 */
sealed class AWSTranscribeException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ConfigurationMissing : AWSTranscribeException("AWS configuration is missing. Please check your credentials.")

    class UploadFailed(cause: Throwable) : AWSTranscribeException("Failed to upload file to S3: ${cause.message}", cause)

    class JobStartFailed(cause: Throwable) : AWSTranscribeException("Failed to start transcription job: ${cause.message}", cause)

    class JobMonitoringFailed(cause: Throwable) : AWSTranscribeException("Failed to monitor transcription job: ${cause.message}", cause)

    class JobFailed(reason: String) : AWSTranscribeException("Transcription job failed: $reason")

    class JobNotFound : AWSTranscribeException("Transcription job not found")

    class UnknownJobStatus : AWSTranscribeException("Unknown transcription job status")

    class NoTranscriptAvailable : AWSTranscribeException("No transcript available for the completed job")

    class InvalidTranscriptURI : AWSTranscribeException("Invalid transcript URI")

    class InvalidTranscriptFormat : AWSTranscribeException("Invalid transcript format")
}

/**
 * AWS Regions
 */
enum class AWSRegion(val regionId: String, val displayName: String) {
    US_EAST_1("us-east-1", "US East (N. Virginia)"),
    US_EAST_2("us-east-2", "US East (Ohio)"),
    US_WEST_1("us-west-1", "US West (N. California)"),
    US_WEST_2("us-west-2", "US West (Oregon)"),
    EU_WEST_1("eu-west-1", "EU (Ireland)"),
    EU_CENTRAL_1("eu-central-1", "EU (Frankfurt)"),
    AP_SOUTHEAST_1("ap-southeast-1", "Asia Pacific (Singapore)"),
    AP_NORTHEAST_1("ap-northeast-1", "Asia Pacific (Tokyo)");

    companion object {
        fun fromRegionId(id: String): AWSRegion {
            return values().find { it.regionId == id } ?: US_EAST_1
        }
    }
}
