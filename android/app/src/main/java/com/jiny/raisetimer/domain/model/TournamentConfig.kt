package com.jiny.raisetimer.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemePreset {
    EMERALD,
    OCEAN,
    RUBY,
    SUNSET,
    LAVENDER,
    SLATE,
    MINT,
    CORAL,
    MIDNIGHT,
}

@Serializable
data class BlindStructurePreset(
    val id: String,
    val name: String,
    val startingStack: Int,
    val buyInAmount: Int,
    val feePerEntry: Int,
    val rebuyAllowed: Boolean,
    val levels: List<BlindLevel>,
    val payoutPercents: List<Int>,
)

@Serializable
data class TournamentConfig(
    val startingStack: Int = 10_000,
    val buyInAmount: Int = 50_000,
    /** Per-entry fee (house rake). Subtracted from each buy-in/rebuy before prize pool. */
    val feePerEntry: Int = 0,
    val rebuyAllowed: Boolean = true,
    val levels: List<BlindLevel> = defaultBlindStructure(),
    val payoutPercents: List<Int> = listOf(50, 30, 20),
    val themePreset: ThemePreset = ThemePreset.EMERALD,
    val fullscreenLogoFileName: String? = null,
    val fullscreenLogoBase64: String? = null,
    val savedBlindStructures: List<BlindStructurePreset> = emptyList(),
) {
    companion object {
        fun defaultBlindStructure(): List<BlindLevel> = listOf(
            BlindLevel(1, 100, 200, 0, 20 * 60),
            BlindLevel(2, 200, 400, 0, 20 * 60),
            BlindLevel(3, 300, 600, 0, 20 * 60),
            BlindLevel(4, 400, 800, 0, 20 * 60),
            BlindLevel.breakLevel(5, 10 * 60),
            BlindLevel(6, 500, 1_000, 100, 20 * 60),
            BlindLevel(7, 700, 1_400, 200, 20 * 60),
            BlindLevel(8, 1_000, 2_000, 300, 20 * 60),
            BlindLevel(9, 1_500, 3_000, 400, 20 * 60),
            BlindLevel(10, 2_000, 4_000, 500, 20 * 60),
            BlindLevel(11, 3_000, 6_000, 1_000, 20 * 60),
            BlindLevel(12, 5_000, 10_000, 1_000, 20 * 60),
            BlindLevel(13, 8_000, 16_000, 2_000, 20 * 60),
        )
    }
}
