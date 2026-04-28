package com.jiny.raisetimer.ui.turntimer

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiny.raisetimer.R
import com.jiny.raisetimer.ui.rememberLogoImageBitmap
import com.jiny.raisetimer.ui.TournamentViewModel
import com.jiny.raisetimer.ui.theme.LocalRaiseTimerPalette

private val PRESETS = listOf(15, 30, 45, 60, 90, 120)

@Composable
fun TurnTimerScreen(
    contentPadding: PaddingValues,
    turnTimerViewModel: TurnTimerViewModel = viewModel(),
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    logoFileName: String? = null,
    timeChipSeconds: Int = 30,
    onUpdateTimeChip: (Int) -> Unit = {},
) {
    val palette = LocalRaiseTimerPalette.current
    val timerState by turnTimerViewModel.state.collectAsStateWithLifecycle()

    val progress = if (timerState.totalSeconds > 0)
        (timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()).coerceAtMost(1f)
    else 0f

    val progressColor = when {
        timerState.remainingSeconds == 0 -> palette.chipRed
        progress <= 0.2f -> palette.chipRed
        progress <= 0.4f -> palette.chipGold
        else -> palette.chipGoldSoft
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Logo background (fullscreen only)
        if (isFullscreen && logoFileName != null) {
            val context = LocalContext.current
            val logoBitmap = rememberLogoImageBitmap(context, logoFileName)
            if (logoBitmap != null) {
                Image(
                    bitmap = logoBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .alpha(0.12f),
                )
            }
        }

        if (isFullscreen) {
            fullscreenLayout(
                timerState = timerState,
                progress = progress,
                progressColor = progressColor,
                timeChipSeconds = timeChipSeconds,
                onSelectPreset = { turnTimerViewModel.selectPreset(it) },
                onToggle = { turnTimerViewModel.toggle() },
                onReset = { turnTimerViewModel.reset() },
                onAddTime = { turnTimerViewModel.addTime(timeChipSeconds) },
                onClose = onToggleFullscreen,
            )
        } else {
            normalLayout(
                timerState = timerState,
                progress = progress,
                progressColor = progressColor,
                contentPadding = contentPadding,
                timeChipSeconds = timeChipSeconds,
                onSelectPreset = { turnTimerViewModel.selectPreset(it) },
                onToggle = { turnTimerViewModel.toggle() },
                onReset = { turnTimerViewModel.reset() },
                onAddTime = { turnTimerViewModel.addTime(timeChipSeconds) },
                onUpdateTimeChip = onUpdateTimeChip,
                onToggleFullscreen = onToggleFullscreen,
            )
        }
    }
}

@Composable
private fun normalLayout(
    timerState: TurnTimerState,
    progress: Float,
    progressColor: Color,
    contentPadding: PaddingValues,
    timeChipSeconds: Int,
    onSelectPreset: (Int) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onAddTime: () -> Unit,
    onUpdateTimeChip: (Int) -> Unit,
    onToggleFullscreen: (() -> Unit)?,
) {
    val palette = LocalRaiseTimerPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.turn_timer_title),
                style = MaterialTheme.typography.titleLarge,
                color = palette.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (onToggleFullscreen != null) {
                FilledIconButton(
                    onClick = onToggleFullscreen,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = palette.surfaceHighlight,
                        contentColor = palette.onSurface,
                    ),
                ) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = stringResource(R.string.timer_enter_fullscreen))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        presetChips(timerState, onSelectPreset)

        Spacer(modifier = Modifier.weight(1f))

        timerRing(
            remainingSeconds = timerState.remainingSeconds,
            hasAlerted = timerState.hasAlerted,
            progress = progress,
            progressColor = progressColor,
            ringSize = 280.dp,
            fontSize = 64,
        )

        Spacer(modifier = Modifier.weight(1f))

        timeChipField(timeChipSeconds, onUpdateTimeChip)

        Spacer(modifier = Modifier.height(12.dp))

        controlRow(timerState, timeChipSeconds, onToggle, onReset, onAddTime)
    }
}

