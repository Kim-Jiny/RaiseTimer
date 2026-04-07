package com.jiny.raisetimer.ui.payout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiny.raisetimer.R
import com.jiny.raisetimer.domain.PayoutCalculator
import com.jiny.raisetimer.ui.TournamentViewModel
import com.jiny.raisetimer.ui.clearFocusOnTap
import com.jiny.raisetimer.ui.theme.LocalRaiseTimerPalette

@Composable
fun PayoutScreen(viewModel: TournamentViewModel, contentPadding: PaddingValues) {
    val palette = LocalRaiseTimerPalette.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val payouts = PayoutCalculator.calculate(state)
    val percents = state.config.payoutPercents
    val totalPercent = percents.sum()
    val isValidTotal = totalPercent == 100
    val payoutPresets = listOf(
        "60/40" to listOf(60, 40),
        "50/30/20" to listOf(50, 30, 20),
        "70/20/10" to listOf(70, 20, 10),
        "40/30/20/10" to listOf(40, 30, 20, 10),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = palette.surfaceHighlight,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.payout_distribution_rank), color = palette.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.payout_people_format, percents.size), color = palette.chipGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Surface(
                color = palette.surfaceHighlight,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.payout_total), color = palette.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$totalPercent%",
                        color = if (totalPercent == 100) palette.chipGold else palette.chipRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.payout_total_prize),
                    color = palette.onSurfaceMuted,
                )
                Text(
                    text = stringResource(R.string.payout_amount_format, "%,d".format(state.totalPrizePool)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.chipGold,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.payout_buyin_detail_format, "%,d".format(state.config.buyInAmount), state.totalBuyInsAndRebuys, "%,d".format(state.totalBuyInsGross)),
                    color = palette.onSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (state.config.feePerEntry > 0) {
                    Text(
                        text = stringResource(R.string.payout_fee_detail_format, "%,d".format(state.config.feePerEntry), state.totalBuyInsAndRebuys, "%,d".format(state.totalFee)),
                        color = palette.chipRed,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.payout_ratio_title),
                style = MaterialTheme.typography.titleLarge,
                color = palette.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.payout_total_percent_format, totalPercent),
                color = if (totalPercent == 100) palette.chipGold else palette.chipRed,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = if (isValidTotal) {
                stringResource(R.string.payout_valid_message)
            } else {
                stringResource(R.string.payout_invalid_message)
            },
            color = if (isValidTotal) palette.chipGold else palette.chipRed,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (state.isTournamentComplete) {
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.timer_summary_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(R.string.payout_winner_format, state.winner?.name ?: stringResource(R.string.timer_undetermined)),
                        color = palette.chipGold,
                        fontWeight = FontWeight.Bold,
                    )
                    state.finalStandings.take(payouts.size.coerceAtLeast(3)).forEach { player ->
                        val amount = player.placement?.let { payouts.getOrNull(it - 1)?.amount } ?: 0
                        Text(
                            text = stringResource(R.string.timer_place_format, player.placement ?: 0, player.name, "%,d".format(amount)),
                            color = palette.onSurface,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            payoutPresets.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { (label, values) ->
                        Button(
                            onClick = { viewModel.updatePayoutPercents(values) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceElevated, contentColor = palette.onSurface),
                        ) {
                            Text(label)
                        }
                    }
                    repeat(2 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        percents.forEachIndexed { index, percent ->
            Card(
                colors = CardDefaults.cardColors(containerColor = palette.surfaceHighlight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.payout_place_format, index + 1),
                        fontWeight = FontWeight.Bold,
                        color = palette.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = percent.toString(),
                        onValueChange = { text ->
                            val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                            val newList = percents.toMutableList().also { it[index] = parsed }
                            viewModel.updatePayoutPercents(newList)
                        },
                        label = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(96.dp),
                    )
                    Text(
                        text = "  " + stringResource(R.string.payout_amount_format, "%,d".format(payouts.getOrNull(index)?.amount ?: 0)),
                        color = palette.chipGold,
                    )
                    IconButton(
                        onClick = {
                            val newList = percents.toMutableList().also { it.removeAt(index) }
                            viewModel.updatePayoutPercents(newList)
                        },
                        enabled = percents.size > 1,
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.structure_delete))
                    }
                }
            }
        }

        Button(
            onClick = {
                val newList = percents + 0
                viewModel.updatePayoutPercents(newList)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = palette.chipGold, contentColor = palette.feltGreenDark),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.payout_add_rank))
        }

        Spacer(Modifier.height(8.dp))
    }
}
