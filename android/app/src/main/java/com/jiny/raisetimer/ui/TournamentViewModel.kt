package com.jiny.raisetimer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jiny.raisetimer.R
import com.jiny.raisetimer.data.TournamentRepository
import com.jiny.raisetimer.domain.TimerEngine
import com.jiny.raisetimer.domain.model.BlindLevel
import com.jiny.raisetimer.domain.model.BlindStructurePreset
import com.jiny.raisetimer.domain.model.Player
import com.jiny.raisetimer.domain.model.ThemePreset
import com.jiny.raisetimer.domain.model.TournamentAppStorage
import com.jiny.raisetimer.domain.model.TournamentConfig
import com.jiny.raisetimer.domain.model.TournamentSlotSnapshot
import com.jiny.raisetimer.domain.model.TournamentSlotSummary
import com.jiny.raisetimer.domain.model.TournamentState
import com.jiny.raisetimer.sound.SoundPlayer
import com.jiny.raisetimer.ui.LogoStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Single source of truth for the tournament. All four screens subscribe to [state].
 */
class TournamentViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TournamentRepository(app)
    private val soundPlayer = SoundPlayer(app)

    private val _state = MutableStateFlow(TournamentState())
    val state: StateFlow<TournamentState> = _state.asStateFlow()
    private val _slotSummaries = MutableStateFlow<List<TournamentSlotSummary>>(emptyList())
    val slotSummaries: StateFlow<List<TournamentSlotSummary>> = _slotSummaries.asStateFlow()
    private val _currentTournamentName = MutableStateFlow(app.getString(R.string.default_tournament_name))
    val currentTournamentName: StateFlow<String> = _currentTournamentName.asStateFlow()
    private var appStorage = TournamentAppStorage.default(app.getString(R.string.default_tournament_name))

    private var tickJob: Job? = null
    private var restoredInitialState = false
    private var localChangesBeforeRestore = false

    init {
        viewModelScope.launch {
            val restoredStorage = repository.storageFlow.first()
            appStorage = restoredStorage
            publishSlotMetadata()
            val restored = restoredStorage.tournaments
                .firstOrNull { it.id == restoredStorage.currentTournamentId }
                ?.state
                ?: TournamentState()
            if (!localChangesBeforeRestore) {
                val migratedConfig = migrateLegacyLogo(restored.config)
                val normalized = normalizePlayerPlacements(restored.copy(config = migratedConfig))
                if (migratedConfig != restored.config) {
                    _state.value = normalized
                    persist()
                }
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
                publishSlotMetadata()
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
                val previousLevelIndex = _state.value.currentLevelIndex
                _state.value = result.state
                // Countdown beep only fires on a smooth 1-second foreground tick.
                if (!result.levelAdvanced && result.state.currentLevelIndex == previousLevelIndex) {
                    if (prevRemaining > 60 && result.state.remainingSeconds <= 60) {
                        soundPlayer.playMinuteWarning()
                    }
                    if (prevRemaining > 10 && result.state.remainingSeconds <= 10) {
                        soundPlayer.playFinalCountdownWarning()
                    }
                    if (result.state.remainingSeconds in 1..5 &&
                        result.state.remainingSeconds < prevRemaining
                    ) {
                        soundPlayer.playCountdownTick()
                    }
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
        stopTicker()
        markLocalMutation()
        _state.value = TimerEngine.pause(_state.value)
        persist()
    }

    fun reset() {
        stopTicker()
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

    fun saveCurrentBlindStructure(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateConfig { config ->
            val preset = BlindStructurePreset(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                startingStack = config.startingStack,
                buyInAmount = config.buyInAmount,
                feePerEntry = config.feePerEntry,
                rebuyAllowed = config.rebuyAllowed,
                levels = config.levels.mapIndexed { index, level ->
                    level.copy(
                        id = UUID.randomUUID().toString(),
                        level = index + 1,
                    )
                },
                payoutPercents = config.payoutPercents.toList(),
            )
            config.copy(savedBlindStructures = config.savedBlindStructures + preset)
        }
    }

    fun loadBlindStructure(presetId: String) {
        val preset = _state.value.config.savedBlindStructures.firstOrNull { it.id == presetId } ?: return
        stopTicker()
        markLocalMutation()
        _state.value = _state.value.copy(
            config = _state.value.config.copy(
                startingStack = preset.startingStack,
                buyInAmount = preset.buyInAmount,
                feePerEntry = preset.feePerEntry,
                rebuyAllowed = preset.rebuyAllowed,
                levels = preset.levels.mapIndexed { index, level ->
                    level.copy(
                        id = UUID.randomUUID().toString(),
                        level = index + 1,
                    )
                },
                payoutPercents = preset.payoutPercents.toList(),
            ),
            currentLevelIndex = 0,
            remainingSeconds = preset.levels.firstOrNull()?.durationSeconds ?: 0,
            isRunning = false,
            lastTickAt = null,
        )
        persist()
    }

    fun deleteBlindStructure(presetId: String) {
        updateConfig { config ->
            config.copy(
                savedBlindStructures = config.savedBlindStructures.filterNot { it.id == presetId }
            )
        }
    }

    fun createTournamentSlot(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        markLocalMutation()
        val config = _state.value.config
        val newState = TournamentState(
            config = TournamentConfig(
                themePreset = config.themePreset,
                fullscreenLogoFileName = config.fullscreenLogoFileName,
                savedBlindStructures = config.savedBlindStructures,
            )
        )
        val snapshot = TournamentSlotSnapshot(
            name = trimmed,
            updatedAt = System.currentTimeMillis(),
            state = newState,
        )
        appStorage = appStorage.copy(
            currentTournamentId = snapshot.id,
            tournaments = appStorage.tournaments + snapshot,
        )
        _state.value = newState
        stopTicker()
        persist()
    }

    fun switchTournamentSlot(slotId: String) {
        if (slotId == appStorage.currentTournamentId) return
        val target = appStorage.tournaments.firstOrNull { it.id == slotId } ?: return
        stopTicker()
        val updatedCurrent = currentSlotSnapshot()
        appStorage = appStorage.copy(
            currentTournamentId = slotId,
            tournaments = appStorage.tournaments.map { snapshot ->
                when (snapshot.id) {
                    updatedCurrent.id -> updatedCurrent
                    else -> snapshot
                }
            }
        )
        val migratedConfig = migrateLegacyLogo(target.state.config)
        _state.value = normalizePlayerPlacements(
            target.state.copy(config = migratedConfig, isRunning = false, lastTickAt = null)
        )
        persist()
    }

    fun deleteTournamentSlot(slotId: String) {
        if (appStorage.tournaments.size <= 1) return
        val remaining = appStorage.tournaments.filterNot { it.id == slotId }
        if (slotId == appStorage.currentTournamentId) {
            stopTicker()
            val fallback = remaining.first()
            val migratedConfig = migrateLegacyLogo(fallback.state.config)
            appStorage = appStorage.copy(
                currentTournamentId = fallback.id,
                tournaments = remaining,
            )
            _state.value = normalizePlayerPlacements(
                fallback.state.copy(config = migratedConfig, isRunning = false, lastTickAt = null)
            )
        } else {
            appStorage = appStorage.copy(tournaments = remaining)
        }
        persist()
    }

    fun updateThemePreset(themePreset: ThemePreset) {
        updateConfig { it.copy(themePreset = themePreset) }
    }

    fun updateFullscreenLogoFileName(fileName: String?) {
        val previousFileName = _state.value.config.fullscreenLogoFileName
        if (previousFileName != null && previousFileName != fileName) {
            LogoStorage.delete(getApplication(), previousFileName)
        }
        updateConfig {
            it.copy(
                fullscreenLogoFileName = fileName,
                fullscreenLogoBase64 = null,
            )
        }
    }

    private fun persist() {
        appStorage = appStorage.copy(
            tournaments = appStorage.tournaments
                .map { snapshot -> if (snapshot.id == appStorage.currentTournamentId) currentSlotSnapshot() else snapshot }
                .ifEmpty { listOf(currentSlotSnapshot()) }
        )
        publishSlotMetadata()
        viewModelScope.launch { repository.save(appStorage) }
    }

    private fun stopTicker() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun markLocalMutation() {
        if (!restoredInitialState) localChangesBeforeRestore = true
    }

    private fun normalizePlayerPlacements(state: TournamentState): TournamentState {
        val activeCount = state.players.count { !it.isEliminated }
        var nextPlacement = activeCount + 1

        val existingPlacements = state.players
            .filter { it.isEliminated && it.placement != null }
            .mapTo(mutableSetOf()) { it.placement!! }

        val placementsById = mutableMapOf<String, Int>()

        state.players.forEach { player ->
            if (player.isEliminated) {
                if (player.placement != null) {
                    placementsById[player.id] = player.placement
                } else {
                    while (nextPlacement in existingPlacements) {
                        nextPlacement++
                    }
                    placementsById[player.id] = nextPlacement
                    existingPlacements.add(nextPlacement)
                    nextPlacement++
                }
            }
        }

        return state.copy(
            players = state.players.map { player ->
                if (player.isEliminated) {
                    player.copy(placement = placementsById[player.id])
                } else if (activeCount == 1) {
                    player.copy(placement = 1)
                } else {
                    player.copy(placement = null)
                }
            }
        )
    }

    private fun renumberLevels(levels: List<BlindLevel>): List<BlindLevel> =
        levels.mapIndexed { index, level -> level.copy(level = index + 1) }

    private fun currentSlotSnapshot(): TournamentSlotSnapshot {
        val existing = appStorage.tournaments.firstOrNull { it.id == appStorage.currentTournamentId }
        return TournamentSlotSnapshot(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = existing?.name ?: _currentTournamentName.value,
            updatedAt = System.currentTimeMillis(),
            state = _state.value,
        )
    }

    private fun publishSlotMetadata() {
        _currentTournamentName.value =
            appStorage.tournaments.firstOrNull { it.id == appStorage.currentTournamentId }?.name
                ?: getApplication<Application>().getString(R.string.default_tournament_name)
        _slotSummaries.value = appStorage.tournaments.map { snapshot ->
            TournamentSlotSummary(
                id = snapshot.id,
                name = snapshot.name,
                updatedAt = snapshot.updatedAt,
                playerCount = snapshot.state.players.size,
                activePlayerCount = snapshot.state.activePlayers.size,
                isCurrent = snapshot.id == appStorage.currentTournamentId,
            )
        }.sortedByDescending { it.updatedAt }
    }

    private fun migrateLegacyLogo(config: TournamentConfig): TournamentConfig {
        if (config.fullscreenLogoFileName != null || config.fullscreenLogoBase64 == null) return config
        val fileName = LogoStorage.migrateLegacyBase64(getApplication(), config.fullscreenLogoBase64)
        return config.copy(
            fullscreenLogoFileName = fileName,
            fullscreenLogoBase64 = null,
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopTicker()
        soundPlayer.release()
    }
}
