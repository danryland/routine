package wales.ryland.routine

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import wales.ryland.routine.core.LegacyImport
import wales.ryland.routine.core.Routine
import wales.ryland.routine.core.RoutineStore
import wales.ryland.routine.ui.RoutineActions
import wales.ryland.routine.ui.RoutineUiState

/** Owns the routine for the UI, and saves every change straight away. */
class RoutineViewModel(application: Application) : AndroidViewModel(application), RoutineActions {
    private val store = RoutineStore(File(application.filesDir, "routine.json")) {
        application
            .getSharedPreferences(LegacyImport.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(LegacyImport.PREFERENCES_KEY, null)
    }
    private val saveLock = Mutex()

    private val _state = MutableStateFlow<RoutineUiState?>(null)

    /** Null until the saved routine has been read from disk. */
    val state: StateFlow<RoutineUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val today = LocalDate.now()
            val routine = withContext(Dispatchers.IO) { store.load(today) }
            _state.value = RoutineUiState(routine, today)

            // Add tomorrow's column at midnight if the app is open.
            while (isActive) {
                delay(millisUntilTomorrow())
                refreshToday()
            }
        }
    }

    /** Call when the app comes to the foreground, in case the date changed. */
    fun refreshToday() {
        val today = LocalDate.now()
        _state.update { current -> if (current == null || current.today == today) current else current.copy(today = today) }
    }

    override fun tap(habitId: String, day: LocalDate) = edit { routine, _ -> routine.tapped(habitId, day) }

    override fun addHabit(name: String, goal: Int) = edit { routine, today -> routine.withHabitAdded(name, goal, today) }

    override fun rename(habitId: String, name: String) = edit { routine, _ -> routine.renamed(habitId, name) }

    override fun setGoal(habitId: String, goal: Int) = edit { routine, _ -> routine.withGoal(habitId, goal) }

    override fun delete(habitId: String) = edit { routine, _ -> routine.removing(habitId) }

    override fun move(from: Int, to: Int) = edit { routine, _ -> routine.moving(from, to) }

    private fun edit(change: (Routine, LocalDate) -> Routine) {
        val current = _state.value ?: return
        val edited = change(current.routine, current.today)
        if (edited == current.routine) return
        _state.value = current.copy(routine = edited)

        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock {
                // Always write the newest state, so saves can't land out of order.
                val latest = _state.value?.routine ?: return@withLock
                try {
                    store.save(latest)
                } catch (e: IOException) {
                    Log.e("Routine", "Couldn't save routine", e)
                }
            }
        }
    }

    private fun millisUntilTomorrow(): Long {
        val midnight = LocalDate.now().plusDays(1).atStartOfDay()
        return Duration.between(LocalDateTime.now(), midnight).toMillis() + 1_000
    }
}
