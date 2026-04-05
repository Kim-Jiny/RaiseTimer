package com.jiny.raisetimer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import android.app.Activity
import android.view.WindowManager
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jiny.raisetimer.ui.payout.PayoutScreen
import com.jiny.raisetimer.ui.players.PlayersScreen
import com.jiny.raisetimer.ui.structure.StructureScreen
import com.jiny.raisetimer.ui.timer.TimerScreen

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Timer : Tab("timer", "타이머", Icons.Filled.Timer)
    data object Players : Tab("players", "플레이어", Icons.Filled.Groups)
    data object Structure : Tab("structure", "블라인드", Icons.Filled.Tune)
    data object Payout : Tab("payout", "상금", Icons.Filled.AttachMoney)
}

private val tabs = listOf(Tab.Timer, Tab.Players, Tab.Structure, Tab.Payout)

@Composable
fun RaiseTimerApp(viewModel: TournamentViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val tournamentState by viewModel.state.collectAsStateWithLifecycle()

    // Re-sync the running timer against wall-clock time whenever the app comes back to
    // the foreground. Handles backgrounding, screen off, and cold restart uniformly.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.catchUpFromBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keep the screen awake while the timer is running so players can glance at it
    // without having to tap to unlock. Cleared automatically when the timer pauses,
    // finishes, or this composable leaves composition.
    val view = LocalView.current
    DisposableEffect(tournamentState.isRunning) {
        val window = (view.context as? Activity)?.window
        if (tournamentState.isRunning) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == tab.route }
                        ?: (tab == Tab.Timer)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Timer.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(Tab.Timer.route) { TimerScreen(viewModel, PaddingValues()) }
            composable(Tab.Players.route) { PlayersScreen(viewModel, PaddingValues()) }
            composable(Tab.Structure.route) { StructureScreen(viewModel, PaddingValues()) }
            composable(Tab.Payout.route) { PayoutScreen(viewModel, PaddingValues()) }
        }
    }
}
