package wales.ryland.routine.data

import android.content.Context
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import wales.ryland.routine.core.LegacyImport
import wales.ryland.routine.core.Routine
import wales.ryland.routine.core.RoutineStore

/**
 * The one routine file, shared by the app, its widget and reminders (all in
 * this process). Changes go through [edit] so they can't overwrite each other.
 */
object Storage {
    private val lock = Mutex()
    private val _externalChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires when something other than the app's screen (e.g. the widget) changes the routine. */
    val externalChanges: SharedFlow<Unit> = _externalChanges

    fun file(context: Context): File = File(context.filesDir, "routine.json")

    fun store(context: Context): RoutineStore = RoutineStore(file(context)) {
        context
            .getSharedPreferences(LegacyImport.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(LegacyImport.PREFERENCES_KEY, null)
    }

    suspend fun load(context: Context, today: LocalDate = LocalDate.now()): RoutineStore.Loaded =
        lock.withLock { store(context).loadReportingOrigin(today) }

    suspend fun save(context: Context, routine: Routine) = lock.withLock { store(context).save(routine) }

    /** Loads, changes and saves in one go, then tells the app. */
    suspend fun edit(context: Context, change: (Routine) -> Routine) {
        lock.withLock {
            val store = store(context)
            store.save(change(store.load(LocalDate.now())))
        }
        _externalChanges.tryEmit(Unit)
    }
}
