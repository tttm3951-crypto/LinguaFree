package org.linguafree.core.data.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Answer sounds and haptics. The sounds are synthesised on the device (original LinguaFree
 * tones, no audio files). Both can be switched off in settings.
 */
class Feedback(context: Context) {

    enum class Sound { Correct, Wrong, Combo, Complete, Tap, Chest }

    var soundEnabled = true
    var hapticsEnabled = true

    private val executor = Executors.newSingleThreadExecutor()
    private val rate = 22_050
    private val cache = HashMap<Sound, ShortArray>()
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun correct() { play(Sound.Correct); vibrate(strong = false) }
    fun wrong() { play(Sound.Wrong); vibrate(strong = true) }
    fun combo() = play(Sound.Combo)
    fun complete() { play(Sound.Complete); vibrate(strong = false) }
    fun tap() { if (hapticsEnabled) vibrateTick() }
    fun chest() = play(Sound.Chest)

    fun play(sound: Sound) {
        if (!soundEnabled) return
        executor.execute {
            runCatching {
                val pcm = cache.getOrPut(sound) { synth(sound) }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / rate + 60)
                track.stop()
                track.release()
            }
        }
    }

    /** Notes as (frequency Hz, duration ms, volume 0..1). Soft bell-like tone: sine + octave, exp decay. */
    private fun synth(sound: Sound): ShortArray {
        val notes: List<Triple<Double, Int, Double>> = when (sound) {
            Sound.Correct -> listOf(Triple(784.0, 90, 0.55), Triple(1175.0, 220, 0.6))
            Sound.Wrong -> listOf(Triple(330.0, 110, 0.35), Triple(262.0, 220, 0.35))
            Sound.Combo -> listOf(Triple(784.0, 80, 0.45), Triple(988.0, 80, 0.5), Triple(1175.0, 80, 0.55), Triple(1568.0, 220, 0.6))
            Sound.Complete -> listOf(Triple(523.0, 120, 0.5), Triple(659.0, 120, 0.5), Triple(784.0, 120, 0.55), Triple(1047.0, 360, 0.65))
            Sound.Tap -> listOf(Triple(1400.0, 25, 0.25))
            Sound.Chest -> listOf(Triple(988.0, 70, 0.4), Triple(1319.0, 70, 0.45), Triple(1760.0, 260, 0.55))
        }
        val total = notes.sumOf { it.second } * rate / 1000
        val out = ShortArray(total)
        var offset = 0
        for ((f, ms, vol) in notes) {
            val n = ms * rate / 1000
            for (i in 0 until n) {
                val t = i.toDouble() / rate
                val attack = (i / (rate * 0.006)).coerceAtMost(1.0)
                val decay = exp(-t * 6.0)
                val tail = ((n - i) / (rate * 0.02)).coerceAtMost(1.0)
                val wave = sin(2 * PI * f * t) + 0.25 * sin(4 * PI * f * t)
                out[offset + i] = (wave * attack * decay * tail * vol * 9000).toInt().coerceIn(-32767, 32767).toShort()
            }
            offset += n
        }
        return out
    }

    private fun vibrate(strong: Boolean) {
        if (!hapticsEnabled) return
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                v.vibrate(VibrationEffect.createPredefined(if (strong) VibrationEffect.EFFECT_DOUBLE_CLICK else VibrationEffect.EFFECT_CLICK))
            } else {
                v.vibrate(VibrationEffect.createOneShot(if (strong) 60 else 35, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    private fun vibrateTick() {
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            else v.vibrate(VibrationEffect.createOneShot(10, 80))
        }
    }
}
