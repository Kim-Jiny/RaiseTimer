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
    val turnTimeChipSeconds: Int = 30,
) {
    companion object {
        fun defaultBlindStructure(): List<BlindLevel> = listOf(
            BlindLevel(level = 1, smallBlind = 100, bigBlind = 200, ante = 0, durationSeconds = 20 * 60),
            BlindLevel(level = 2, smallBlind = 200, bigBlind = 400, ante = 0, durationSeconds = 20 * 60),
            BlindLevel(level = 3, smallBlind = 300, bigBlind = 600, ante = 0, durationSeconds = 20 * 60),
            BlindLevel(level = 4, smallBlind = 400, bigBlind = 800, ante = 0, durationSeconds = 20 * 60),
            BlindLevel.breakLevel(5, 10 * 60),
            BlindLevel(level = 6, smallBlind = 500, bigBlind = 1_000, ante = 100, durationSeconds = 20 * 60),
            BlindLevel(level = 7, smallBlind = 700, bigBlind = 1_400, ante = 200, durationSeconds = 20 * 60),
            BlindLevel(level = 8, smallBlind = 1_000, bigBlind = 2_000, ante = 300, durationSeconds = 20 * 60),
            BlindLevel(level = 9, smallBlind = 1_500, bigBlind = 3_000, ante = 400, durationSeconds = 20 * 60),
            BlindLevel(level = 10, smallBlind = 2_000, bigBlind = 4_000, ante = 500, durationSeconds = 20 * 60),
            BlindLevel(level = 11, smallBlind = 3_000, bigBlind = 6_000, ante = 1_000, durationSeconds = 20 * 60),
            BlindLevel(level = 12, smallBlind = 5_000, bigBlind = 10_000, ante = 1_000, durationSeconds = 20 * 60),
            BlindLevel(level = 13, smallBlind = 8_000, bigBlind = 16_000, ante = 2_000, durationSeconds = 20 * 60),
        )
    }
}
