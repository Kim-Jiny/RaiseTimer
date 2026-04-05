package com.jiny.raisetimer.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TournamentState(
    val config: TournamentConfig = TournamentConfig(),
    val currentLevelIndex: Int = 0,
    val remainingSeconds: Int = TournamentConfig().levels.first().durationSeconds,
    val isRunning: Boolean = false,
    val players: List<Player> = emptyList(),
    /**
     * Wall-clock epoch millis of the last applied tick. Non-null only while [isRunning] is
     * true. Used by [TimerEngine.catchUp] to recover elapsed time after backgrounding or a
     * process restart.
     */
    val lastTickAt: Long? = null,
) {
    val currentLevel: BlindLevel
        get() = config.levels[currentLevelIndex.coerceIn(0, config.levels.lastIndex)]

    val nextLevel: BlindLevel?
        get() = config.levels.getOrNull(currentLevelIndex + 1)

    val activePlayers: List<Player>
        get() = players.filter { !it.isEliminated }

    val totalBuyInsAndRebuys: Int
        get() = players.sumOf { 1 + it.rebuyCount }

    /** Gross collected from entries before deducting house fee. */
    val totalBuyInsGross: Int
        get() = totalBuyInsAndRebuys * config.buyInAmount

    /** Total fee taken by the house (per-entry fee × number of entries). */
    val totalFee: Int
        get() = totalBuyInsAndRebuys * config.feePerEntry

    /** Net prize pool distributed to winners (gross minus fee, floored at 0). */
    val totalPrizePool: Int
        get() = (totalBuyInsGross - totalFee).coerceAtLeast(0)
}
