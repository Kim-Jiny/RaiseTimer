package com.jiny.raisetimer.domain

import com.jiny.raisetimer.domain.model.TournamentState

object PayoutCalculator {

    data class Payout(val place: Int, val amount: Long)

    /**
     * Splits the total prize pool across [TournamentConfig.payoutPercents]. Percentages are
     * normalized if they do not sum to 100, and any remainder from integer division is
     * added to the first place.
     */
    fun calculate(state: TournamentState): List<Payout> {
        val percents = state.config.payoutPercents
        if (percents.isEmpty()) return emptyList()

        val pool = state.totalPrizePool
        if (pool <= 0) return percents.indices.map { Payout(it + 1, 0L) }

        val sum = percents.sum().takeIf { it > 0 } ?: 100
        val rawAmounts = percents.map { pct -> pool * pct / sum }
        val distributed = rawAmounts.sum()
        val remainder = pool - distributed

        return rawAmounts.mapIndexed { index, amount ->
            val adjusted = if (index == 0) amount + remainder else amount
            Payout(place = index + 1, amount = adjusted)
        }
    }
}
