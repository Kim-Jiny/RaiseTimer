package com.jiny.raisetimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jiny.raisetimer.ui.RaiseTimerApp
import com.jiny.raisetimer.ui.theme.RaiseTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaiseTimerTheme {
                RaiseTimerApp()
            }
        }
    }
}
