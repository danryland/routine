package wales.ryland.routine.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import wales.ryland.routine.core.Routine

@Preview(name = "Routine", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF150107)
@Composable
private fun RoutinePreview() {
    RoutineApp(sampleRoutineState(), NoRoutineActions, NoAppActions)
}

@Preview(name = "Empty", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF150107)
@Composable
private fun EmptyPreview() {
    RoutineApp(RoutineUiState(Routine(startDate = LocalDate.now()), LocalDate.now()), NoRoutineActions, NoAppActions)
}

@Preview(name = "Stats", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF150107)
@Composable
private fun StatsPreview() {
    RoutineTheme { StatsScreen(sampleRoutineState(), onBack = {}, onUnlock = {}) }
}

@Preview(name = "Settings", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF150107)
@Composable
private fun SettingsPreview() {
    RoutineTheme { SettingsScreen(sampleRoutineState(isUnlocked = false), NoRoutineActions, NoAppActions, onBack = {}, onLocked = {}) }
}