@Composable
private fun fullscreenLayout(
    timerState: TurnTimerState,
    progress: Float,
    progressColor: Color,
    timeChipSeconds: Int,
    onSelectPreset: (Int) -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onAddTime: () -> Unit,
    onClose: (() -> Unit)?,
) {
    val palette = LocalRaiseTimerPalette.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // Left: timer ring
        timerRing(
            remainingSeconds = timerState.remainingSeconds,
            hasAlerted = timerState.hasAlerted,
            progress = progress,
            progressColor = progressColor,
            ringSize = 260.dp,
            fontSize = 56,
        )

        // Right: presets + controls
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Close button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onClose != null) {
                    FilledIconButton(
                        onClick = onClose,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = palette.surfaceElevated.copy(alpha = 0.92f),
                            contentColor = palette.onSurface,
                        ),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.timer_exit_fullscreen))
                    }
                }
            }

            presetChips(timerState, onSelectPreset)

            Spacer(modifier = Modifier.weight(1f))

            controlRow(timerState, timeChipSeconds, onToggle, onReset, onAddTime)
        }
    }
}

@Composable
private fun presetChips(timerState: TurnTimerState, onSelectPreset: (Int) -> Unit) {
    val palette = LocalRaiseTimerPalette.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PRESETS) { seconds ->
            FilterChip(
                selected = timerState.totalSeconds == seconds,
                onClick = { onSelectPreset(seconds) },
                label = {
                    Text(
                        text = formatPresetLabel(seconds),
                        fontWeight = FontWeight.Bold,
                    )
                },
                enabled = !timerState.isRunning,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.chipGold,
                    selectedLabelColor = palette.feltGreenDark,
                    containerColor = palette.surfaceHighlight,
                    labelColor = palette.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun timerRing(
    remainingSeconds: Int,
    hasAlerted: Boolean,
    progress: Float,
    progressColor: Color,
    ringSize: Dp,
    fontSize: Int,
) {
    val palette = LocalRaiseTimerPalette.current
    Box(
        modifier = Modifier.size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset(
                (size.width - radius * 2) / 2,
                (size.height - radius * 2) / 2,
            )
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(
                color = palette.surfaceHighlight,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTime(remainingSeconds),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = progressColor,
            )
            if (remainingSeconds == 0 && hasAlerted) {
                Text(
                    text = stringResource(R.string.turn_timer_times_up),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.chipRed,
                )
            }
        }
    }
}

@Composable
private fun timeChipField(
    timeChipSeconds: Int,
    onUpdateTimeChip: (Int) -> Unit,
) {
    val palette = LocalRaiseTimerPalette.current
    var text by remember { mutableStateOf(timeChipSeconds.toString()) }

    LaunchedEffect(timeChipSeconds) {
        if (text.toIntOrNull() != timeChipSeconds) {
            text = timeChipSeconds.toString()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.turn_timer_time_chip),
            color = palette.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(3)
                text = digits
                val parsed = digits.toIntOrNull()
                if (parsed != null && parsed in 1..600) {
                    onUpdateTimeChip(parsed)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(96.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = palette.onSurface,
                unfocusedTextColor = palette.onSurface,
                focusedBorderColor = palette.chipGold,
                unfocusedBorderColor = palette.surfaceHighlight,
                cursorColor = palette.chipGold,
            ),
        )
        Text(
            text = stringResource(R.string.turn_timer_seconds_unit),
            color = palette.onSurface,
        )
    }
}

@Composable
private fun controlRow(
    timerState: TurnTimerState,
    timeChipSeconds: Int,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onAddTime: () -> Unit,
) {
    val palette = LocalRaiseTimerPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.surfaceHighlight,
                contentColor = palette.onSurface,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(
                text = stringResource(R.string.turn_timer_reset),
                modifier = Modifier.padding(start = 6.dp),
                fontWeight = FontWeight.Bold,
            )
        }

        Button(
            onClick = onAddTime,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.feltGreen,
                contentColor = palette.onSurface,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.AddCircle, contentDescription = null)
            Text(
                text = stringResource(R.string.turn_timer_add_time, timeChipSeconds),
                modifier = Modifier.padding(start = 6.dp),
                fontWeight = FontWeight.Bold,
            )
        }

        Button(
            onClick = onToggle,
            enabled = !(timerState.remainingSeconds == 0 && !timerState.hasAlerted),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.chipGold,
                contentColor = palette.feltGreenDark,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(
                if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
            )
            Text(
                text = if (timerState.isRunning) stringResource(R.string.turn_timer_pause) else stringResource(R.string.turn_timer_start),
                modifier = Modifier.padding(start = 6.dp),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun formatPresetLabel(seconds: Int): String {
    if (seconds >= 60) {
        val m = seconds / 60
        val s = seconds % 60
        return if (s > 0) "${m}m ${s}s" else "${m}m"
    }
    return "${seconds}s"
}
