import Foundation

enum PayoutCalculator {
    struct Payout: Identifiable, Hashable {
        var id: Int { place }
        var place: Int
        var amount: Int
    }

    static func calculate(_ state: TournamentState) -> [Payout] {
        let percents = state.config.payoutPercents
        guard !percents.isEmpty else { return [] }

        let pool = state.totalPrizePool
        if pool <= 0 {
            return percents.indices.map { Payout(place: $0 + 1, amount: 0) }
        }

        let sum: Int = max(percents.reduce(0, +), 1)
        let poolLong: Int64 = Int64(pool)
        let sumLong: Int64 = Int64(sum)
        var rawAmounts: [Int] = []
        rawAmounts.reserveCapacity(percents.count)
        for pct in percents {
            let amount: Int64 = (poolLong * Int64(pct)) / sumLong
            rawAmounts.append(Int(amount))
        }
        let distributed: Int = rawAmounts.reduce(0, +)
        let remainder: Int = pool - distributed

        return rawAmounts.enumerated().map { index, amount in
            let adjusted = index == 0 ? amount + remainder : amount
            return Payout(place: index + 1, amount: adjusted)
        }
    }
}
