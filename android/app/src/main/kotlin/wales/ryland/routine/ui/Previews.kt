package wales.ryland.routine.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import wales.ryland.routine.core.Routine

@Preview(name = "Routine", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF150107)
@Composable
private fun RoutinePreview() {
    RoutineTheme { RoutineApp(sampleRoutineState(), NoRoutineActions) }
}

@Preview(name = "Empty", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF150107)
@Composable
private fun EmptyPreview() {
    RoutineTheme { RoutineApp(RoutineUiState(Routine(startDate = LocalDate.now()), LocalDate.now()), NoRoutineActions) }
}

@Preview(name = "Edit habits", widthDp = 393, heightDp = 852, showBackground = true, backgroundColor = 0xFF150107)
@Composable
private fun EditPreview() {
    RoutineTheme { EditHabitsScreen(sampleRoutineState().routine.habits, NoRoutineActions, onDone = {}) }
}
