package com.jiny.raisetimer.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jiny.raisetimer.domain.model.ThemePreset
import com.jiny.raisetimer.ui.TournamentViewModel
import com.jiny.raisetimer.ui.LogoStorage
import com.jiny.raisetimer.ui.rememberLogoImageBitmap
import com.jiny.raisetimer.ui.theme.LocalRaiseTimerPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: TournamentViewModel, contentPadding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val slotSummaries by viewModel.slotSummaries.collectAsStateWithLifecycle()
    val currentTournamentName by viewModel.currentTournamentName.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val palette = LocalRaiseTimerPalette.current
    val logoBitmap = rememberLogoImageBitmap(context, state.config.fullscreenLogoFileName)
    var newTournamentName by remember { mutableStateOf("") }
    var slotsExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(true) }
    var logoExpanded by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        viewModel.updateFullscreenLogoFileName(LogoStorage.saveFromUri(context, uri))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("설정", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("빠른 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "현재 사용 중인 핵심 설정만 먼저 보여줍니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                SettingSummaryRow(label = "현재 슬롯", value = currentTournamentName)
                SettingSummaryRow(
                    label = "테마",
                    value = state.config.themePreset.name.lowercase().replaceFirstChar { it.titlecase() },
                )
                SettingSummaryRow(
                    label = "전체화면 로고",
                    value = if (logoBitmap == null) "사용 안 함" else "적용됨",
                )
            }
        }

        SettingsSectionCard(
            title = "테마 컬러",
            summary = state.config.themePreset.name.lowercase().replaceFirstChar { it.titlecase() },
            expanded = themeExpanded,
            onToggle = { themeExpanded = !themeExpanded },
        ) {
            Text(
                text = "앱 포인트 컬러와 타이머 분위기를 프리셋으로 바꿉니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemePreset.entries.chunked(3).forEach { presetRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        presetRow.forEach { preset ->
                            ThemePresetCard(
                                themePreset = preset,
                                selected = state.config.themePreset == preset,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.updateThemePreset(preset) },
                            )
                        }
                        repeat(3 - presetRow.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        SettingsSectionCard(
            title = "전체화면 로고",
            summary = if (logoBitmap == null) "사용 안 함" else "적용됨",
            expanded = logoExpanded,
            onToggle = { logoExpanded = !logoExpanded },
        ) {
            Text(
                text = "선택한 이미지는 전체화면 타이머 배경에 은은하게 표시됩니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(palette.timerGradient), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (logoBitmap != null) {
                    Image(
                        bitmap = logoBitmap,
                        contentDescription = "로고 미리보기",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .alpha(0.16f),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    text = "00:45",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (logoBitmap == null) "로고 선택" else "로고 변경")
                }
                OutlinedButton(
                    onClick = { viewModel.updateFullscreenLogoFileName(null) },
                    enabled = logoBitmap != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("로고 제거")
                }
            }
        }

        SettingsSectionCard(
            title = "토너먼트 슬롯",
            summary = currentTournamentName,
            expanded = slotsExpanded,
            onToggle = { slotsExpanded = !slotsExpanded },
        ) {
            Text(
                text = "현재 슬롯을 바꾸거나 새 슬롯을 만들어 여러 토너먼트를 관리합니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newTournamentName,
                    onValueChange = { newTournamentName = it },
                    label = { Text("새 슬롯 이름") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        viewModel.createTournamentSlot(newTournamentName)
                        newTournamentName = ""
                    },
                    enabled = newTournamentName.isNotBlank(),
                ) {
                    Text("생성")
                }
            }

            slotSummaries.forEach { slot ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (slot.isCurrent) "${slot.name} (현재)" else slot.name,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${slot.activePlayerCount}/${slot.playerCount}명 · ${slot.updatedAt.asSlotDate()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.switchTournamentSlot(slot.id) },
                            enabled = !slot.isCurrent,
                        ) {
                            Text("불러오기")
                        }
                        OutlinedButton(
                            onClick = { viewModel.deleteTournamentSlot(slot.id) },
                            enabled = slotSummaries.size > 1,
                        ) {
                            Text("삭제")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = if (expanded) "접기" else "열기",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            }
        }
    }
}

@Composable
private fun SettingSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ThemePresetCard(
    themePreset: ThemePreset,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                themePreviewColors(themePreset).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(color, RoundedCornerShape(999.dp))
                    )
                }
            }
            Text(
                text = themePreset.name.lowercase().replaceFirstChar { it.titlecase() },
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun themePreviewColors(themePreset: ThemePreset): List<Color> = when (themePreset) {
    ThemePreset.EMERALD -> listOf(Color(0xFF2B8A5A), Color(0xFFD4A017), Color(0xFF0F5132))
    ThemePreset.OCEAN -> listOf(Color(0xFF3BA7D6), Color(0xFF67D3F4), Color(0xFF123A52))
    ThemePreset.RUBY -> listOf(Color(0xFFC94B73), Color(0xFFF07AA2), Color(0xFF5D203B))
    ThemePreset.SUNSET -> listOf(Color(0xFFFF8A3D), Color(0xFFFFB36B), Color(0xFF6C3722))
    ThemePreset.LAVENDER -> listOf(Color(0xFFA884FF), Color(0xFFC7B2FF), Color(0xFF4B3570))
    ThemePreset.SLATE -> listOf(Color(0xFF8FB3C9), Color(0xFFBED3E0), Color(0xFF2B4252))
    ThemePreset.MINT -> listOf(Color(0xFF4FD1B5), Color(0xFF8AE9D4), Color(0xFF1C5C50))
    ThemePreset.CORAL -> listOf(Color(0xFFFF7A6B), Color(0xFFFFA597), Color(0xFF7D3B36))
    ThemePreset.MIDNIGHT -> listOf(Color(0xFF7CA6FF), Color(0xFFB5CBFF), Color(0xFF233A74))
}

private fun Long.asSlotDate(): String =
    SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(this))
