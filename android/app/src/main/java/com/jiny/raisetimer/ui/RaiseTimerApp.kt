package com.jiny.raisetimer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.jiny.raisetimer.R
import androidx.compose.ui.platform.LocalContext
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jiny.raisetimer.ui.payout.PayoutScreen
import com.jiny.raisetimer.ui.settings.SettingsScreen
import com.jiny.raisetimer.ui.setup.TournamentSetupScreen
import com.jiny.raisetimer.ui.timer.TimerScreen
import com.jiny.raisetimer.ui.turntimer.TurnTimerScreen
import com.jiny.raisetimer.ui.theme.LocalRaiseTimerPalette
import com.jiny.raisetimer.ui.turntimer.TurnTimerViewModel

private sealed class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Timer : Tab("timer", R.string.tab_timer, Icons.Filled.Timer)
    data object Setup : Tab("setup", R.string.tab_setup, Icons.Filled.Groups)
    data object TurnTimer : Tab("turn_timer", R.string.tab_turn_timer, Icons.Filled.AvTimer)
    data object Payout : Tab("payout", R.string.tab_payout, Icons.Filled.AttachMoney)
    data object Settings : Tab("settings", R.string.tab_settings, Icons.Filled.Palette)
}

private val tabs = listOf(Tab.Timer, Tab.Setup, Tab.TurnTimer, Tab.Payout, Tab.Settings)
private const val TimerFullscreenRoute = "timer_fullscreen"
private const val TurnTimerFullscreenRoute = "turn_timer_fullscreen"

@Composable
fun RaiseTimerApp(
    viewModel: TournamentViewModel = viewModel(),
    turnTimerViewModel: TurnTimerViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val tournamentState by viewModel.state.collectAsStateWithLifecycle()
    val turnTimerState by turnTimerViewModel.state.collectAsStateWithLifecycle()
    val palette = LocalRaiseTimerPalette.current

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

    // Keep the screen awake while either the tournament clock or the dealer turn timer is
    // running so players can glance at it without having to tap to unlock. Cleared
    // automatically when both stop, or this composable leaves composition.
    val view = LocalView.current
    val keepScreenOn = tournamentState.isRunning || turnTimerState.isRunning
    DisposableEffect(keepScreenOn) {
        val window = (view.context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val isFullscreenTimer = currentDestination?.route == TimerFullscreenRoute
    val isFullscreenTurnTimer = currentDestination?.route == TurnTimerFullscreenRoute
    val isAnyFullscreen = isFullscreenTimer || isFullscreenTurnTimer
    DisposableEffect(isAnyFullscreen, view) {
        val activity = view.context as? Activity
        if (isAnyFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold(
        containerColor = palette.feltGreenDark,
        bottomBar = {
            if (isAnyFullscreen) return@Scaffold
            NavigationBar(
                containerColor = palette.feltGreenDark,
            ) {
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
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.chipGold,
                            selectedTextColor = palette.chipGold,
                            unselectedIconColor = palette.onSurfaceMuted,
                            unselectedTextColor = palette.onSurfaceMuted,
                            indicatorColor = palette.chipGold.copy(alpha = 0.12f),
                        ),
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
            composable(Tab.Timer.route) {
                TimerScreen(
                    viewModel = viewModel,
                    contentPadding = PaddingValues(),
                    isFullscreen = false,
                    onStartFullscreen = {
                        viewModel.start()
                        navController.navigate(TimerFullscreenRoute)
                    },
                    onToggleFullscreen = { navController.navigate(TimerFullscreenRoute) },
                )
            }
            composable(TimerFullscreenRoute) {
                TimerFullscreenRoute(
                    viewModel = viewModel,
                    onClose = { navController.popBackStack() },
                )
            }
            composable(Tab.Setup.route) { TournamentSetupScreen(viewModel, PaddingValues()) }
            composable(Tab.TurnTimer.route) {
                val tournamentState by viewModel.state.collectAsStateWithLifecycle()
                TurnTimerScreen(
                    contentPadding = PaddingValues(),
                    turnTimerViewModel = turnTimerViewModel,
                    onToggleFullscreen = { navController.navigate(TurnTimerFullscreenRoute) },
                    timeChipSeconds = tournamentState.config.turnTimeChipSeconds,
                    onUpdateTimeChip = { seconds ->
                        viewModel.updateConfig { it.copy(turnTimeChipSeconds = seconds.coerceIn(1, 600)) }
                    },
                )
            }
            composable(TurnTimerFullscreenRoute) {
                TurnTimerFullscreenRoute(
                    tournamentViewModel = viewModel,
                    turnTimerViewModel = turnTimerViewModel,
                    onClose = { navController.popBackStack() },
                )
            }
            composable(Tab.Payout.route) { PayoutScreen(viewModel, PaddingValues()) }
            composable(Tab.Settings.route) { SettingsScreen(viewModel, PaddingValues()) }
        }
    }
}

@Composable
private fun TimerFullscreenRoute(
    viewModel: TournamentViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val palette = LocalRaiseTimerPalette.current

    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previousBehavior = controller?.systemBarsBehavior
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (previousBehavior != null) {
                controller.systemBarsBehavior = previousBehavior
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.timerGradient.first().copy(alpha = 0.16f),
                        palette.timerGradient.getOrElse(1) { palette.feltGreenDark },
                        palette.timerGradient.getOrElse(2) { palette.feltGreen },
                    )
                )
            )
    ) {
        TimerScreen(
            viewModel = viewModel,
            contentPadding = PaddingValues(),
            isFullscreen = true,
            onToggleFullscreen = onClose,
        )
    }
}

@Composable
private fun TurnTimerFullscreenRoute(
    tournamentViewModel: TournamentViewModel,
    turnTimerViewModel: TurnTimerViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val palette = LocalRaiseTimerPalette.current
    val tournamentState by tournamentViewModel.state.collectAsStateWithLifecycle()

    DisposableEffect(activity) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previousBehavior = controller?.systemBarsBehavior
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (previousBehavior != null) {
                controller.systemBarsBehavior = previousBehavior
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.timerGradient.first().copy(alpha = 0.16f),
                        palette.timerGradient.getOrElse(1) { palette.feltGreenDark },
                        palette.timerGradient.getOrElse(2) { palette.feltGreen },
                    )
                )
            )
    ) {
        TurnTimerScreen(
            contentPadding = PaddingValues(),
            turnTimerViewModel = turnTimerViewModel,
            isFullscreen = true,
            onToggleFullscreen = onClose,
            logoFileName = tournamentState.config.fullscreenLogoFileName,
            timeChipSeconds = tournamentState.config.turnTimeChipSeconds,
        )
    }
}
