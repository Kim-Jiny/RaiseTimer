package com.jiny.raisetimer.domain

import com.jiny.raisetimer.domain.model.TournamentState

/**
 * Pure, stateless transformations on [TournamentState]. The engine is driven entirely by
 * wall-clock deltas supplied by the caller so it survives backgrounding and process restarts:
 * the UI layer calls [catchUp] on every tick and on app resume, passing the current epoch
 * millis. No internal time source — keeps the engine unit-testable and side-effect free.
 */
object TimerEngine {

    data class TickResult(
        val state: TournamentState,
        val levelAdvanced: Boolean,
        val finished: Boolean,
    )

    /**
     * Apply wall-clock delta between [TournamentState.lastTickAt] and [nowMillis]. Handles
     * the common 1-second foreground tick as well as multi-second/multi-level catch-up after
     * a background or cold start. Sub-second remainder is preserved in the new [lastTickAt]
     * so repeated calls stay phase-aligned.
     */
    fun catchUp(state: TournamentState, nowMillis: Long): TickResult {
        if (!state.isRunning) return TickResult(state, levelAdvanced = false, finished = false)
        val anchor = state.lastTickAt ?: return TickResult(
            state.copy(lastTickAt = nowMillis),
            levelAdvanced = false,
            finished = false,
        )

        val deltaSeconds = ((nowMillis - anchor) / 1000L).toInt()
        if (deltaSeconds <= 0) return TickResult(state, levelAdvanced = false, finished = false)

        var budget = deltaSeconds
        var currentIndex = state.currentLevelIndex
        var remaining = state.remainingSeconds
        var advanced = false
        var finished = false

        while (budget > 0) {
            if (remaining > budget) {
                remaining -= budget
                budget = 0
            } else {
                budget -= remaining
                remaining = 0
                val nextIndex = currentIndex + 1
                if (nextIndex > state.config.levels.lastIndex) {
                    advanced = true
                    finished = true
                    break
                }
                currentIndex = nextIndex
                remaining = state.config.levels[nextIndex].durationSeconds
                advanced = true
            }
        }

        val consumedMillis = deltaSeconds * 1000L
        val newState = state.copy(
            currentLevelIndex = currentIndex,
            remainingSeconds = remaining,
            isRunning = !finished,
            lastTickAt = if (finished) null else anchor + consumedMillis,
        )
        return TickResult(newState, levelAdvanced = advanced, finished = finished)
    }

    fun start(state: TournamentState, nowMillis: Long): TournamentState =
        state.copy(isRunning = true, lastTickAt = nowMillis)

    fun pause(state: TournamentState): TournamentState =
        state.copy(isRunning = false, lastTickAt = null)

    fun reset(state: TournamentState): TournamentState = state.copy(
        currentLevelIndex = 0,
        remainingSeconds = state.config.levels.first().durationSeconds,
        isRunning = false,
        lastTickAt = null,
    )

    fun nextLevel(state: TournamentState, nowMillis: Long): TournamentState {
        val nextIndex = (state.currentLevelIndex + 1).coerceAtMost(state.config.levels.lastIndex)
        return state.copy(
            currentLevelIndex = nextIndex,
            remainingSeconds = state.config.levels[nextIndex].durationSeconds,
            // If running, reset the tick anchor so the fresh level starts counting from now.
            lastTickAt = if (state.isRunning) nowMillis else null,
        )
    }

    fun previousLevel(state: TournamentState, nowMillis: Long): TournamentState {
        val prevIndex = (state.currentLevelIndex - 1).coerceAtLeast(0)
        return state.copy(
            currentLevelIndex = prevIndex,
            remainingSeconds = state.config.levels[prevIndex].durationSeconds,
            lastTickAt = if (state.isRunning) nowMillis else null,
        )
    }
}
