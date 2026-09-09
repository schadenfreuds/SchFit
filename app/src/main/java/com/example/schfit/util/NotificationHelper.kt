package com.example.schfit.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.schfit.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.schfit.R

object NotificationHelper {

    private const val CHANNEL_ID = "schfit_rest_timer_channel"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun sendRestTimerCompletedNotification(
        context: Context,
        exerciseName: String,
        setNumber: Int,
        isVibrationEnabled: Boolean = true
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notif_rest_done_title))
            .setContentText(context.getString(R.string.notif_rest_done_set_ready, exerciseName, setNumber))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission missing
        }
    }

    fun playSoundByType(soundType: String, context: Context? = null) {
        if (soundType == "silent") return

        val soundRes = when (soundType) {
            "bell" -> com.example.schfit.R.raw.sound_boxing_bell
            "chime" -> com.example.schfit.R.raw.sound_melodic_chime
            "whistle" -> com.example.schfit.R.raw.sound_gym_whistle
            else -> com.example.schfit.R.raw.sound_digital_timer
        }

        if (context != null) {
            try {
                val mp = android.media.MediaPlayer.create(context.applicationContext, soundRes)
                mp?.setOnCompletionListener { it.release() }
                mp?.start()
                return
            } catch (_: Exception) {
                // Fallback to ToneGenerator if MediaPlayer fails
            }
        }

        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 280)
        } catch (_: Exception) {
        }
    }

    fun playPrCelebrationSound(context: Context? = null) {
        if (context != null) {
            try {
                val mp = android.media.MediaPlayer.create(
                    context.applicationContext,
                    com.example.schfit.R.raw.sound_minecraft_achievement
                )
                mp?.setOnCompletionListener { it.release() }
                mp?.start()
                return
            } catch (_: Exception) {
            }
        }

        try {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
                kotlinx.coroutines.delay(120)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                kotlinx.coroutines.delay(140)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT, 350)
            }
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 400, 200, 400),
                    -1
                )
                vibrator?.vibrate(vibrationEffect)
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createWaveform(
                        longArrayOf(0, 400, 200, 400),
                        -1
                    )
                    vibrator?.vibrate(vibrationEffect)
                } else {
                    vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            }
        } catch (_: Exception) {
            // Vibration failed
        }
    }
}
