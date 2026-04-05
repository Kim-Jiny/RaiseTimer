package com.jiny.raisetimer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jiny.raisetimer.data.TournamentRepository
import com.jiny.raisetimer.domain.TimerEngine
import com.jiny.raisetimer.domain.model.BlindLevel
import com.jiny.raisetimer.domain.model.Player
import com.jiny.raisetimer.domain.model.TournamentConfig
import com.jiny.raisetimer.domain.model.TournamentState
import com.jiny.raisetimer.sound.SoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single source of truth for the tournament. All four screens subscribe to [state].
 */
class TournamentViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TournamentRepository(app)
    private val soundPlayer = SoundPlayer(app)

    private val _state = MutableStateFlow(TournamentState())
    val state: StateFlow<TournamentState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var restoredInitialState = false
    private var localChangesBeforeRestore = false

    init {
        viewModelScope.launch {
            val restored = repository.stateFlow.first()
            if (!localChangesBeforeRestore) {
                val normalized = normalizePlayerPlacements(restored)
                if (normalized.isRunning && normalized.lastTickAt != null) {
                    // Catch up any wall-clock time that passed while the process was dead.
                    val result = TimerEngine.catchUp(normalized, System.currentTimeMillis())
                    _state.value = result.state
                    if (result.levelAdvanced) soundPlayer.playLevelChange()
                    persist()
                    if (!result.finished) launchTickerLoop()
                } else {
                    _state.value = normalized.copy(isRunning = false, lastTickAt = null)
                }
            }
            restoredInitialState = true
        }
    }

    /**
     * Called from the Activity lifecycle on every ON_RESUME so that the running timer stays
     * in sync with wall-clock time even if the tick loop was throttled or suspended while
     * the app was in the background.
     */
    fun catchUpFromBackground() {
        val current = _state.value
        if (!current.isRunning || current.lastTickAt == null) return
        val result = TimerEngine.catchUp(current, System.currentTimeMillis())
        if (result.state == current) return
        _state.value = result.state
        if (result.levelAdvanced) soundPlayer.playLevelChange()
        persist()
        if (result.finished) {
            tickJob?.cancel()
        } else if (tickJob?.isActive != true) {
            launchTickerLoop()
        }
    }

    // ---- Timer control ----

    fun toggleRunning() {
        val current = _state.value
        if (current.isRunning) pause() else start()
    }

    fun start() {
        if (_state.value.isRunning) return
        markLocalMutation()
        _state.value = TimerEngine.start(_state.value, System.currentTimeMillis())
        persist()
        launchTickerLoop()
    }

    private fun launchTickerLoop() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (_state.value.isRunning) {
                delay(1_000)
                val result = TimerEngine.catchUp(_state.value, System.currentTimeMillis())
                val prevRemaining = _state.value.remainingSeconds
                _state.value = result.state
                // Countdown beep only fires on a smooth 1-second foreground tick.
                if (!result.levelAdvanced &&
                    result.state.remainingSeconds in 1..5 &&
                    result.state.remainingSeconds < prevRemaining
                ) {
                    soundPlayer.playCountdownTick()
                }
                if (result.levelAdvanced) {
                    soundPlayer.playLevelChange()
                    persist()
                    if (result.finished) break
                }
            }
        }
    }

    fun pause() {
        tickJob?.cancel()
        markLocalMutation()
        _state.value = TimerEngine.pause(_state.value)
        persist()
    }

    fun reset() {
        tickJob?.cancel()
        markLocalMutation()
        _state.value = TimerEngine.reset(_state.value)
        persist()
    }

    fun nextLevel() {
        markLocalMutation()
        _state.value = TimerEngine.nextLevel(_state.value, System.currentTimeMillis())
        persist()
    }

    fun previousLevel() {
        markLocalMutation()
        _state.value = TimerEngine.previousLevel(_state.value, System.currentTimeMillis())
        persist()
    }

    // ---- Players ----

    fun addPlayer(name: String) {
        val trimmed = name.trim().ifEmpty { return }
        markLocalMutation()
        _state.value = normalizePlayerPlacements(_state.value.copy(
            players = _state.value.players + Player(name = trimmed)
        ))
        persist()
    }

    fun removePlayer(id: String) {
        markLocalMutation()
        _state.value = normalizePlayerPlacements(_state.value.copy(
            players = _state.value.players.filterNot { it.id == id }
        ))
        persist()
    }

    fun eliminatePlayer(id: String) {
        val current = _state.value
        markLocalMutation()
        _state.value = normalizePlayerPlacements(current.copy(
            players = current.players.map {
                if (it.id == id && !it.isEliminated) {
                    it.copy(isEliminated = true)
                } else it
            }
        ))
        persist()
    }

    fun revivePlayer(id: String) {
        markLocalMutation()
        _state.value = normalizePlayerPlacements(_state.value.copy(
            players = _state.value.players.map {
                if (it.id == id) it.copy(isEliminated = false, placement = null) else it
            }
        ))
        persist()
    }

    fun incrementRebuy(id: String, delta: Int) {
        if (!_state.value.config.rebuyAllowed) return
        markLocalMutation()
        _state.value = _state.value.copy(
            players = _state.value.players.map {
                if (it.id == id) it.copy(rebuyCount = (it.rebuyCount + delta).coerceAtLeast(0)) else it
            }
        )
        persist()
    }

    // ---- Config ----

    fun updateConfig(transform: (TournamentConfig) -> TournamentConfig) {
        val current = _state.value
        val newConfig = transform(current.config)
        val safeIndex = current.currentLevelIndex.coerceIn(0, newConfig.levels.lastIndex)
        val newDuration = newConfig.levels[safeIndex].durationSeconds
        val newState = current.copy(
            config = newConfig,
            currentLevelIndex = safeIndex,
            remainingSeconds = current.remainingSeconds.coerceAtMost(newDuration).coerceAtLeast(0),
        )
        markLocalMutation()
        _state.value = newState
        persist()
    }

    fun updateLevel(index: Int, level: BlindLevel) {
        updateConfig { config ->
            val mutable = config.levels.toMutableList()
            if (index in mutable.indices) mutable[index] = level
            config.copy(levels = mutable)
        }
    }

    fun addLevel() {
        updateConfig { config ->
            val last = config.levels.lastOrNull()
            val newLevel = BlindLevel(
                level = config.levels.size + 1,
                smallBlind = (last?.smallBlind ?: 100) * 2,
                bigBlind = (last?.bigBlind ?: 200) * 2,
                ante = last?.ante ?: 0,
                durationSeconds = last?.durationSeconds ?: 1200,
                isBreak = false,
            )
            config.copy(levels = config.levels + newLevel)
        }
    }

    fun removeLevel(index: Int) {
        val current = _state.value
        val levels = current.config.levels
        if (levels.size <= 1 || index !in levels.indices) return

        val updatedLevels = levels.toMutableList().also { it.removeAt(index) }
        val adjustedIndex = when {
            index < current.currentLevelIndex -> current.currentLevelIndex - 1
            current.currentLevelIndex > updatedLevels.lastIndex -> updatedLevels.lastIndex
            else -> current.currentLevelIndex
        }

        markLocalMutation()
        _state.value = current.copy(
            config = current.config.copy(levels = renumberLevels(updatedLevels)),
            currentLevelIndex = adjustedIndex,
            remainingSeconds = current.remainingSeconds
                .coerceAtMost(updatedLevels[adjustedIndex].durationSeconds)
                .coerceAtLeast(0),
        )
        persist()
    }

    fun updatePayoutPercents(percents: List<Int>) {
        updateConfig { it.copy(payoutPercents = percents) }
    }

    private fun persist() {
        viewModelScope.launch { repository.save(_state.value) }
    }

    private fun markLocalMutation() {
        if (!restoredInitialState) localChangesBeforeRestore = true
    }

    private fun normalizePlayerPlacements(state: TournamentState): TournamentState {
        val activeCount = state.players.count { !it.isEliminated }
        val maxPlacement = state.players.mapNotNull { it.placement }.maxOrNull() ?: activeCount
        var nextPlacement = maxPlacement + 1
        val placementsById = mutableMapOf<String, Int>()

        state.players.forEach { player ->
            if (player.isEliminated) {
                val placement = player.placement ?: nextPlacement++
                placementsById[player.id] = placement
            }
        }

        return state.copy(
            players = state.players.map { player ->
                if (player.isEliminated) {
                    player.copy(placement = placementsById[player.id])
                } else {
                    player.copy(placement = null)
                }
            }
        )
    }

    private fun renumberLevels(levels: List<BlindLevel>): List<BlindLevel> =
        levels.mapIndexed { index, level -> level.copy(level = index + 1) }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
        soundPlayer.release()
    }
}
