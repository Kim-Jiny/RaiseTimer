package com.jiny.raisetimer.ui.turntimer

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TurnTimerState(
    val totalSeconds: Int = 30,
    val remainingSeconds: Int = 30,
    val isRunning: Boolean = false,
    val hasAlerted: Boolean = false,
)

class TurnTimerViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(TurnTimerState())
    val state: StateFlow<TurnTimerState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun selectPreset(seconds: Int) {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(
            totalSeconds = seconds,
            remainingSeconds = seconds,
            hasAlerted = false,
        )
    }

    fun toggle() {
        if (_state.value.isRunning) pause() else start()
    }

    fun start() {
        val current = _state.value
        if (current.remainingSeconds == 0) {
            _state.value = current.copy(
                remainingSeconds = current.totalSeconds,
                hasAlerted = false,
            )
        }
        _state.value = _state.value.copy(isRunning = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.remainingSeconds > 0 && _state.value.isRunning) {
                delay(1000L)
                if (!_state.value.isRunning) break
                val remaining = _state.value.remainingSeconds - 1
                _state.value = _state.value.copy(remainingSeconds = remaining)
                if (remaining in 1..5) {
                    playTick()
                }
                if (remaining == 0) {
                    expire()
                }
            }
        }
    }

    fun pause() {
        _state.value = _state.value.copy(isRunning = false)
        timerJob?.cancel()
        timerJob = null
    }

    fun reset() {
        pause()
        _state.value = _state.value.copy(
            remainingSeconds = _state.value.totalSeconds,
            hasAlerted = false,
        )
    }

    private fun expire() {
        _state.value = _state.value.copy(isRunning = false, hasAlerted = true)
        timerJob?.cancel()
        timerJob = null
        playAlert()
        vibrate()
    }

    private fun playTick() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            tg.release()
        } catch (_: Exception) {}
    }

    private fun playAlert() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
            tg.release()
        } catch (_: Exception) {}
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = app.getSystemService(Vibrator::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        } catch (_: Exception) {}
    }
}
