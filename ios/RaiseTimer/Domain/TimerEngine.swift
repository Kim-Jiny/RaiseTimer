import Foundation

/// Pure transformations on `TournamentState`. Driven by wall-clock deltas supplied by the
/// caller so it survives backgrounding and app restarts. Mirrors the Android `TimerEngine`.
enum TimerEngine {
    struct TickResult {
        var state: TournamentState
        var levelAdvanced: Bool
        var finished: Bool
    }

    /// Apply wall-clock delta between `state.lastTickAt` and `now`. Handles normal 1-second
    /// foreground ticks as well as multi-second/multi-level catch-up after a background or
    /// cold start. Sub-second remainder is preserved in the returned `lastTickAt`.
    static func catchUp(_ state: TournamentState, now: Double) -> TickResult {
        guard state.isRunning else {
            return TickResult(state: state, levelAdvanced: false, finished: false)
        }
        guard let anchor = state.lastTickAt else {
            var copy = state
            copy.lastTickAt = now
            return TickResult(state: copy, levelAdvanced: false, finished: false)
        }

        let deltaSeconds = Int((now - anchor))
        if deltaSeconds <= 0 {
            return TickResult(state: state, levelAdvanced: false, finished: false)
        }

        var budget = deltaSeconds
        var currentIndex = state.currentLevelIndex
        var remaining = state.remainingSeconds
        var advanced = false
        var finished = false

        while budget > 0 {
            if remaining > budget {
                remaining -= budget
                budget = 0
            } else {
                budget -= remaining
                remaining = 0
                let nextIndex = currentIndex + 1
                if nextIndex > state.config.levels.count - 1 {
                    advanced = true
                    finished = true
                    break
                }
                currentIndex = nextIndex
                remaining = state.config.levels[nextIndex].durationSeconds
                advanced = true
            }
        }

        var copy = state
        copy.currentLevelIndex = currentIndex
        copy.remainingSeconds = remaining
        copy.isRunning = !finished
        copy.lastTickAt = finished ? nil : anchor + Double(deltaSeconds)
        return TickResult(state: copy, levelAdvanced: advanced, finished: finished)
    }

    static func start(_ state: TournamentState, now: Double) -> TournamentState {
        var copy = state
        copy.isRunning = true
        copy.lastTickAt = now
        return copy
    }

    static func pause(_ state: TournamentState) -> TournamentState {
        var copy = state
        copy.isRunning = false
        copy.lastTickAt = nil
        return copy
    }

    static func reset(_ state: TournamentState) -> TournamentState {
        var copy = state
        copy.currentLevelIndex = 0
        copy.remainingSeconds = state.config.levels.first?.durationSeconds ?? 0
        copy.isRunning = false
        copy.lastTickAt = nil
        return copy
    }

    static func nextLevel(_ state: TournamentState, now: Double) -> TournamentState {
        var copy = state
        let clamped = min(state.currentLevelIndex + 1, state.config.levels.count - 1)
        copy.currentLevelIndex = clamped
        copy.remainingSeconds = state.config.levels[clamped].durationSeconds
        // If running, reset the tick anchor so the fresh level starts counting from now.
        copy.lastTickAt = state.isRunning ? now : nil
        return copy
    }

    static func previousLevel(_ state: TournamentState, now: Double) -> TournamentState {
        var copy = state
        let clamped = max(state.currentLevelIndex - 1, 0)
        copy.currentLevelIndex = clamped
        copy.remainingSeconds = state.config.levels[clamped].durationSeconds
        copy.lastTickAt = state.isRunning ? now : nil
        return copy
    }
}
