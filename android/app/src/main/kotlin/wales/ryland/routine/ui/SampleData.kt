package wales.ryland.routine.ui

import java.time.LocalDate
import wales.ryland.routine.core.Routine

/** A believable ten days of history, for previews. */
fun sampleRoutineState(today: LocalDate = LocalDate.now()): RoutineUiState {
    val start = today.minusDays(9)
    var routine = Routine(startDate = start)
    val samples = listOf("Teeth AM" to 1, "Face AM" to 1, "SPF" to 1, "Shower" to 1, "Daily greens" to 1, "Water" to 6)
    samples.forEachIndexed { index, (name, goal) ->
        routine = routine.withHabitAdded(name, goal, start)
        val id = routine.habits.last().id
        for (offset in 0..9) {
            if ((offset + index) % 3 == 0 && offset <= 6) continue
            repeat(if (goal == 1) 1 else (offset * 2) % (goal + 1)) {
                routine = routine.tapped(id, start.plusDays(offset.toLong()))
            }
        }
    }
    return RoutineUiState(routine, today)
}

/** Does nothing; for previews. */
object NoRoutineActions : RoutineActions {
    override fun tap(habitId: String, day: LocalDate) = Unit
    override fun addHabit(name: String, goal: Int) = Unit
    override fun rename(habitId: String, name: String) = Unit
    override fun setGoal(habitId: String, goal: Int) = Unit
    override fun delete(habitId: String) = Unit
    override fun move(from: Int, to: Int) = Unit
}
