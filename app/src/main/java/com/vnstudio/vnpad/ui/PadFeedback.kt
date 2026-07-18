package com.vnstudio.vnpad.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Tactile + audible pad feedback: a short haptic tick and a soft synthesized
 * click. The click is generated once (no bundled asset) as a fast-decaying tone.
 * Every step is wrapped so a device without a vibrator or audio focus degrades
 * silently rather than crashing.
 */
class PadFeedback(context: Context) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }.getOrNull()

    private val sampleRate = 44_100
    private val click: ShortArray = buildClick()
    private var track: AudioTrack? = ensureTrack()

    private fun buildClick(): ShortArray {
        val duration = 0.03 // 30 ms
        val count = (sampleRate * duration).toInt()
        val freq = 1300.0
        return ShortArray(count) { i ->
            val t = i / sampleRate.toDouble()
            val envelope = exp(-t * 95.0) // sharp attack, quick decay → soft tick
            val sample = sin(2 * PI * freq * t) * envelope * 0.35
            (sample * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun ensureTrack(): AudioTrack? = runCatching {
        val sizeBytes = click.size * 2
        AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            sizeBytes,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        ).apply { write(click, 0, click.size) }
    }.getOrNull()

    /** Fire haptic + click. Safe to call rapidly. */
    fun tap() {
        runCatching {
            vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        runCatching {
            track?.let { t ->
                runCatching { t.stop() }         // reset playhead to start on a static buffer
                runCatching { t.reloadStaticData() }
                t.play()
            }
        }
    }

    fun dispose() {
        runCatching { track?.release() }
        track = null
    }
}

/** Build a [PadFeedback] tied to the composition; releases audio on leave. */
@Composable
fun rememberPadFeedback(): PadFeedback {
    val context = LocalContext.current
    val feedback = remember { PadFeedback(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { feedback.dispose() } }
    return feedback
}
