package com.jiny.raisetimer.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiny.raisetimer.domain.PayoutCalculator
import com.jiny.raisetimer.domain.model.BlindLevel
import com.jiny.raisetimer.domain.model.TournamentState
import com.jiny.raisetimer.ui.rememberLogoImageBitmap
import com.jiny.raisetimer.ui.TournamentViewModel
import com.jiny.raisetimer.ui.theme.LocalRaiseTimerPalette
import androidx.compose.ui.res.stringResource
import com.jiny.raisetimer.R

@Composable
fun TimerScreen(
    viewModel: TournamentViewModel,
    contentPadding: PaddingValues,
    isFullscreen: Boolean = false,
    onStartFullscreen: (() -> Unit)? = null,
    onToggleFullscreen: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalRaiseTimerPalette.current
    val colors = palette.timerGradient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = colors)
            )
            .padding(contentPadding)
    ) {
        if (isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FullscreenTimerContent(
                    state = state,
                    onClose = onToggleFullscreen,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                LevelBadge(
                    state = state,
                    isFullscreen = false,
                    onToggleFullscreen = onToggleFullscreen,
                )
                TimerHero(state, isFullscreen = false)
                QuickStats(state)
                if (state.isTournamentComplete) {
                    TournamentSummaryCard(state)
                }
                CurrentBlindsCard(state.currentLevel)
                state.nextLevel?.let { next -> NextBlindsCard(next) }
                Spacer(Modifier.height(16.dp))
                TimerControls(
                    isRunning = state.isRunning,
                    onPrevious = viewModel::previousLevel,
                    onToggle = viewModel::toggleRunning,
                    onStartFullscreen = onStartFullscreen,
                    onNext = viewModel::nextLevel,
                    onReset = viewModel::reset,
                    isFullscreen = false,
                )
            }
        }
    }
}

@Composable
private fun TournamentSummaryCard(state: TournamentState) {
    val palette = LocalRaiseTimerPalette.current
    val payouts = PayoutCalculator.calculate(state)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = palette.surfaceElevated
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.timer_summary_title), fontWeight = FontWeight.Bold, color = palette.onSurface)
            Text(
                text = "${stringResource(R.string.timer_winner)} ${state.winner?.name ?: stringResource(R.string.timer_undetermined)}",
                color = palette.accentSoft,
                fontWeight = FontWeight.Bold,
            )
            state.finalStandings.take(3).forEach { player ->
                val amount = player.placement?.let { payouts.getOrNull(it - 1)?.amount } ?: 0L
                Text(
                    text = stringResource(R.string.timer_place_format, player.placement ?: 0, player.name, "%,d".format(amount)),
                    color = palette.onSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun FullscreenTimerContent(
    state: TournamentState,
    onClose: (() -> Unit)?,
) {
    val context = LocalContext.current
    val logoBitmap = rememberLogoImageBitmap(context, state.config.fullscreenLogoFileName)
    val palette = LocalRaiseTimerPalette.current
    Box(modifier = Modifier.fillMaxSize()) {
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(36.dp)
                    .alpha(0.12f),
                contentScale = ContentScale.Fit,
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelPill(state)
                FullscreenStatPill(
                    text = "${state.totalBuyInsAndRebuys} ${stringResource(R.string.timer_entries)} · ${stringResource(R.string.payout_amount_format, "%,d".format(state.totalPrizePool))}",
                )
                Spacer(Modifier.weight(1f))
                if (onClose != null) {
                    FilledIconButton(
                        onClick = onClose,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = palette.surfaceElevated.copy(alpha = 0.88f),
                            contentColor = palette.onSurface,
                        ),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.timer_exit_fullscreen))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = if (state.isRunning) stringResource(R.string.timer_status_running) else stringResource(R.string.timer_status_ready),
                        color = palette.accentSoft,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatClock(state.remainingSeconds),
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                        color = palette.onSurface,
                    )
                    Surface(
                        color = palette.surfaceHighlight,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = if (state.currentLevel.isBreak) {
                                stringResource(R.string.timer_break)
                            } else {
                                stringResource(R.string.timer_blinds_display_format, formatChips(state.currentLevel.smallBlind), formatChips(state.currentLevel.bigBlind))
                            },
                            color = palette.onSurface,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                Spacer(Modifier.width(24.dp))

                state.nextLevel?.let { next ->
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.timer_next_level),
                            color = palette.onSurfaceMuted,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = when {
                                next.isBreak -> stringResource(R.string.timer_break_minutes_format, next.durationSeconds / 60)
                                next.ante > 0 ->
                                    "${formatChips(next.smallBlind)} / ${formatChips(next.bigBlind)} · ${stringResource(R.string.timer_ante_format, formatChips(next.ante))}"
                                else -> "${formatChips(next.smallBlind)} / ${formatChips(next.bigBlind)}"
                            },
                            color = palette.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TimerHero(state: TournamentState, isFullscreen: Boolean) {
    val palette = LocalRaiseTimerPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = palette.surfaceElevated
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (state.isRunning) stringResource(R.string.timer_status_running) else stringResource(R.string.timer_status_ready),
                color = palette.accentSoft,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatClock(state.remainingSeconds),
                style = if (isFullscreen) {
                    MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black)
                } else {
                    MaterialTheme.typography.displayLarge
                },
                color = palette.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                color = palette.surfaceHighlight,
                shape = CircleShape,
            ) {
                Text(
                    text = if (state.currentLevel.isBreak) stringResource(R.string.timer_break) else stringResource(R.string.timer_blinds_display_format, formatChips(state.currentLevel.smallBlind), formatChips(state.currentLevel.bigBlind)),
                    color = palette.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun QuickStats(state: TournamentState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InfoChip(
            label = stringResource(R.string.timer_remaining_players),
            value = "${state.activePlayers.size}/${state.players.size}",
            modifier = Modifier.weight(1f),
        )
        InfoChip(
            label = stringResource(R.string.timer_entries),
            value = "${state.totalBuyInsAndRebuys}",
            modifier = Modifier.weight(1f),
        )
        InfoChip(
            label = stringResource(R.string.tab_payout),
            value = stringResource(R.string.payout_amount_format, "%,d".format(state.totalPrizePool)),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = LocalRaiseTimerPalette.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = palette.surfaceHighlight
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                color = palette.onSurfaceMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = palette.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onStartFullscreen: (() -> Unit)?,
    onNext: () -> Unit,
    onReset: () -> Unit,
    isFullscreen: Boolean,
) {
    val palette = LocalRaiseTimerPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onPrevious,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = palette.surfaceHighlight,
                    contentColor = palette.onSurface,
                ),
                modifier = Modifier.size(58.dp),
            ) { Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.timer_action_previous)) }

            Button(
                onClick = {
                    if (!isRunning && !isFullscreen && onStartFullscreen != null) {
                        onStartFullscreen()
                    } else {
                        onToggle()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.chipGold,
                    contentColor = palette.feltGreenDark,
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(if (isFullscreen) 70.dp else 62.dp),
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (isRunning) stringResource(R.string.timer_action_pause) else stringResource(R.string.timer_action_start),
                    fontWeight = FontWeight.Bold,
                )
            }

            FilledIconButton(
                onClick = onNext,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = palette.surfaceHighlight,
                    contentColor = palette.onSurface,
                ),
                modifier = Modifier.size(58.dp),
            ) { Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.timer_action_next)) }
        }

        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.surface,
                contentColor = palette.onSurface,
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.timer_reset_level))
        }
    }
}

