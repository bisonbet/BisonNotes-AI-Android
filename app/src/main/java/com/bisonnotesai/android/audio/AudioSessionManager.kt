package com.bisonnotesai.android.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio focus and audio session configuration
 * Handles interruptions from phone calls, other apps, etc.
 */
@Singleton
class AudioSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private var onAudioFocusLost: (() -> Unit)? = null
    private var onAudioFocusGained: (() -> Unit)? = null

    /**
     * Request audio focus for recording
     */
    fun requestAudioFocusForRecording(
        onFocusLost: () -> Unit,
        onFocusGained: () -> Unit
    ): Boolean {
        this.onAudioFocusLost = onFocusLost
        this.onAudioFocusGained = onFocusGained

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestAudioFocusOreo(AudioManager.AUDIOFOCUS_GAIN, onFocusLost, onFocusGained)
        } else {
            requestAudioFocusLegacy(AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    /**
     * Request audio focus for playback
     */
    fun requestAudioFocusForPlayback(
        onFocusLost: () -> Unit,
        onFocusGained: () -> Unit
    ): Boolean {
        this.onAudioFocusLost = onFocusLost
        this.onAudioFocusGained = onFocusGained

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestAudioFocusOreo(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, onFocusLost, onFocusGained)
        } else {
            requestAudioFocusLegacy(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    /**
     * Request audio focus (Android O and above)
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocusOreo(
        focusGain: Int,
        onFocusLost: () -> Unit,
        onFocusGained: () -> Unit
    ): Boolean {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val focusRequest = AudioFocusRequest.Builder(focusGain)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange, onFocusLost, onFocusGained)
            }
            .build()

        this.audioFocusRequest = focusRequest

        val result = audioManager.requestAudioFocus(focusRequest)
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        return hasAudioFocus
    }

    /**
     * Request audio focus (Legacy - before Android O)
     */
    @Suppress("DEPRECATION")
    private fun requestAudioFocusLegacy(focusGain: Int): Boolean {
        val result = audioManager.requestAudioFocus(
            { focusChange ->
                handleAudioFocusChange(
                    focusChange,
                    onAudioFocusLost ?: {},
                    onAudioFocusGained ?: {}
                )
            },
            AudioManager.STREAM_MUSIC,
            focusGain
        )

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    /**
     * Handle audio focus changes
     */
    private fun handleAudioFocusChange(
        focusChange: Int,
        onFocusLost: () -> Unit,
        onFocusGained: () -> Unit
    ) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained audio focus
                hasAudioFocus = true
                onFocusGained()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost audio focus permanently
                hasAudioFocus = false
                abandonAudioFocus()
                onFocusLost()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost audio focus temporarily
                hasAudioFocus = false
                onFocusLost()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost audio focus but can duck (lower volume)
                // For recording, we'll treat this as full loss
                hasAudioFocus = false
                onFocusLost()
            }
        }
    }

    /**
     * Abandon audio focus
     */
    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus { }
        }

        hasAudioFocus = false
        audioFocusRequest = null
        onAudioFocusLost = null
        onAudioFocusGained = null
    }

    /**
     * Check if we currently have audio focus
     */
    fun hasAudioFocus(): Boolean {
        return hasAudioFocus
    }

    /**
     * Set audio mode for recording
     */
    fun setModeForRecording() {
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    /**
     * Set audio mode for playback
     */
    fun setModeForPlayback() {
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    /**
     * Reset audio mode to default
     */
    fun resetAudioMode() {
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    /**
     * Check if headphones are connected
     */
    fun isHeadphonesConnected(): Boolean {
        return audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
    }

    /**
     * Get current volume level
     */
    fun getCurrentVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    /**
     * Get max volume level
     */
    fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
}
