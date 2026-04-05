import Foundation
import Observation

/// Single source of truth for the tournament on iOS. Mirrors Android's `TournamentViewModel`.
@Observable
final class TournamentStore {
    private(set) var state: TournamentState

    private var timerTask: Task<Void, Never>?
    private let soundPlayer = SoundPlayer()
    private let storageKey = "raise_timer.state.v1"

    init() {
        if let data = UserDefaults.standard.data(forKey: storageKey),
           let decoded = try? JSONDecoder().decode(TournamentState.self, from: data) {
            self.state = decoded
        } else {
            self.state = TournamentState()
        }
        // All stored properties initialized — safe to call instance methods.
        self.state = normalizePlayerPlacements(self.state)
        if self.state.isRunning, self.state.lastTickAt != nil {
            // Catch up any wall-clock time that passed while the process was dead.
            let result = TimerEngine.catchUp(self.state, now: Date().timeIntervalSince1970)
            self.state = result.state
            if result.levelAdvanced { soundPlayer.playLevelChange() }
            persist()
            if !result.finished { launchTickerLoop() }
        } else {
            self.state.isRunning = false
            self.state.lastTickAt = nil
        }
    }

    /// Re-sync the running timer against wall-clock time when the app returns to the
    /// foreground. Called from `RaiseTimerApp.scenePhase`.
    func catchUpFromBackground() {
        guard state.isRunning, state.lastTickAt != nil else { return }
        let before = state
        let result = TimerEngine.catchUp(state, now: Date().timeIntervalSince1970)
        guard result.state != before else { return }
        state = result.state
        if result.levelAdvanced { soundPlayer.playLevelChange() }
        persist()
        if result.finished {
            timerTask?.cancel()
            timerTask = nil
        } else if timerTask == nil {
            launchTickerLoop()
        }
    }

    // MARK: - Timer

    func toggleRunning() {
        state.isRunning ? pause() : start()
    }

    func start() {
        guard !state.isRunning else { return }
        state = TimerEngine.start(state, now: Date().timeIntervalSince1970)
        persist()
        launchTickerLoop()
    }

    private func launchTickerLoop() {
        timerTask?.cancel()
        timerTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard let self, self.state.isRunning, !Task.isCancelled else { return }
                let prev = self.state.remainingSeconds
                let result = TimerEngine.catchUp(self.state, now: Date().timeIntervalSince1970)
                self.state = result.state
                // Countdown beep only on smooth foreground tick (single-second decrement).
                if !result.levelAdvanced,
                   result.state.remainingSeconds > 0,
                   result.state.remainingSeconds <= 5,
                   result.state.remainingSeconds < prev {
                    self.soundPlayer.playCountdownTick()
                }
                if result.levelAdvanced {
                    self.soundPlayer.playLevelChange()
                    self.persist()
                    if result.finished { return }
                }
            }
        }
    }

    func pause() {
        timerTask?.cancel()
        timerTask = nil
        state = TimerEngine.pause(state)
        persist()
    }

    func reset() {
        timerTask?.cancel()
        timerTask = nil
        state = TimerEngine.reset(state)
        persist()
    }

    func nextLevel() {
        state = TimerEngine.nextLevel(state, now: Date().timeIntervalSince1970)
        persist()
    }

    func previousLevel() {
        state = TimerEngine.previousLevel(state, now: Date().timeIntervalSince1970)
        persist()
    }

    // MARK: - Players

    func addPlayer(name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        state.players.append(Player(name: trimmed))
        state = normalizePlayerPlacements(state)
        persist()
    }

    func removePlayer(id: UUID) {
        state.players.removeAll { $0.id == id }
        state = normalizePlayerPlacements(state)
        persist()
    }

    func eliminatePlayer(id: UUID) {
        if let idx = state.players.firstIndex(where: { $0.id == id }), !state.players[idx].isEliminated {
            state.players[idx].isEliminated = true
            state = normalizePlayerPlacements(state)
            persist()
        }
    }

    func revivePlayer(id: UUID) {
        if let idx = state.players.firstIndex(where: { $0.id == id }) {
            state.players[idx].isEliminated = false
            state.players[idx].placement = nil
            state = normalizePlayerPlacements(state)
            persist()
        }
    }

    func adjustRebuy(id: UUID, delta: Int) {
        guard state.config.rebuyAllowed else { return }
        if let idx = state.players.firstIndex(where: { $0.id == id }) {
            state.players[idx].rebuyCount = max(0, state.players[idx].rebuyCount + delta)
            persist()
        }
    }

    // MARK: - Config

    func updateConfig(_ transform: (inout TournamentConfig) -> Void) {
        var cfg = state.config
        transform(&cfg)
        state.config = cfg
        let clamped = min(max(state.currentLevelIndex, 0), cfg.levels.count - 1)
        state.currentLevelIndex = clamped
        state.remainingSeconds = min(state.remainingSeconds, cfg.levels[clamped].durationSeconds)
        persist()
    }

    func updateLevel(at index: Int, with level: BlindLevel) {
        updateConfig { cfg in
            guard cfg.levels.indices.contains(index) else { return }
            cfg.levels[index] = level
        }
    }

    func addLevel() {
        updateConfig { cfg in
            let last = cfg.levels.last
            let new = BlindLevel(
                level: cfg.levels.count + 1,
                smallBlind: (last?.smallBlind ?? 100) * 2,
                bigBlind: (last?.bigBlind ?? 200) * 2,
                ante: last?.ante ?? 0,
                durationSeconds: last?.durationSeconds ?? 1200,
                isBreak: false
            )
            cfg.levels.append(new)
        }
    }

    func removeLevel(at index: Int) {
        guard state.config.levels.count > 1, state.config.levels.indices.contains(index) else { return }

        var levels = state.config.levels
        levels.remove(at: index)

        let lastIndex = levels.count - 1
        let adjustedIndex: Int
        if index < state.currentLevelIndex {
            adjustedIndex = state.currentLevelIndex - 1
        } else if state.currentLevelIndex > lastIndex {
            adjustedIndex = lastIndex
        } else {
            adjustedIndex = state.currentLevelIndex
        }

        state.config.levels = renumberLevels(levels)
        state.currentLevelIndex = adjustedIndex
        state.remainingSeconds = min(state.remainingSeconds, levels[adjustedIndex].durationSeconds)
        persist()
    }

    func updatePayoutPercents(_ percents: [Int]) {
        updateConfig { cfg in cfg.payoutPercents = percents }
    }

    // MARK: - Persistence

    private func persist() {
        if let data = try? JSONEncoder().encode(state) {
            UserDefaults.standard.set(data, forKey: storageKey)
        }
    }

    private func normalizePlayerPlacements(_ state: TournamentState) -> TournamentState {
        let activeCount = state.players.filter { !$0.isEliminated }.count
        let maxPlacement = state.players.compactMap(\.placement).max() ?? activeCount
        var nextPlacement = maxPlacement + 1
        var placementsById: [UUID: Int] = [:]

        for player in state.players where player.isEliminated {
            let placement = player.placement ?? nextPlacement
            placementsById[player.id] = placement
            if player.placement == nil {
                nextPlacement += 1
            }
        }

        var normalized = state
        normalized.players = state.players.map { player in
            var copy = player
            copy.placement = player.isEliminated ? placementsById[player.id] : nil
            return copy
        }
        return normalized
    }

    private func renumberLevels(_ levels: [BlindLevel]) -> [BlindLevel] {
        levels.enumerated().map { index, level in
            var copy = level
            copy.level = index + 1
            return copy
        }
    }

    deinit {
        timerTask?.cancel()
    }
}
