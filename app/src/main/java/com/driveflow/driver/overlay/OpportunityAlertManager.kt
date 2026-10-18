package com.driveflow.driver.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.driveflow.driver.R
import com.driveflow.driver.data.prefs.AlertMode
import com.driveflow.driver.engine.OpportunityEvent

/**
 * Manages audible and haptic opportunity alerts for golden / profitable ride requests.
 * Uses a dedicated notification channel with custom audio chime (opportunity_chime.wav).
 */
class OpportunityAlertManager(
    private val context: Context,
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
) {
    companion object {
        const val CHANNEL_ID = "opportunity"
        const val NOTIFICATION_ID = 1001
        val VIBRATION_PATTERN = longArrayOf(0, 150, 100, 200)
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var testMediaPlayer: MediaPlayer? = null

    init {
        createNotificationChannel()
    }

    /**
     * Creates or updates the notification channel with high importance, custom chime, and vibration.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse(
                "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/opportunity_chime"
            )
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channelName = context.getString(R.string.channel_opportunity_name)
            val channelDesc = context.getString(R.string.channel_opportunity_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Fires the alert for an opportunity event according to the driver's alert mode.
     * Guaranteed never to present a lingering heads-up card covering the ride app.
     */
    fun fireAlert(event: OpportunityEvent, alertMode: AlertMode = AlertMode.TONE_VIBRATE) {
        // Direct media player playback ensures sound plays instantaneously
        playSound()
        if (alertMode == AlertMode.TONE_VIBRATE) {
            vibrate()
        }
    }

    /**
     * Plays the alert chime for user preview ("Hear the alert" button).
     */
    fun playTestAlert(alertMode: AlertMode = AlertMode.TONE_VIBRATE) {
        playSound()
        if (alertMode == AlertMode.TONE_VIBRATE) {
            vibrate()
        }
    }

    private fun playSound() {
        try {
            testMediaPlayer?.release()
            testMediaPlayer = MediaPlayer.create(context, R.raw.opportunity_chime)?.apply {
                setOnCompletionListener { mp ->
                    mp.release()
                    testMediaPlayer = null
                }
                start()
            }
        } catch (_: Exception) {}
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(VIBRATION_PATTERN, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(VIBRATION_PATTERN, -1)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Checks if notification or media volume is muted or on vibrate only.
     */
    fun isNotificationMuted(): Boolean {
        val am = audioManager ?: return false
        val ringerMode = am.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            return true
        }
        val notifVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        return notifVolume == 0
    }
}
