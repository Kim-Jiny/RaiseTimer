package com.jiny.raisetimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jiny.raisetimer.R
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

    private val appContext = context.applicationContext
    private val dataStore = appContext.dataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val storageFlow: Flow<TournamentAppStorage> = dataStore.data.map { prefs ->
        val raw = prefs[STATE_KEY]
        when {
            raw == null -> TournamentAppStorage.default(appContext.getString(R.string.default_tournament_name))
            else -> runCatching { json.decodeFromString(TournamentAppStorage.serializer(), raw) }
                .recoverCatching {
                    val legacyState = json.decodeFromString(TournamentState.serializer(), raw)
                    val slot = com.jiny.raisetimer.domain.model.TournamentSlotSnapshot(
                        name = appContext.getString(R.string.default_tournament_name),
                        updatedAt = System.currentTimeMillis(),
                        state = legacyState,
                    )
                    TournamentAppStorage(
                        currentTournamentId = slot.id,
                        tournaments = listOf(slot),
                    )
                }
                .getOrElse { TournamentAppStorage.default(appContext.getString(R.string.default_tournament_name)) }
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
