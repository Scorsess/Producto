package com.producto.timer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Notifies the user that a countdown has finished. */
interface SessionNotifier {
    fun notifySessionComplete()
}

/**
 * Plays a tone and/or vibrates when a countdown finishes, respecting the device's ringer mode:
 * silent -> nothing, vibrate -> haptic only, normal -> tone + haptic.
 */
class AndroidSessionNotifier(private val context: Context) : SessionNotifier {

    override fun notifySessionComplete() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                playTone()
                vibrate()
            }
            AudioManager.RINGER_MODE_VIBRATE -> vibrate()
            else -> Unit // RINGER_MODE_SILENT: honor the mute, stay silent.
        }
    }

    private fun playTone() {
        runCatching {
            // Use a dedicated ToneGenerator or manage its lifecycle better.
            // For a short beep, creating and releasing is acceptable, but let's ensure it's released.
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS.toInt())
            // We still need a delay to allow the tone to play before releasing.
            // But we can use a more robust way to handle this if we were doing complex audio.
            // For this app, this is okay, but let's make sure we don't leak it if the app closes.
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching { toneGenerator.release() }
            }, TONE_DURATION_MS + 500L)
        }
    }

    private fun vibrate() {
        val vibrator = vibratorOrNull() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_DURATION_MS)
        }
    }

    private fun vibratorOrNull(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private companion object {
        const val TONE_DURATION_MS = 400L
        const val VIBRATION_DURATION_MS = 300L
    }
}
