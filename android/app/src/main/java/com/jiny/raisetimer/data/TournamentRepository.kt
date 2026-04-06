package com.jiny.raisetimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jiny.raisetimer.domain.model.TournamentAppStorage
import com.jiny.raisetimer.domain.model.TournamentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "raise_timer")

/**
 * Persists app-level tournament storage as JSON in Preferences DataStore.
 */
class TournamentRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val storageFlow: Flow<TournamentAppStorage> = dataStore.data.map { prefs ->
        val raw = prefs[STATE_KEY]
        when {
            raw == null -> TournamentAppStorage.default()
            else -> runCatching { json.decodeFromString(TournamentAppStorage.serializer(), raw) }
                .recoverCatching {
                    val legacyState = json.decodeFromString(TournamentState.serializer(), raw)
                    val slot = com.jiny.raisetimer.domain.model.TournamentSlotSnapshot(
                        name = "기본 토너먼트",
                        updatedAt = System.currentTimeMillis(),
                        state = legacyState,
                    )
                    TournamentAppStorage(
                        currentTournamentId = slot.id,
                        tournaments = listOf(slot),
                    )
                }
                .getOrElse { TournamentAppStorage.default() }
        }
    }

    suspend fun save(storage: TournamentAppStorage) {
        val raw = json.encodeToString(TournamentAppStorage.serializer(), storage)
        dataStore.edit { prefs -> prefs[STATE_KEY] = raw }
    }

    private companion object {
        val STATE_KEY = stringPreferencesKey("tournament_state_json")
    }
}
