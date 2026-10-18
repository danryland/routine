package wales.ryland.routine

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wales.ryland.routine.core.ReviewPolicy
import wales.ryland.routine.core.Routine
import wales.ryland.routine.core.RoutineCsv
import wales.ryland.routine.core.RoutineJson
import wales.ryland.routine.core.RoutineStore
import wales.ryland.routine.core.canAddHabit
import wales.ryland.routine.core.isPerfect
import wales.ryland.routine.core.perfectDayCount
import wales.ryland.routine.core.restoreRoutine
import wales.ryland.routine.data.Storage
import wales.ryland.routine.reminders.Reminders
import wales.ryland.routine.ui.RoutineActions
import wales.ryland.routine.ui.RoutineUiState
import wales.ryland.routine.widget.TodayWidget

/** Owns the routine for the UI, saves every change, and keeps the widget in step. */
class RoutineViewModel(application: Application) : AndroidViewModel(application), RoutineActions {
    private val app = application as RoutineApplication
    private val settings = app.settings
    private val billing = app.billing

    private data class Loaded(val routine: Routine, val today: LocalDate)

    private val loaded = MutableStateFlow<Loaded?>(null)

    /** Null until the saved routine has been read from disk. */
    val state: StateFlow<RoutineUiState?> = combine(loaded, settings.values, billing.state) { loaded, settings, billing ->
        loaded?.let {
            RoutineUiState(
                routine = it.routine,
                today = it.today,
                isUnlocked = settings.isUnlocked,
                isEarlySupporter = settings.earlySupporter,
                unlockPrice = billing.unlockPrice,
                tips = billing.tips,
                themeId = settings.themeId,
                remindersOn = settings.remindersOn,
                reminderMinute = settings.reminderMinute,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _reviewDue = MutableStateFlow(false)

    /** True when it's a good moment to ask for a Play Store rating. */
    val reviewDue: StateFlow<Boolean> = _reviewDue.asStateFlow()

    init {
        viewModelScope.launch {
            val today = LocalDate.now()
            val result = withContext(Dispatchers.IO) { Storage.load(app, today) }
            if (result.origin == RoutineStore.Origin.ImportedFromLegacyApp) {
                settings.setEarlySupporter()
            }
            loaded.value = Loaded(result.routine, today)

            // Ticks made in the widget.
            launch {
                Storage.externalChanges.collect {
                    val routine = withContext(Dispatchers.IO) { Storage.load(app).routine }
                    loaded.update { current -> current?.copy(routine = routine) }
                }
            }
            // Unlocking (or a theme change) changes what the widget and reminders show.
            launch {
                settings.values.map { it.isUnlocked to it.themeId }.distinctUntilChanged().drop(1).collect {
                    TodayWidget.refresh(app)
                    Reminders.schedule(app)
                }
            }
            // Add tomorrow's column at midnight if the app is open.
            while (isActive) {
                delay(millisUntilTomorrow())
                refreshToday()
            }
        }
    }

    /** Call when the app comes to the foreground. */
    fun onResume() {
        refreshToday()
        billing.refresh()
        Reminders.schedule(app)
    }

    private fun refreshToday() {
        val today = LocalDate.now()
        loaded.update { current -> if (current == null || current.today == today) current else current.copy(today = today) }
    }

    // MARK: Routine edits

    override fun tap(habitId: String, day: LocalDate) {
        val before = loaded.value ?: return
        edit { routine, _ -> routine.tapped(habitId, day) }
        val after = loaded.value?.routine ?: return
        if (day == before.today && !before.routine.isPerfect(day) && after.isPerfect(day)) {
            val lastAsked = settings.current.lastReviewRequest
            if (ReviewPolicy.shouldAsk(after.perfectDayCount(day), lastAsked, day)) _reviewDue.value = true
        }
    }

    override fun addHabit(name: String, goal: Int) = edit { routine, today ->
        if (routine.canAddHabit(settings.current.isUnlocked)) routine.withHabitAdded(name, goal, today) else routine
    }

    override fun rename(habitId: String, name: String) = edit { routine, _ -> routine.renamed(habitId, name) }

    override fun setGoal(habitId: String, goal: Int) = edit { routine, _ -> routine.withGoal(habitId, goal) }

    override fun delete(habitId: String) = edit { routine, _ -> routine.removing(habitId) }

    override fun move(from: Int, to: Int) = edit { routine, _ -> routine.moving(from, to) }

    override fun setTheme(id: String) = settings.setTheme(id)

    // MARK: Settings and files

    fun setReminders(on: Boolean, minuteOfDay: Int) {
        settings.setReminders(on, minuteOfDay)
        Reminders.schedule(app)
    }

    fun didRequestReview() {
        _reviewDue.value = false
        settings.setLastReviewRequest(LocalDate.now())
    }

    fun csv(): String? = loaded.value?.let { RoutineCsv.export(it.routine, it.today) }

    fun backupJson(): String? = loaded.value?.let { RoutineJson.encode(it.routine) }

    /** Replaces everything with a backup. Returns false if it isn't one. */
    fun restore(text: String): Boolean {
        val restored = restoreRoutine(text, LocalDate.now()) ?: return false
        edit { _, _ -> restored }
        return true
    }

    private fun edit(change: (Routine, LocalDate) -> Routine) {
        val current = loaded.value ?: return
        val edited = change(current.routine, current.today)
        if (edited == current.routine) return
        loaded.value = current.copy(routine = edited)

        viewModelScope.launch(Dispatchers.IO) {
            // Always write the newest state, so saves can't land out of order.
            val latest = loaded.value?.routine ?: return@launch
            try {
                Storage.save(app, latest)
            } catch (e: IOException) {
                Log.e("Routine", "Couldn't save routine", e)
            }
            TodayWidget.refresh(app)
        }
    }

    private fun millisUntilTomorrow(): Long {
        val midnight = LocalDate.now().plusDays(1).atStartOfDay()
        return Duration.between(LocalDateTime.now(), midnight).toMillis() + 1_000
    }
}
