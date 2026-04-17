package com.jiny.raisetimer.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jiny.raisetimer.R
import com.jiny.raisetimer.ui.TournamentViewModel
import com.jiny.raisetimer.ui.players.PlayersScreen
import com.jiny.raisetimer.ui.structure.StructureScreen
import com.jiny.raisetimer.ui.theme.LocalRaiseTimerPalette

@Composable
fun TournamentSetupScreen(viewModel: TournamentViewModel, contentPadding: PaddingValues) {
    val palette = LocalRaiseTimerPalette.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_players),
        stringResource(R.string.tab_structure),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = palette.feltGreenDark,
            contentColor = palette.chipGold,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = palette.chipGold,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) palette.chipGold else palette.onSurfaceMuted,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            0 -> PlayersScreen(viewModel, contentPadding)
            1 -> StructureScreen(viewModel, contentPadding)
        }
    }
}
