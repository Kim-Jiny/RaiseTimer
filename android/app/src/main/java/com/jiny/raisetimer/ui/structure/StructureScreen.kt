package com.jiny.raisetimer.ui.structure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiny.raisetimer.domain.model.BlindLevel
import com.jiny.raisetimer.ui.TournamentViewModel

@Composable
fun StructureScreen(viewModel: TournamentViewModel, contentPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "블라인드 구조",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumberField(
                    label = "시작 스택",
                    value = state.config.startingStack,
                    onValueChange = { newValue ->
                        viewModel.updateConfig { it.copy(startingStack = newValue) }
                    },
                )
                NumberField(
                    label = "바이인 (원)",
                    value = state.config.buyInAmount,
                    onValueChange = { newValue ->
                        viewModel.updateConfig { it.copy(buyInAmount = newValue) }
                    },
                )
                NumberField(
                    label = "수수료 / 참가당 (원)",
                    value = state.config.feePerEntry,
                    onValueChange = { newValue ->
                        viewModel.updateConfig { it.copy(feePerEntry = newValue) }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "리바이 허용",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.config.rebuyAllowed,
                        onCheckedChange = { enabled ->
                            viewModel.updateConfig { it.copy(rebuyAllowed = enabled) }
                        },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.config.levels, key = { index, _ -> index }) { index, level ->
                LevelCard(
                    index = index,
                    level = level,
                    onChange = { updated -> viewModel.updateLevel(index, updated) },
                    onDelete = { viewModel.removeLevel(index) },
                )
            }
        }

        Button(
            onClick = viewModel::addLevel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("레벨 추가")
        }
    }
}

@Composable
private fun LevelCard(
    index: Int,
    level: BlindLevel,
    onChange: (BlindLevel) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (level.isBreak) "휴식 ${index + 1}" else "레벨 ${level.level}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text("휴식", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(
                    checked = level.isBreak,
                    onCheckedChange = { isBreak -> onChange(level.copy(isBreak = isBreak)) },
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "삭제")
                }
            }

            if (!level.isBreak) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NumberField(
                        label = "SB",
                        value = level.smallBlind,
                        modifier = Modifier.weight(1f),
                        onValueChange = { onChange(level.copy(smallBlind = it)) },
                    )
                    NumberField(
                        label = "BB",
                        value = level.bigBlind,
                        modifier = Modifier.weight(1f),
                        onValueChange = { onChange(level.copy(bigBlind = it)) },
                    )
                    NumberField(
                        label = "앤티",
                        value = level.ante,
                        modifier = Modifier.weight(1f),
                        onValueChange = { onChange(level.copy(ante = it)) },
                    )
                }
            }

            NumberField(
                label = "지속(초)",
                value = level.durationSeconds,
                onValueChange = { onChange(level.copy(durationSeconds = it.coerceAtLeast(1))) },
            )
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text ->
            val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 0
            onValueChange(parsed)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}
