import Foundation
import Observation

/// Single source of truth for the tournament on iOS. Mirrors Android's `TournamentViewModel`.
@Observable
final class TournamentStore {
    private(set) var state: TournamentState
    private(set) var tournamentSlots: [TournamentSlotSummary] = []
    private(set) var currentTournamentID: UUID
    private(set) var currentTournamentName: String

    private var storage: TournamentAppStorage
    private var timerTask: Task<Void, Never>?
    private let soundPlayer = SoundPlayer()
    private static let storageKey = "raise_timer.state.v1"

    init() {
        let loadedStorage: TournamentAppStorage = {
            guard let data = UserDefaults.standard.data(forKey: Self.storageKey) else {
                return .default()
            }
            if let decoded = try? JSONDecoder().decode(TournamentAppStorage.self, from: data) {
                return decoded
            }
            if let legacy = try? JSONDecoder().decode(TournamentState.self, from: data) {
                let slot = TournamentSlotSnapshot(
                    name: String(localized: "default_tournament_name"),
                    updatedAt: Date(),
                    state: legacy
                )
                return TournamentAppStorage(currentTournamentID: slot.id, tournaments: [slot])
            }
            return .default()
        }()

        self.storage = loadedStorage
        let currentSlot = loadedStorage.tournaments.first(where: { $0.id == loadedStorage.currentTournamentID })
            ?? loadedStorage.tournaments.first
            ?? TournamentSlotSnapshot(name: String(localized: "default_tournament_name"), updatedAt: Date(), state: TournamentState())
        self.currentTournamentID = currentSlot.id
        self.currentTournamentName = currentSlot.name
        self.state = currentSlot.state

        migrateLegacyLogoIfNeeded()
        self.state = normalizePlayerPlacements(self.state)
        if self.state.isRunning, self.state.lastTickAt != nil {
            let result = TimerEngine.catchUp(self.state, now: Date().timeIntervalSince1970)
            self.state = result.state
            if result.levelAdvanced { soundPlayer.playLevelChange() }
            persist()
            if !result.finished { launchTickerLoop() }
        } else {
            self.state.isRunning = false
            self.state.lastTickAt = nil
        }
        publishSlotMetadata()
    }

