package com.bisonnotesai.android.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio player wrapper around ExoPlayer
 * Handles playback of recorded audio files
 */
@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: AudioSessionManager
) {

    private var exoPlayer: ExoPlayer? = null
    private var currentFile: File? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    /**
     * Initialize player for a file
     */
    fun preparePlayer(file: File): Result<Unit> {
        return try {
            // Release any existing player
            release()

            // Request audio focus
            val hasFocus = sessionManager.requestAudioFocusForPlayback(
                onFocusLost = { pause() },
                onFocusGained = { /* Can resume if needed */ }
            )

            if (!hasFocus) {
                return Result.failure(Exception("Failed to gain audio focus"))
            }

            // Create ExoPlayer instance
            val player = ExoPlayer.Builder(context).build()

            // Set up player listener
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            _isPlaying.value = false
                            seekTo(0)
                        }
                        Player.STATE_READY -> {
                            _duration.value = player.duration
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }
            })

            // Create media item from file
            val mediaItem = MediaItem.fromUri(file.toURI().toString())
            player.setMediaItem(mediaItem)
            player.prepare()

            exoPlayer = player
            currentFile = file

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start or resume playback
     */
    fun play(): Result<Unit> {
        return try {
            exoPlayer?.play()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pause playback
     */
    fun pause(): Result<Unit> {
        return try {
            exoPlayer?.pause()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop playback and reset position
     */
    fun stop(): Result<Unit> {
        return try {
            exoPlayer?.apply {
                stop()
                seekTo(0)
            }
            _isPlaying.value = false
            _currentPosition.value = 0
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Seek to position (milliseconds)
     */
    fun seekTo(positionMs: Long): Result<Unit> {
        return try {
            exoPlayer?.seekTo(positionMs)
            _currentPosition.value = positionMs
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Skip forward (default 10 seconds)
     */
    fun skipForward(milliseconds: Long = 10000): Result<Unit> {
        val currentPos = exoPlayer?.currentPosition ?: 0
        val newPos = (currentPos + milliseconds).coerceAtMost(exoPlayer?.duration ?: 0)
        return seekTo(newPos)
    }

    /**
     * Skip backward (default 10 seconds)
     */
    fun skipBackward(milliseconds: Long = 10000): Result<Unit> {
        val currentPos = exoPlayer?.currentPosition ?: 0
        val newPos = (currentPos - milliseconds).coerceAtLeast(0)
        return seekTo(newPos)
    }

    /**
     * Set playback speed
     * Speed can be 0.5x to 2.0x
     */
    fun setPlaybackSpeed(speed: Float): Result<Unit> {
        return try {
            val validSpeed = speed.coerceIn(0.5f, 2.0f)
            exoPlayer?.setPlaybackSpeed(validSpeed)
            _playbackSpeed.value = validSpeed
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update current position (call periodically from a timer)
     */
    fun updatePosition() {
        exoPlayer?.let { player ->
            _currentPosition.value = player.currentPosition
            _duration.value = player.duration
        }
    }

    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }

    /**
     * Get total duration
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0
    }

    /**
     * Check if player is ready
     */
    fun isReady(): Boolean {
        return exoPlayer?.playbackState == Player.STATE_READY
    }

    /**
     * Release player resources
     */
    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        currentFile = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _duration.value = 0
        sessionManager.abandonAudioFocus()
    }

    /**
     * Format duration for display (MM:SS)
     */
    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * Get playback progress (0.0 to 1.0)
     */
    fun getProgress(): Float {
        val duration = getDuration()
        val position = getCurrentPosition()
        return if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    /**
     * Playback speed presets
     */
    companion object {
        const val SPEED_HALF = 0.5f
        const val SPEED_NORMAL = 1.0f
        const val SPEED_1_25X = 1.25f
        const val SPEED_1_5X = 1.5f
        const val SPEED_2X = 2.0f

        val SPEED_OPTIONS = listOf(
            SPEED_HALF,
            SPEED_NORMAL,
            SPEED_1_25X,
            SPEED_1_5X,
            SPEED_2X
        )
    }
}
