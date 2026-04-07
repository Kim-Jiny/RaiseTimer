package com.jiny.raisetimer.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class TournamentSlotSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val updatedAt: Long,
    val state: TournamentState,
)

@Serializable
data class TournamentAppStorage(
    val currentTournamentId: String,
    val tournaments: List<TournamentSlotSnapshot>,
) {
    companion object {
        fun default(defaultName: String = "기본 토너먼트"): TournamentAppStorage {
            val slot = TournamentSlotSnapshot(
                name = defaultName,
                updatedAt = System.currentTimeMillis(),
                state = TournamentState(),
            )
            return TournamentAppStorage(
                currentTournamentId = slot.id,
                tournaments = listOf(slot),
            )
        }
    }
}

data class TournamentSlotSummary(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val playerCount: Int,
    val activePlayerCount: Int,
    val isCurrent: Boolean,
)