    func catchUpFromBackground() {
        guard state.isRunning, state.lastTickAt != nil else { return }
        let before = state
        let result = TimerEngine.catchUp(state, now: Date().timeIntervalSince1970)
        guard result.state != before else { return }
        state = result.state
        if result.levelAdvanced { soundPlayer.playLevelChange() }
        persist()
        if result.finished {
            stopTicker()
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
        stopTicker()
        timerTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard let self, self.state.isRunning, !Task.isCancelled else { return }
                let prev = self.state.remainingSeconds
                let previousLevelIndex = self.state.currentLevelIndex
                let result = TimerEngine.catchUp(self.state, now: Date().timeIntervalSince1970)
                self.state = result.state

                if !result.levelAdvanced, self.state.currentLevelIndex == previousLevelIndex {
                    if prev > 60, result.state.remainingSeconds <= 60 {
                        self.soundPlayer.playMinuteWarning()
                    }
                    if prev > 10, result.state.remainingSeconds <= 10 {
                        self.soundPlayer.playFinalCountdownWarning()
                    }
                    if result.state.remainingSeconds > 0,
                       result.state.remainingSeconds <= 5,
                       result.state.remainingSeconds < prev {
                        self.soundPlayer.playCountdownTick()
                    }
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
        stopTicker()
        state = TimerEngine.pause(state)
        persist()
    }

    func reset() {
        stopTicker()
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

    func saveCurrentBlindStructure(name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        updateConfig { cfg in
            let preset = BlindStructurePreset(
                name: trimmed,
                startingStack: cfg.startingStack,
                buyInAmount: cfg.buyInAmount,
                feePerEntry: cfg.feePerEntry,
                rebuyAllowed: cfg.rebuyAllowed,
                levels: cfg.levels.enumerated().map { index, level in
                    var copy = level
                    copy.id = UUID()
                    copy.level = index + 1
                    return copy
                },
                payoutPercents: cfg.payoutPercents
            )
            cfg.savedBlindStructures.append(preset)
        }
    }

    func loadBlindStructure(id: UUID) {
        guard let preset = state.config.savedBlindStructures.first(where: { $0.id == id }) else { return }
        stopTicker()
        state.config.startingStack = preset.startingStack
        state.config.buyInAmount = preset.buyInAmount
        state.config.feePerEntry = preset.feePerEntry
        state.config.rebuyAllowed = preset.rebuyAllowed
        state.config.levels = preset.levels.enumerated().map { index, level in
            var copy = level
            copy.id = UUID()
            copy.level = index + 1
            return copy
        }
        state.config.payoutPercents = preset.payoutPercents
        state.currentLevelIndex = 0
        state.remainingSeconds = state.config.levels.first?.durationSeconds ?? 0
        state.isRunning = false
        state.lastTickAt = nil
        persist()
    }

    func deleteBlindStructure(id: UUID) {
        updateConfig { cfg in
            cfg.savedBlindStructures.removeAll { $0.id == id }
        }
    }

    func updateThemePreset(_ preset: ThemePreset) {
        updateConfig { cfg in cfg.themePreset = preset }
    }

    func updateFullscreenLogoFileName(_ fileName: String?) {
        let previousFileName = state.config.fullscreenLogoFileName
        if previousFileName != nil, previousFileName != fileName {
            LogoStore.delete(fileName: previousFileName)
        }
        updateConfig {
            $0.fullscreenLogoFileName = fileName
            $0.fullscreenLogoBase64 = nil
        }
    }

    // MARK: - Tournament Slots

    func createTournamentSlot(name: String) {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        stopTicker()
        let config = state.config
        var newConfig = TournamentConfig()
        newConfig.themePreset = config.themePreset
        newConfig.fullscreenLogoFileName = config.fullscreenLogoFileName
        newConfig.savedBlindStructures = config.savedBlindStructures
        let newState = TournamentState(config: newConfig)
        let slot = TournamentSlotSnapshot(
            name: trimmed,
            updatedAt: Date(),
            state: newState
        )
        storage.currentTournamentID = slot.id
        storage.tournaments.append(slot)
        currentTournamentID = slot.id
        currentTournamentName = slot.name
        state = newState
        persist()
    }

    func switchTournamentSlot(id: UUID) {
        guard id != currentTournamentID,
              let target = storage.tournaments.first(where: { $0.id == id }) else { return }
        stopTicker()
        if let currentIndex = storage.tournaments.firstIndex(where: { $0.id == currentTournamentID }) {
            storage.tournaments[currentIndex] = currentSlotSnapshot()
        }
        storage.currentTournamentID = id
        currentTournamentID = id
        currentTournamentName = target.name
        state = normalizePlayerPlacements(
            target.state.withStoppedTimer().withMigratedLogo(using: LogoStore.migrateLegacyBase64)
        )
        persist()
    }

    func deleteTournamentSlot(id: UUID) {
        guard storage.tournaments.count > 1 else { return }
        storage.tournaments.removeAll { $0.id == id }
        if currentTournamentID == id, let fallback = storage.tournaments.first {
            stopTicker()
            storage.currentTournamentID = fallback.id
            currentTournamentID = fallback.id
            currentTournamentName = fallback.name
            state = normalizePlayerPlacements(
                fallback.state
                    .withStoppedTimer()
                    .withMigratedLogo(using: LogoStore.migrateLegacyBase64)
            )
        }
        persist()
    }

    // MARK: - Persistence

    private func persist() {
        if let currentIndex = storage.tournaments.firstIndex(where: { $0.id == currentTournamentID }) {
            storage.tournaments[currentIndex] = currentSlotSnapshot()
        } else {
            storage.tournaments.append(currentSlotSnapshot())
            storage.currentTournamentID = currentTournamentID
        }
        publishSlotMetadata()
        if let data = try? JSONEncoder().encode(storage) {
            UserDefaults.standard.set(data, forKey: Self.storageKey)
        }
    }

    private func stopTicker() {
        timerTask?.cancel()
        timerTask = nil
    }

    private func publishSlotMetadata() {
        tournamentSlots = storage.tournaments
            .map {
                TournamentSlotSummary(
                    id: $0.id,
                    name: $0.name,
                    updatedAt: $0.updatedAt,
                    playerCount: $0.state.players.count,
                    activePlayerCount: $0.state.activePlayers.count,
                    isCurrent: $0.id == currentTournamentID
                )
            }
            .sorted { $0.updatedAt > $1.updatedAt }
        currentTournamentName = storage.tournaments.first(where: { $0.id == currentTournamentID })?.name ?? String(localized: "default_tournament_name")
    }

    private func currentSlotSnapshot() -> TournamentSlotSnapshot {
        TournamentSlotSnapshot(
            id: currentTournamentID,
            name: currentTournamentName,
            updatedAt: Date(),
            state: state
        )
    }

    private func migrateLegacyLogoIfNeeded() {
        guard state.config.fullscreenLogoFileName == nil,
              state.config.fullscreenLogoBase64 != nil else { return }
        state.config.fullscreenLogoFileName = LogoStore.migrateLegacyBase64(state.config.fullscreenLogoBase64)
        state.config.fullscreenLogoBase64 = nil
        persist()
    }

    private func normalizePlayerPlacements(_ state: TournamentState) -> TournamentState {
        let activeCount = state.players.filter { !$0.isEliminated }.count
        var nextPlacement = activeCount + 1

        var existingPlacements = Set<Int>()
        for player in state.players where player.isEliminated && player.placement != nil {
            existingPlacements.insert(player.placement!)
        }

        var placementsById: [UUID: Int] = [:]
        for player in state.players where player.isEliminated {
            if let existing = player.placement {
                placementsById[player.id] = existing
            } else {
                while existingPlacements.contains(nextPlacement) {
                    nextPlacement += 1
                }
                placementsById[player.id] = nextPlacement
                existingPlacements.insert(nextPlacement)
                nextPlacement += 1
            }
        }

        var normalized = state
        normalized.players = state.players.map { player in
            var copy = player
            if player.isEliminated {
                copy.placement = placementsById[player.id]
            } else if activeCount == 1 {
                copy.placement = 1
            } else {
                copy.placement = nil
            }
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

private extension TournamentState {
    func withStoppedTimer() -> TournamentState {
        var copy = self
        copy.isRunning = false
        copy.lastTickAt = nil
        return copy
    }

    func withMigratedLogo(using migrate: (String?) -> String?) -> TournamentState {
        guard config.fullscreenLogoFileName == nil,
              config.fullscreenLogoBase64 != nil else { return self }
        var copy = self
        copy.config.fullscreenLogoFileName = migrate(copy.config.fullscreenLogoBase64)
        copy.config.fullscreenLogoBase64 = nil
        return copy
    }
}
