package wales.ryland.routine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import wales.ryland.routine.core.Routine
import wales.ryland.routine.core.canAddHabit

data class TipOption(val id: String, val title: String, val price: String)

data class RoutineUiState(
    val routine: Routine,
    val today: LocalDate,
    val isUnlocked: Boolean = false,
    val isEarlySupporter: Boolean = false,
    /** Formatted price of the unlock, or null if the store can't be reached. */
    val unlockPrice: String? = null,
    val tips: List<TipOption> = emptyList(),
    val themeId: String = Palette.Rose.id,
    val remindersOn: Boolean = false,
    val reminderMinute: Int = 20 * 60,
) {
    val days: List<LocalDate> = routine.days(through = today)
    val canAddHabit: Boolean get() = routine.canAddHabit(isUnlocked)
    val palette: Palette get() = Palette.named(themeId, isUnlocked)
}

/** Changes to the routine and settings. */
interface RoutineActions {
    fun tap(habitId: String, day: LocalDate)
    fun addHabit(name: String, goal: Int)
    fun rename(habitId: String, name: String)
    fun setGoal(habitId: String, goal: Int)
    fun delete(habitId: String)
    fun move(from: Int, to: Int)
    fun setTheme(id: String)
}

/** Things that need the Activity: purchases, permissions and files. */
interface AppActions {
    fun buyUnlock()
    fun buyTip(id: String)
    fun restorePurchases()
    fun setReminders(on: Boolean, minuteOfDay: Int)
    fun exportCsv()
    fun backUp()
    fun restoreBackup()
}

private enum class Screen { Routine, Edit, Stats, Settings }

/** What the paywall says the user was trying to do. */
private const val HABIT_LIMIT = "habit limit"

@Composable
fun RoutineApp(state: RoutineUiState, actions: RoutineActions, app: AppActions) {
    var screen by rememberSaveable { mutableStateOf(Screen.Routine) }
    var isAdding by rememberSaveable { mutableStateOf(false) }
    var paywallFor by rememberSaveable { mutableStateOf<String?>(null) }
    val back = { screen = Screen.Routine }

    RoutineTheme(palette = state.palette) {
        if (screen != Screen.Routine) SystemBackHandler(back)
        when (screen) {
            Screen.Routine -> RoutineScreen(
                state = state,
                onTap = actions::tap,
                onEdit = { screen = Screen.Edit },
                onStats = { screen = Screen.Stats },
                onSettings = { screen = Screen.Settings },
                onAdd = { if (state.canAddHabit) isAdding = true else paywallFor = HABIT_LIMIT },
            )
            Screen.Edit -> EditHabitsScreen(habits = state.routine.habits, actions = actions, onDone = back)
            Screen.Stats -> StatsScreen(state = state, onBack = back, onUnlock = { paywallFor = "Stats" })
            Screen.Settings -> SettingsScreen(state = state, actions = actions, app = app, onBack = back, onLocked = { paywallFor = it })
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

        paywallFor?.let { feature ->
            PaywallDialog(
                headline = if (feature == HABIT_LIMIT) {
                    "The free version tracks up to 3 habits. Unlock Routine once to add as many as you like, plus everything below."
                } else {
                    "$feature is part of Routine Pro. Unlock it once and it’s yours for good."
                },
                state = state,
                app = app,
                onDismiss = { paywallFor = null },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    state: RoutineUiState,
    onTap: (habitId: String, day: LocalDate) -> Unit,
    onEdit: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
) {
    val palette = LocalPalette.current
    val hasHabits = state.routine.habits.isNotEmpty()
    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Routine", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = palette.background),
                )
                HorizontalDivider(color = palette.track)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = palette.track)
                BottomAppBar(
                    containerColor = palette.background,
                    actions = {
                        IconButton(onClick = onEdit, enabled = hasHabits) {
                            Icon(RoutineIcons.Edit, contentDescription = "Edit habits")
                        }
                        IconButton(onClick = onStats) {
                            Icon(Icons.Rounded.BarChart, contentDescription = "Stats")
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = onAdd,
                            containerColor = palette.accent,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                            modifier = Modifier.testTag("newHabit"),
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
            Icon(RoutineIcons.Check, contentDescription = null, tint = LocalPalette.current.accent, modifier = Modifier.size(56.dp))
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
