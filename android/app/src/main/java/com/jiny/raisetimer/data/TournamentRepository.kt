package com.jiny.raisetimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jiny.raisetimer.domain.model.TournamentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "raise_timer")

/**
 * Persists the full [TournamentState] as JSON in a Preferences DataStore. Simple enough for
 * v1 — a single tournament at a time, no history.
 */
class TournamentRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val stateFlow: Flow<TournamentState> = dataStore.data.map { prefs ->
        val raw = prefs[STATE_KEY] ?: return@map TournamentState()
        runCatching { json.decodeFromString(TournamentState.serializer(), raw) }
            .getOrElse { TournamentState() }
    }

    suspend fun save(state: TournamentState) {
        val raw = json.encodeToString(TournamentState.serializer(), state)
        dataStore.edit { prefs -> prefs[STATE_KEY] = raw }
    }

    private companion object {
        val STATE_KEY = stringPreferencesKey("tournament_state_json")
    }
}
