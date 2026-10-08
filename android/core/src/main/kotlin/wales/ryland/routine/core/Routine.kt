package wales.ryland.routine.core

import java.time.LocalDate
import java.util.UUID

/**
 * Something the user wants to do every day.
 *
 * [goal] is how many times a day it needs doing: `1` is a simple tick box,
 * anything higher is a counter (e.g. "Water ×6") that fills up as it's tapped.
 * [counts] never holds zeros or values above [goal]; build habits with [Habit.of]
 * to keep it that way.
 */
@ConsistentCopyVisibility
data class Habit internal constructor(
    val id: String,
    val name: String,
    val goal: Int,
    /** The day the habit was added. Days before this don't count against it. */
    val createdOn: LocalDate,
    val counts: Map<LocalDate, Int>,
) {
    val isCounter: Boolean get() = goal > 1

    fun count(day: LocalDate): Int = counts[day] ?: 0

    fun isDone(day: LocalDate): Boolean = count(day) >= goal

    /**
     * The first day this habit counts towards the daily score: the day it was
     * created, or earlier if the user back-filled a day before that.
     */
    val firstTrackedDay: LocalDate
        get() = counts.keys.minOrNull()?.let { minOf(it, createdOn) } ?: createdOn

    fun isTracked(day: LocalDate): Boolean = day >= firstTrackedDay

    /**
     * A tick box toggles. A counter goes up by one, then wraps back to zero
     * after it reaches its goal (so a mistaken tap can always be undone).
     */
    fun tapped(day: LocalDate): Habit {
        val current = count(day)
        return withCount(if (current >= goal) 0 else current + 1, day)
    }

    fun withCount(count: Int, day: LocalDate): Habit {
        val clamped = count.coerceIn(0, goal)
        return copy(counts = if (clamped == 0) counts - day else counts + (day to clamped))
    }

    fun renamed(name: String): Habit = copy(name = name)

    /**
     * Changes the daily goal without rewriting history: days that were done
     * stay done, and days that weren't stay not done.
     */
    fun withGoal(newValue: Int): Habit {
        val newGoal = clampGoal(newValue)
        if (newGoal == goal) return this
        val newCounts = counts.mapValues { (_, count) ->
            if (count >= goal) newGoal else minOf(count, newGoal - 1)
        }.filterValues { it > 0 }
        return copy(goal = newGoal, counts = newCounts)
    }

    companion object {
        val GOAL_RANGE = 1..99

        fun clampGoal(goal: Int): Int = goal.coerceIn(GOAL_RANGE)

        fun of(
            name: String,
            createdOn: LocalDate,
            goal: Int = 1,
            counts: Map<LocalDate, Int> = emptyMap(),
            id: String = UUID.randomUUID().toString(),
        ): Habit {
            val clampedGoal = clampGoal(goal)
            return Habit(
                id = id.lowercase(),
                name = name,
                goal = clampedGoal,
                createdOn = createdOn,
                counts = counts.filterValues { it > 0 }.mapValues { (_, count) -> minOf(count, clampedGoal) },
            )
        }
    }
}

/** How much of the routine was done on one day. */
data class DayProgress(val done: Int, val total: Int) {
    enum class Tier { NothingToDo, None, Low, Medium, High, NearlyThere, Perfect }

    /** Whole percent, rounded down so that 100% only ever means everything. */
    val percent: Int get() = if (total == 0) 0 else done * 100 / total

    val tier: Tier
        get() = when {
            total == 0 -> Tier.NothingToDo
            done == total -> Tier.Perfect
            percent >= 76 -> Tier.NearlyThere
            percent >= 51 -> Tier.High
            percent >= 26 -> Tier.Medium
            percent >= 1 -> Tier.Low
            else -> Tier.None
        }
}

/** The user's whole routine: an ordered list of habits and the day tracking began. */
data class Routine(val startDate: LocalDate, val habits: List<Habit> = emptyList()) {

    /** Every day from the start of tracking up to and including [through]. */
    fun days(through: LocalDate): List<LocalDate> {
        val first = minOf(startDate, through)
        return (first.toEpochDay()..through.toEpochDay()).map(LocalDate::ofEpochDay)
    }

    fun progress(day: LocalDate): DayProgress {
        val tracked = habits.filter { it.isTracked(day) }
        return DayProgress(done = tracked.count { it.isDone(day) }, total = tracked.size)
    }

    fun withHabitAdded(name: String, goal: Int, today: LocalDate): Routine =
        copy(habits = habits + Habit.of(name = name, goal = goal, createdOn = today))

    fun tapped(id: String, day: LocalDate): Routine = updating(id) { it.tapped(day) }

    fun renamed(id: String, name: String): Routine = updating(id) { it.renamed(name) }

    fun withGoal(id: String, goal: Int): Routine = updating(id) { it.withGoal(goal) }

    fun removing(id: String): Routine = copy(habits = habits.filterNot { it.id == id })

    /** Moves the habit at [from] so it ends up at index [to]. */
    fun moving(from: Int, to: Int): Routine {
        if (from !in habits.indices || to !in habits.indices || from == to) return this
        val reordered = habits.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        return copy(habits = reordered)
    }

    private inline fun updating(id: String, change: (Habit) -> Habit): Routine =
        copy(habits = habits.map { if (it.id == id) change(it) else it })

    companion object {
        const val CURRENT_VERSION = 1
    }
}
