import Foundation

struct BlindLevel: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var level: Int
    var smallBlind: Int
    var bigBlind: Int
    var ante: Int
    var durationSeconds: Int
    var isBreak: Bool

    init(
        id: UUID = UUID(),
        level: Int,
        smallBlind: Int,
        bigBlind: Int,
        ante: Int = 0,
        durationSeconds: Int,
        isBreak: Bool = false
    ) {
        self.id = id
        self.level = level
        self.smallBlind = smallBlind
        self.bigBlind = bigBlind
        self.ante = ante
        self.durationSeconds = durationSeconds
        self.isBreak = isBreak
    }

    static func breakLevel(level: Int, durationSeconds: Int) -> BlindLevel {
        BlindLevel(
            level: level,
            smallBlind: 0,
            bigBlind: 0,
            ante: 0,
            durationSeconds: durationSeconds,
            isBreak: true
        )
    }
}

struct Player: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var name: String
    var rebuyCount: Int = 0
    var isEliminated: Bool = false
    var placement: Int?
}

struct TournamentConfig: Codable, Hashable {
    var startingStack: Int = 10_000
    var buyInAmount: Int = 50_000
    /// Per-entry house fee. Subtracted from each buy-in/rebuy before prize pool.
    var feePerEntry: Int = 0
    var rebuyAllowed: Bool = true
    var levels: [BlindLevel] = TournamentConfig.defaultBlindStructure()
    var payoutPercents: [Int] = [50, 30, 20]

    static func defaultBlindStructure() -> [BlindLevel] {
        [
            BlindLevel(level: 1, smallBlind: 100, bigBlind: 200, durationSeconds: 1200),
            BlindLevel(level: 2, smallBlind: 200, bigBlind: 400, durationSeconds: 1200),
            BlindLevel(level: 3, smallBlind: 300, bigBlind: 600, durationSeconds: 1200),
            BlindLevel(level: 4, smallBlind: 400, bigBlind: 800, durationSeconds: 1200),
            BlindLevel.breakLevel(level: 5, durationSeconds: 600),
            BlindLevel(level: 6, smallBlind: 500, bigBlind: 1_000, ante: 100, durationSeconds: 1200),
            BlindLevel(level: 7, smallBlind: 700, bigBlind: 1_400, ante: 200, durationSeconds: 1200),
            BlindLevel(level: 8, smallBlind: 1_000, bigBlind: 2_000, ante: 300, durationSeconds: 1200),
            BlindLevel(level: 9, smallBlind: 1_500, bigBlind: 3_000, ante: 400, durationSeconds: 1200),
            BlindLevel(level: 10, smallBlind: 2_000, bigBlind: 4_000, ante: 500, durationSeconds: 1200),
            BlindLevel(level: 11, smallBlind: 3_000, bigBlind: 6_000, ante: 1_000, durationSeconds: 1200),
            BlindLevel(level: 12, smallBlind: 5_000, bigBlind: 10_000, ante: 1_000, durationSeconds: 1200),
            BlindLevel(level: 13, smallBlind: 8_000, bigBlind: 16_000, ante: 2_000, durationSeconds: 1200),
        ]
    }
}

struct TournamentState: Codable, Hashable {
    var config: TournamentConfig = TournamentConfig()
    var currentLevelIndex: Int = 0
    var remainingSeconds: Int
    var isRunning: Bool = false
    var players: [Player] = []
    /// Wall-clock epoch seconds (as Double) of the last applied tick. Non-nil only while
    /// `isRunning` is true. Used by `TimerEngine.catchUp` to recover elapsed time after
    /// backgrounding or a cold launch.
    var lastTickAt: Double?

    init(config: TournamentConfig = TournamentConfig()) {
        self.config = config
        self.remainingSeconds = config.levels.first?.durationSeconds ?? 0
        self.lastTickAt = nil
    }

    var currentLevel: BlindLevel {
        let clamped = max(0, min(currentLevelIndex, config.levels.count - 1))
        return config.levels[clamped]
    }

    var nextLevel: BlindLevel? {
        let next = currentLevelIndex + 1
        return next < config.levels.count ? config.levels[next] : nil
    }

    var activePlayers: [Player] {
        players.filter { !$0.isEliminated }
    }

    var totalBuyInsAndRebuys: Int {
        players.reduce(0) { $0 + 1 + $1.rebuyCount }
    }

    /// Gross collected from entries before deducting house fee.
    var totalBuyInsGross: Int {
        totalBuyInsAndRebuys * config.buyInAmount
    }

    /// Total fee taken by the house.
    var totalFee: Int {
        totalBuyInsAndRebuys * config.feePerEntry
    }

    /// Net prize pool distributed to winners.
    var totalPrizePool: Int {
        max(0, totalBuyInsGross - totalFee)
    }
}
