package wales.ryland.routine.core

import java.time.LocalDate

/** What's free and what needs the one-time unlock. */
object FreeTier {
    /** Free users can track this many habits; the unlock removes the limit. */
    const val HABIT_LIMIT = 3
}

fun Routine.canAddHabit(isUnlocked: Boolean): Boolean = isUnlocked || habits.size < FreeTier.HABIT_LIMIT

/**
 * When to ask for a store rating: only on a day when everything got done, once
 * the user has had a few of them, and rarely.
 */
object ReviewPolicy {
    const val MINIMUM_PERFECT_DAYS = 3
    const val DAYS_BETWEEN_REQUESTS = 120L

    fun shouldAsk(perfectDays: Int, lastAsked: LocalDate?, today: LocalDate): Boolean {
        if (perfectDays < MINIMUM_PERFECT_DAYS) return false
        return lastAsked == null || today.toEpochDay() - lastAsked.toEpochDay() >= DAYS_BETWEEN_REQUESTS
    }
}

/** Exports every tracked day as CSV, one row per habit per day, for spreadsheets. */
object RoutineCsv {
    const val HEADER = "date,habit,count,goal,done"

    fun export(routine: Routine, through: LocalDate): String = buildString {
        append(HEADER).append("\r\n")
        for (day in routine.days(through)) {
            for (habit in routine.habits.filter { it.isTracked(day) }) {
                append(listOf(day.toString(), escape(habit.name), habit.count(day), habit.goal, if (habit.isDone(day)) "yes" else "no").joinToString(","))
                append("\r\n")
            }
        }
    }

    /** RFC 4180 quoting, plus a leading apostrophe on anything a spreadsheet would run as a formula. */
    fun escape(field: String): String {
        val value = if (field.isNotEmpty() && field[0] in "=+-@\t\r") "'$field" else field
        return if (value.any { it in ",\"\r\n" }) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }
}

/** Reads a backup file: this app's own format, or a copy of the original app's data. */
fun restoreRoutine(text: String, today: LocalDate): Routine? {
    try {
        return RoutineJson.decode(text)
    } catch (e: RoutineFormatException) {
        // Fall through to the legacy format.
    }
    // Any JSON object "imports" as an empty legacy routine, so insist on habits.
    return LegacyImport.routine(text, today)?.takeIf { it.habits.isNotEmpty() }
}
