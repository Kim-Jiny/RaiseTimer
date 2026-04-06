package com.jiny.raisetimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiny.raisetimer.ui.RaiseTimerApp
import com.jiny.raisetimer.ui.TournamentViewModel
import com.jiny.raisetimer.ui.theme.RaiseTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val tournamentViewModel: TournamentViewModel = viewModel()
            val state = tournamentViewModel.state.collectAsStateWithLifecycle()
            RaiseTimerTheme(themePreset = state.value.config.themePreset) {
                RaiseTimerApp(viewModel = tournamentViewModel)
            }
        }
    }
}
