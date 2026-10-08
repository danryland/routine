package wales.ryland.routine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import wales.ryland.routine.core.Routine

data class RoutineUiState(val routine: Routine, val today: LocalDate) {
    val days: List<LocalDate> = routine.days(through = today)
}

interface RoutineActions {
    fun tap(habitId: String, day: LocalDate)
    fun addHabit(name: String, goal: Int)
    fun rename(habitId: String, name: String)
    fun setGoal(habitId: String, goal: Int)
    fun delete(habitId: String)
    fun move(from: Int, to: Int)
}

@Composable
fun RoutineApp(state: RoutineUiState, actions: RoutineActions) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var isAdding by rememberSaveable { mutableStateOf(false) }

    if (isEditing) {
        SystemBackHandler { isEditing = false }
        EditHabitsScreen(habits = state.routine.habits, actions = actions, onDone = { isEditing = false })
    } else {
        RoutineScreen(
            state = state,
            onTap = actions::tap,
            onEdit = { isEditing = true },
            onAdd = { isAdding = true },
        )
    }

    if (isAdding) {
        NewHabitDialog(
            onDismiss = { isAdding = false },
            onAdd = { name, goal ->
                actions.addHabit(name, goal)
                isAdding = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    state: RoutineUiState,
    onTap: (habitId: String, day: LocalDate) -> Unit,
    onEdit: () -> Unit,
    onAdd: () -> Unit,
) {
    val hasHabits = state.routine.habits.isNotEmpty()
    Scaffold(
        containerColor = RoutineColors.Background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Routine", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = RoutineColors.Background),
                )
                HorizontalDivider(color = RoutineColors.Maroon)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = RoutineColors.Maroon)
                BottomAppBar(
                    containerColor = RoutineColors.Background,
                    actions = {
                        IconButton(onClick = onEdit, enabled = hasHabits) {
                            Icon(RoutineIcons.Edit, contentDescription = "Edit habits")
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = onAdd,
                            containerColor = RoutineColors.Rose,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        ) {
                            Icon(RoutineIcons.Add, contentDescription = "New habit")
                        }
                    },
                )
            }
        },
    ) { padding ->
        if (hasHabits) {
            RoutineGrid(state = state, onTap = onTap, modifier = Modifier.padding(padding))
        } else {
            EmptyState(onAdd = onAdd, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(RoutineIcons.Check, contentDescription = null, tint = RoutineColors.Rose, modifier = Modifier.size(56.dp))
            Text("Build your routine", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Add the small things you want to do every day, then tick them off as you go.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAdd, modifier = Modifier.padding(top = 8.dp)) {
                Text("Add a habit")
            }
        }
    }
}
