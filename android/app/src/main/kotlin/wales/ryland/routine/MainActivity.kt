package wales.ryland.routine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import wales.ryland.routine.ui.RoutineApp
import wales.ryland.routine.ui.RoutineColors
import wales.ryland.routine.ui.RoutineTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RoutineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark, so system bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            RoutineTheme {
                val state by viewModel.state.collectAsState()
                Box(Modifier.fillMaxSize().background(RoutineColors.Background)) {
                    state?.let { RoutineApp(it, viewModel) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshToday()
    }
}
