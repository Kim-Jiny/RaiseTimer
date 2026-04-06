package com.jiny.raisetimer.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class BlindLevel(
    val id: String = UUID.randomUUID().toString(),
    val level: Int,
    val smallBlind: Int,
    val bigBlind: Int,
    val ante: Int = 0,
    val durationSeconds: Int,
    val isBreak: Boolean = false,
) {
    companion object {
        fun breakLevel(level: Int, durationSeconds: Int): BlindLevel =
            BlindLevel(
                level = level,
                smallBlind = 0,
                bigBlind = 0,
                ante = 0,
                durationSeconds = durationSeconds,
                isBreak = true,
            )
    }
}