@Composable
private fun LevelBadge(
    state: TournamentState,
    isFullscreen: Boolean,
    onToggleFullscreen: (() -> Unit)?,
) {
    val palette = LocalRaiseTimerPalette.current
    val level = state.currentLevel
    val label = if (level.isBreak) stringResource(R.string.timer_break_time) else stringResource(R.string.timer_level_format, level.level)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(palette.chipGoldSoft)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = palette.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        if (onToggleFullscreen != null) {
            FilledIconButton(
                onClick = onToggleFullscreen,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = palette.surfaceHighlight,
                    contentColor = palette.onSurface,
                ),
            ) {
                Icon(
                    imageVector = if (isFullscreen) {
                        Icons.Filled.FullscreenExit
                    } else {
                        Icons.Filled.Fullscreen
                    },
                    contentDescription = if (isFullscreen) stringResource(R.string.timer_exit_fullscreen) else stringResource(R.string.timer_enter_fullscreen),
                )
            }
        }
    }
}

@Composable
private fun LevelPill(state: TournamentState) {
    val palette = LocalRaiseTimerPalette.current
    val label = if (state.currentLevel.isBreak) stringResource(R.string.timer_break_time) else stringResource(R.string.timer_level_format, state.currentLevel.level)
    Surface(
        color = palette.surfaceHighlight.copy(alpha = 0.88f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette.chipGoldSoft)
            )
            Text(
                text = label,
                color = palette.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FullscreenStatPill(text: String) {
    val palette = LocalRaiseTimerPalette.current
    Surface(
        color = palette.surfaceElevated.copy(alpha = 0.92f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            color = palette.onSurfaceMuted,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun CurrentBlindsCard(level: BlindLevel) {
    val palette = LocalRaiseTimerPalette.current
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surfaceElevated),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                text = stringResource(R.string.timer_current_blinds),
                color = palette.onSurfaceMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (level.isBreak) stringResource(R.string.timer_break) else "${formatChips(level.smallBlind)} / ${formatChips(level.bigBlind)}",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.onSurface,
            )
            if (level.ante > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.timer_ante_format, formatChips(level.ante)),
                    color = palette.onSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun NextBlindsCard(level: BlindLevel) {
    val palette = LocalRaiseTimerPalette.current
    Card(
        colors = CardDefaults.cardColors(containerColor = palette.surfaceHighlight),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.timer_next_level),
                color = palette.onSurfaceMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    level.isBreak -> stringResource(R.string.timer_break_minutes_format, level.durationSeconds / 60)
                    level.ante > 0 ->
                        "${formatChips(level.smallBlind)} / ${formatChips(level.bigBlind)} · ${stringResource(R.string.timer_ante_format, formatChips(level.ante))}"
                    else -> "${formatChips(level.smallBlind)} / ${formatChips(level.bigBlind)}"
                },
                color = palette.onSurface,
            )
        }
    }
}

private fun formatClock(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

internal fun formatChips(value: Int): String =
    "%,d".format(value)
