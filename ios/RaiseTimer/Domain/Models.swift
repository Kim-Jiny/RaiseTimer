import Foundation

enum ThemePreset: String, Codable, CaseIterable, Hashable {
    case emerald
    case ocean
    case ruby
    case sunset
    case lavender
    case slate
    case mint
    case coral
    case midnight

    var title: String {
        switch self {
        case .emerald: return "Emerald"
        case .ocean: return "Ocean"
        case .ruby: return "Ruby"
        case .sunset: return "Sunset"
        case .lavender: return "Lavender"
        case .slate: return "Slate"
        case .mint: return "Mint"
        case .coral: return "Coral"
        case .midnight: return "Midnight"
        }
    }
}

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

struct BlindStructurePreset: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var name: String
    var startingStack: Int
    var buyInAmount: Int
    var feePerEntry: Int
    var rebuyAllowed: Bool
    var levels: [BlindLevel]
    var payoutPercents: [Int]
}

struct TournamentSlotSnapshot: Codable, Identifiable, Hashable {
    var id: UUID = UUID()
    var name: String
    var updatedAt: Date
    var state: TournamentState
}

struct TournamentAppStorage: Codable, Hashable {
    var currentTournamentID: UUID
    var tournaments: [TournamentSlotSnapshot]

    static func `default`() -> TournamentAppStorage {
        let slot = TournamentSlotSnapshot(
            name: String(localized: "default_tournament_name"),
            updatedAt: Date(),
            state: TournamentState()
        )
        return TournamentAppStorage(
            currentTournamentID: slot.id,
            tournaments: [slot]
        )
    }
}

struct TournamentSlotSummary: Identifiable, Hashable {
    let id: UUID
    let name: String
    let updatedAt: Date
    let playerCount: Int
    let activePlayerCount: Int
    let isCurrent: Bool
}

struct TournamentConfig: Codable, Hashable {
    var startingStack: Int = 10_000
    var buyInAmount: Int = 50_000
    /// Per-entry house fee. Subtracted from each buy-in/rebuy before prize pool.
    var feePerEntry: Int = 0
    var rebuyAllowed: Bool = true
    var levels: [BlindLevel] = TournamentConfig.defaultBlindStructure()
    var payoutPercents: [Int] = [50, 30, 20]
    var themePreset: ThemePreset = .emerald
    var fullscreenLogoFileName: String? = nil
    var fullscreenLogoBase64: String? = nil
    var savedBlindStructures: [BlindStructurePreset] = []

    enum CodingKeys: String, CodingKey {
        case startingStack
        case buyInAmount
        case feePerEntry
        case rebuyAllowed
        case levels
        case payoutPercents
        case themePreset
        case fullscreenLogoFileName
        case fullscreenLogoBase64
        case savedBlindStructures
    }

    init() {}

    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        startingStack = try container.decodeIfPresent(Int.self, forKey: .startingStack) ?? 10_000
        buyInAmount = try container.decodeIfPresent(Int.self, forKey: .buyInAmount) ?? 50_000
        feePerEntry = try container.decodeIfPresent(Int.self, forKey: .feePerEntry) ?? 0
        rebuyAllowed = try container.decodeIfPresent(Bool.self, forKey: .rebuyAllowed) ?? true
        levels = try container.decodeIfPresent([BlindLevel].self, forKey: .levels) ?? TournamentConfig.defaultBlindStructure()
        payoutPercents = try container.decodeIfPresent([Int].self, forKey: .payoutPercents) ?? [50, 30, 20]
        themePreset = try container.decodeIfPresent(ThemePreset.self, forKey: .themePreset) ?? .emerald
        fullscreenLogoFileName = try container.decodeIfPresent(String.self, forKey: .fullscreenLogoFileName)
        fullscreenLogoBase64 = try container.decodeIfPresent(String.self, forKey: .fullscreenLogoBase64)
        savedBlindStructures = try container.decodeIfPresent([BlindStructurePreset].self, forKey: .savedBlindStructures) ?? []
    }

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
        guard !config.levels.isEmpty else {
            return BlindLevel(level: 1, smallBlind: 0, bigBlind: 0, durationSeconds: 0)
        }
        let clamped = max(0, min(currentLevelIndex, config.levels.count - 1))
        return config.levels[clamped]
    }

    var nextLevel: BlindLevel? {
        guard !config.levels.isEmpty else { return nil }
        let next = currentLevelIndex + 1
        return next < config.levels.count ? config.levels[next] : nil
    }

    var activePlayers: [Player] {
        players.filter { !$0.isEliminated }
    }

    var isTournamentComplete: Bool {
        !players.isEmpty && activePlayers.count <= 1
    }

    var winner: Player? {
        players.first(where: { $0.placement == 1 }) ?? activePlayers.only
    }

    var finalStandings: [Player] {
        players.sorted {
            let lhs = $0.placement ?? Int.max
            let rhs = $1.placement ?? Int.max
            if lhs == rhs { return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
            return lhs < rhs
        }
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

private extension Array {
    var only: Element? { count == 1 ? first : nil }
}
