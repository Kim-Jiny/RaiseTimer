package com.jiny.raisetimer.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Plays the level-change alert. Tries to load a bundled sound from res/raw/level_change; if
 * that file is missing (the repo ships without audio assets), falls back to the system
 * [ToneGenerator] beep so the app still audibly signals level transitions.
 */
class SoundPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val levelChangeSoundId: Int? = runCatching {
        val resId = appContext.resources.getIdentifier("level_change", "raw", appContext.packageName)
        if (resId != 0) soundPool.load(appContext, resId, 1) else null
    }.getOrNull()

    private val toneGenerator by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }.getOrNull()
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playLevelChange() {
        val id = levelChangeSoundId
        if (id != null) {
            soundPool.play(id, 1f, 1f, 1, 0, 1f)
        } else {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        }
        vibrate(longArrayOf(0, 200, 100, 200))
    }

    fun playCountdownTick() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    private fun vibrate(pattern: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, -1)
        }
    }

    fun release() {
        soundPool.release()
        toneGenerator?.release()
    }
}
