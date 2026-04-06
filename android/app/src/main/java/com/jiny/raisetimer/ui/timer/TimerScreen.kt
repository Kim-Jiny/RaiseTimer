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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isFullscreen) 32.dp else 20.dp,
                    vertical = if (isFullscreen) 24.dp else 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (isFullscreen) {
                FullscreenTimerContent(
                    state = state,
                    onClose = onToggleFullscreen,
                )
            } else {
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
                Spacer(Modifier.weight(1f))
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
    val payouts = PayoutCalculator.calculate(state)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("게임 종료 요약", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "우승 ${state.winner?.name ?: "미정"}",
                color = LocalRaiseTimerPalette.current.accentSoft,
                fontWeight = FontWeight.Bold,
            )
            state.finalStandings.take(3).forEach { player ->
                val amount = player.placement?.let { payouts.getOrNull(it - 1)?.amount } ?: 0
                Text(
                    text = "${player.placement?.toString() ?: "-"}위 ${player.name} · ${"%,d".format(amount)}원",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Spacer(Modifier.weight(1f))
                if (onClose != null) {
                    FilledIconButton(
                        onClick = onClose,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "전체화면 종료")
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
                        text = if (state.isRunning) "진행 중" else "준비됨",
                        color = palette.accentSoft,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatClock(state.remainingSeconds),
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = if (state.currentLevel.isBreak) {
                                "Break"
                            } else {
                                "Blinds ${formatChips(state.currentLevel.smallBlind)} / ${formatChips(state.currentLevel.bigBlind)}"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
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
                            text = "다음 레벨",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = when {
                                next.isBreak -> "휴식 ${next.durationSeconds / 60}분"
                                next.ante > 0 ->
                                    "${formatChips(next.smallBlind)} / ${formatChips(next.bigBlind)} · 앤티 ${formatChips(next.ante)}"
                                else -> "${formatChips(next.smallBlind)} / ${formatChips(next.bigBlind)}"
                            },
                            color = MaterialTheme.colorScheme.onBackground,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
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
                text = if (state.isRunning) "진행 중" else "준비됨",
                color = LocalRaiseTimerPalette.current.accentSoft,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatClock(state.remainingSeconds),
                style = if (isFullscreen) {
                    MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black)
                } else {
                    MaterialTheme.typography.displayLarge
                },
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
            ) {
                Text(
                    text = if (state.currentLevel.isBreak) "Break" else "Blinds ${formatChips(state.currentLevel.smallBlind)} / ${formatChips(state.currentLevel.bigBlind)}",
                    color = MaterialTheme.colorScheme.onSurface,
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
            label = "남은 인원",
            value = "${state.activePlayers.size}/${state.players.size}",
            modifier = Modifier.weight(1f),
        )
        InfoChip(
            label = "엔트리",
            value = "${state.totalBuyInsAndRebuys}",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onPrevious,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.size(58.dp),
            ) { Icon(Icons.Filled.SkipPrevious, contentDescription = "이전") }

            Button(
                onClick = {
                    if (!isRunning && !isFullscreen && onStartFullscreen != null) {
                        onStartFullscreen()
                    } else {
                        onToggle()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
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
                    text = if (isRunning) "일시정지" else "시작",
                    fontWeight = FontWeight.Bold,
                )
            }

            FilledIconButton(
                onClick = onNext,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.size(58.dp),
            ) { Icon(Icons.Filled.SkipNext, contentDescription = "다음") }
        }

        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("레벨 처음부터 리셋")
        }
    }
}

@Composable
private fun LevelBadge(
    state: TournamentState,
    isFullscreen: Boolean,
    onToggleFullscreen: (() -> Unit)?,
) {
    val level = state.currentLevel
    val label = if (level.isBreak) "휴식 시간" else "레벨 ${level.level}"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(ChipGoldSoft)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        if (onToggleFullscreen != null) {
            FilledIconButton(
                onClick = onToggleFullscreen,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(
                    imageVector = if (isFullscreen) {
                        Icons.Filled.FullscreenExit
                    } else {
                        Icons.Filled.Fullscreen
                    },
                    contentDescription = if (isFullscreen) "전체화면 종료" else "전체화면",
                )
            }
        }
    }
}

@Composable
private fun LevelPill(state: TournamentState) {
    val label = if (state.currentLevel.isBreak) "휴식 시간" else "레벨 ${state.currentLevel.level}"
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
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
                    .background(ChipGoldSoft)
            )
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CurrentBlindsCard(level: BlindLevel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                text = "현재 블라인드",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (level.isBreak) "휴식" else "${formatChips(level.smallBlind)} / ${formatChips(level.bigBlind)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (level.ante > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "앤티 ${formatChips(level.ante)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NextBlindsCard(level: BlindLevel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = "다음 레벨",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    level.isBreak -> "휴식 ${level.durationSeconds / 60}분"
                    level.ante > 0 ->
                        "${formatChips(level.smallBlind)} / ${formatChips(level.bigBlind)} · 앤티 ${formatChips(level.ante)}"
                    else -> "${formatChips(level.smallBlind)} / ${formatChips(level.bigBlind)}"
                },
                color = MaterialTheme.colorScheme.onSurface,
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
