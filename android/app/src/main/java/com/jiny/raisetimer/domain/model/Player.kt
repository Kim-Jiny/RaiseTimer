package com.jiny.raisetimer.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rebuyCount: Int = 0,
    val isEliminated: Boolean = false,
    val placement: Int? = null,
)
