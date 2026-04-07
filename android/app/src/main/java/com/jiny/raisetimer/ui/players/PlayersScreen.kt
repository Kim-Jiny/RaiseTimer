package com.jiny.raisetimer.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiny.raisetimer.domain.PayoutCalculator
import com.jiny.raisetimer.domain.model.Player
import com.jiny.raisetimer.ui.clearFocusOnTap
import com.jiny.raisetimer.ui.TournamentViewModel
import androidx.compose.ui.res.stringResource
import com.jiny.raisetimer.R
import com.jiny.raisetimer.ui.theme.LocalRaiseTimerPalette

@Composable
fun PlayersScreen(viewModel: TournamentViewModel, contentPadding: PaddingValues) {
    val palette = LocalRaiseTimerPalette.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val payouts = PayoutCalculator.calculate(state)
    var newName by remember { mutableStateOf("") }
    val rebuyAllowed = state.config.rebuyAllowed
    val canAddPlayer = newName.trim().isNotEmpty()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.players_title),
                style = MaterialTheme.typography.titleLarge,
                color = palette.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.activePlayers.size} / ${state.players.size}",
                color = palette.chipGold,
                fontWeight = FontWeight.Bold,
            )
        }

        Surface(
            color = palette.surfaceElevated,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.players_total_entries), color = palette.onSurfaceMuted)
                    Text(
                        text = "${state.totalBuyInsAndRebuys}",
                        color = palette.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (rebuyAllowed) stringResource(R.string.players_rebuy_available) else stringResource(R.string.players_rebuy_closed),
                        color = if (rebuyAllowed) palette.chipGold else palette.onSurfaceMuted,
                    )
                    Text(
                        text = stringResource(R.string.players_prize_format, "%,d".format(state.totalPrizePool)),
                        color = palette.onSurface,
                    )
                }
            }
        }

        if (state.isTournamentComplete) {
            Surface(
                color = palette.surfaceHighlight,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.timer_summary_title), fontWeight = FontWeight.Bold, color = palette.onSurface)
                    Text(
                        text = "${stringResource(R.string.timer_winner)} ${state.winner?.name ?: stringResource(R.string.timer_undetermined)}",
                        color = palette.chipGold,
                        fontWeight = FontWeight.Bold,
                    )
                    state.finalStandings.take(3).forEach { player ->
                        val amount = player.placement?.let { payouts.getOrNull(it - 1)?.amount } ?: 0
                        Text(
                            text = stringResource(R.string.timer_place_format, player.placement ?: 0, player.name, "%,d".format(amount)),
                            color = palette.onSurfaceMuted,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text(stringResource(R.string.players_name_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canAddPlayer) {
                            viewModel.addPlayer(newName)
                            newName = ""
                            focusManager.clearFocus(force = true)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = palette.chipGold, cursorColor = palette.chipGold),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    viewModel.addPlayer(newName)
                    newName = ""
                    focusManager.clearFocus(force = true)
                },
                enabled = canAddPlayer,
                colors = ButtonDefaults.buttonColors(containerColor = palette.chipGold, contentColor = palette.feltGreenDark),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.players.isEmpty()) {
                item {
                    Surface(
                        color = palette.surfaceHighlight,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(stringResource(R.string.players_empty_title), color = palette.onSurface)
                            Text(
                                text = stringResource(R.string.players_empty_description),
                                color = palette.onSurfaceMuted,
                            )
                        }
                    }
                }
            }
            items(state.players, key = { it.id }) { player ->
                PlayerRow(
                    player = player,
                    rebuyAllowed = rebuyAllowed,
                    onEliminate = { viewModel.eliminatePlayer(player.id) },
                    onRevive = { viewModel.revivePlayer(player.id) },
                    onRebuyPlus = { viewModel.incrementRebuy(player.id, +1) },
                    onRebuyMinus = { viewModel.incrementRebuy(player.id, -1) },
                    onDelete = { viewModel.removePlayer(player.id) },
                )
            }
        }
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    rebuyAllowed: Boolean,
    onEliminate: () -> Unit,
    onRevive: () -> Unit,
    onRebuyPlus: () -> Unit,
    onRebuyMinus: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalRaiseTimerPalette.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (player.isEliminated)
                palette.surface
            else
                palette.surfaceHighlight
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.onSurface,
                    textDecoration = if (player.isEliminated) TextDecoration.LineThrough else null,
                )
                val meta = buildString {
                    if (player.rebuyCount > 0) append(stringResource(R.string.players_rebuy_format, player.rebuyCount))
                    if (player.placement != null) {
                        if (isNotEmpty()) append(" · ")
                        append(stringResource(R.string.players_place_format, player.placement!!))
                    }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        color = palette.onSurfaceMuted,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            if (rebuyAllowed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onRebuyMinus,
                        enabled = player.rebuyCount > 0,
                    ) { Text("-") }
                    Text("${player.rebuyCount}", color = palette.onSurface)
                    TextButton(onClick = onRebuyPlus) { Text("+") }
                }
            }

            if (player.isEliminated) {
                IconButton(onClick = onRevive) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.players_action_revive), tint = palette.chipGold)
                }
            } else {
                IconButton(onClick = onEliminate) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.players_action_eliminate), tint = palette.chipRed)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.players_action_delete), tint = palette.chipRed)
            }
        }
    }
}
