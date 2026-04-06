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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.jiny.raisetimer.ui.theme.SurfaceElevated
import com.jiny.raisetimer.ui.theme.SurfaceHighlight

@Composable
fun PlayersScreen(viewModel: TournamentViewModel, contentPadding: PaddingValues) {
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
                text = "참가자",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${state.activePlayers.size} / ${state.players.size}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        Surface(
            color = SurfaceElevated,
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
                    Text("총 엔트리", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${state.totalBuyInsAndRebuys}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (rebuyAllowed) "리바이 가능" else "리바이 닫힘",
                        color = if (rebuyAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "상금 ${"%,d".format(state.totalPrizePool)}원",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (state.isTournamentComplete) {
            Surface(
                color = SurfaceHighlight,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("게임 종료 요약", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "우승 ${state.winner?.name ?: "미정"}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    state.finalStandings.take(3).forEach { player ->
                        val amount = player.placement?.let { payouts.getOrNull(it - 1)?.amount } ?: 0
                        Text(
                            text = "${player.placement ?: "-"}위 ${player.name} · ${"%,d".format(amount)}원",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                placeholder = { Text("이름 입력") },
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
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    viewModel.addPlayer(newName)
                    newName = ""
                    focusManager.clearFocus(force = true)
                },
                enabled = canAddPlayer,
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
                        color = SurfaceHighlight,
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
                            Text("아직 참가자가 없습니다", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "이름을 추가하면 탈락 관리와 상금 계산이 바로 연결됩니다.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (player.isEliminated)
                SurfaceElevated
            else
                SurfaceHighlight
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
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (player.isEliminated) TextDecoration.LineThrough else null,
                )
                val meta = buildString {
                    if (player.rebuyCount > 0) append("리바이 ${player.rebuyCount}")
                    if (player.placement != null) {
                        if (isNotEmpty()) append(" · ")
                        append("${player.placement}위")
                    }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text("${player.rebuyCount}", color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = onRebuyPlus) { Text("+") }
                }
            }

            if (player.isEliminated) {
                IconButton(onClick = onRevive) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "되살리기")
                }
            } else {
                IconButton(onClick = onEliminate) {
                    Icon(Icons.Filled.Close, contentDescription = "탈락")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "삭제")
            }
        }
    }
}
