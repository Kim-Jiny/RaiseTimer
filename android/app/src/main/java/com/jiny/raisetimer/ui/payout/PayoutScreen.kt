package com.jiny.raisetimer.ui.payout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiny.raisetimer.domain.PayoutCalculator
import com.jiny.raisetimer.ui.TournamentViewModel
import com.jiny.raisetimer.ui.theme.SurfaceElevated
import com.jiny.raisetimer.ui.theme.SurfaceHighlight

@Composable
fun PayoutScreen(viewModel: TournamentViewModel, contentPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val payouts = PayoutCalculator.calculate(state)
    val percents = state.config.payoutPercents
    val totalPercent = percents.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "상금",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Surface(
            color = SurfaceHighlight,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("분배 순위", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${percents.size}명", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("합계", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$totalPercent%",
                        color = if (totalPercent == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "총 상금 (수수료 제외)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${"%,d".format(state.totalPrizePool)}원",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "바이인 ${"%,d".format(state.config.buyInAmount)} × ${state.totalBuyInsAndRebuys}건 = ${"%,d".format(state.totalBuyInsGross)}원",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (state.config.feePerEntry > 0) {
                    Text(
                        text = "수수료 ${"%,d".format(state.config.feePerEntry)} × ${state.totalBuyInsAndRebuys}건 = -${"%,d".format(state.totalFee)}원",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "분배 비율 (%)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "합계 $totalPercent%",
                color = if (totalPercent == 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(percents) { index, percent ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceHighlight),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${index + 1}위",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = percent.toString(),
                            onValueChange = { text ->
                                val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                                val newList = percents.toMutableList().also { it[index] = parsed }
                                viewModel.updatePayoutPercents(newList)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(96.dp),
                        )
                        Text(
                            text = "  ${"%,d".format(payouts.getOrNull(index)?.amount ?: 0)}원",
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(onClick = {
                            val newList = percents.toMutableList().also { it.removeAt(index) }
                            viewModel.updatePayoutPercents(newList)
                        }) {
                            Icon(Icons.Filled.Remove, contentDescription = "삭제")
                        }
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
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("순위 추가")
        }
    }
}
