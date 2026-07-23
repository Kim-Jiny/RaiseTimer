package com.jiny.raisetimer.ui.structure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiny.raisetimer.R
import com.jiny.raisetimer.domain.model.BlindLevel
import com.jiny.raisetimer.ui.TournamentViewModel
import com.jiny.raisetimer.ui.clearFocusOnTap
import com.jiny.raisetimer.ui.theme.LocalRaiseTimerPalette

@Composable
fun StructureScreen(viewModel: TournamentViewModel, contentPadding: PaddingValues) {
    val palette = LocalRaiseTimerPalette.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val totalMinutes = state.config.levels.sumOf { it.durationSeconds } / 60
    var presetName by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.structure_nav_title),
            style = MaterialTheme.typography.titleLarge,
            color = palette.onSurface,
        )
        Text(
            text = stringResource(R.string.structure_summary_format, state.config.levels.size, totalMinutes),
            color = palette.onSurfaceMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumberField(
                    label = stringResource(R.string.structure_starting_stack),
                    value = state.config.startingStack,
                    onValueChange = { newValue ->
                        viewModel.updateConfig { it.copy(startingStack = newValue) }
                    },
                )
                NumberField(
                    label = stringResource(R.string.structure_buyin),
                    value = state.config.buyInAmount,
                    onValueChange = { newValue ->
                        viewModel.updateConfig { it.copy(buyInAmount = newValue) }
                    },
                )
                NumberField(
                    label = stringResource(R.string.structure_fee),
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
                        text = stringResource(R.string.structure_rebuy_allowed),
                        color = palette.onSurface,
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

        Card(
            colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.structure_save_settings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.structure_save_description),
                    color = palette.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = presetName,
                        onValueChange = { presetName = it },
                        label = { Text(stringResource(R.string.structure_settings_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            viewModel.saveCurrentBlindStructure(presetName)
                            presetName = ""
                        },
                        enabled = presetName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.chipGold, contentColor = palette.feltGreenDark),
                    ) {
                        Text(stringResource(R.string.structure_save))
                    }
                }

                if (state.config.savedBlindStructures.isEmpty()) {
                    Text(
                        text = stringResource(R.string.structure_no_saved),
                        color = palette.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    state.config.savedBlindStructures.reversed().forEach { preset ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = palette.surfaceHighlight),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(preset.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = stringResource(R.string.structure_preset_detail_format, preset.levels.size, preset.payoutPercents.size),
                                        color = palette.onSurfaceMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                OutlinedButton(onClick = { viewModel.loadBlindStructure(preset.id) }) {
                                    Text(stringResource(R.string.structure_load))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteBlindStructure(preset.id) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = palette.chipRed
                                    ),
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.structure_delete_setting))
                                }
                            }
                        }
                    }
                }
            }
        }

        state.config.levels.forEachIndexed { index, level ->
            LevelCard(
                index = index,
                level = level,
                onChange = { updated -> viewModel.updateLevel(index, updated) },
                onDelete = { viewModel.removeLevel(index) },
            )
        }

        Button(
            onClick = viewModel::addLevel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = palette.chipGold, contentColor = palette.feltGreenDark),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.structure_add_level))
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
    val palette = LocalRaiseTimerPalette.current
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surfaceHighlight),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (level.isBreak) stringResource(R.string.structure_break_index_format, index + 1) else stringResource(R.string.timer_level_format, level.level),
                    fontWeight = FontWeight.Bold,
                    color = palette.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(stringResource(R.string.structure_break_toggle), color = palette.onSurfaceMuted)
                Switch(
                    checked = level.isBreak,
                    onCheckedChange = { isBreak -> onChange(level.copy(isBreak = isBreak)) },
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.structure_delete), tint = palette.chipRed)
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
                        label = stringResource(R.string.structure_ante),
                        value = level.ante,
                        modifier = Modifier.weight(1f),
                        onValueChange = { onChange(level.copy(ante = it)) },
                    )
                }
            }

            NumberField(
                label = stringResource(R.string.structure_duration_seconds),
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
    var text by remember { mutableStateOf(value.toString()) }
    // Re-sync the local buffer when the external value changes from elsewhere (steppers,
    // switching tournament slots). Skip the empty-vs-zero case so the user can clear the
    // field to retype without it snapping straight back to "0".
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value && !(text.isEmpty() && value == 0)) {
            text = value.toString()
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            // Cap at 9 digits so buy-in / starting stack stay within Int range; the
            // prize-pool arithmetic widens to Long downstream.
            val digits = raw.filter { it.isDigit() }.take(9)
            text = digits
            onValueChange(digits.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        placeholder = { Text("0") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}
