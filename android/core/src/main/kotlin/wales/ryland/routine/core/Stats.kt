package wales.ryland.routine.core

import java.time.LocalDate

// Streaks and completion rates. "Today" is treated as still in progress: a
// habit not yet done today doesn't break a streak or drag down a rate until
// the day is over.

/** Consecutive done days up to today, or up to yesterday if today isn't done yet. */
fun Habit.currentStreak(today: LocalDate): Int {
    var day = if (isDone(today)) today else today.minusDays(1)
    var streak = 0
    while (isDone(day)) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

fun Habit.bestStreak(through: LocalDate): Int {
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (day in counts.keys.filter { it <= through && isDone(it) }.sorted()) {
        run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    return best
}

/** The share of tracked days in the last [days] days that were done, or null if none count yet. */
fun Habit.completionRate(days: Int, today: LocalDate): Double? {
    val end = if (isDone(today)) today else today.minusDays(1)
    val start = maxOf(today.minusDays(days - 1L), firstTrackedDay)
    if (start > end) return null
    val total = end.toEpochDay() - start.toEpochDay() + 1
    val done = (start.toEpochDay()..end.toEpochDay()).count { isDone(LocalDate.ofEpochDay(it)) }
    return done.toDouble() / total
}

fun Routine.isPerfect(day: LocalDate): Boolean = progress(day).tier == DayProgress.Tier.Perfect

fun Routine.perfectDayCount(through: LocalDate): Int = days(through).count { isPerfect(it) }

/** Consecutive perfect days up to today, or up to yesterday if today isn't perfect yet. */
fun Routine.currentPerfectStreak(today: LocalDate): Int {
    val first = minOf(startDate, today)
    var day = if (isPerfect(today)) today else today.minusDays(1)
    var streak = 0
    while (day >= first && isPerfect(day)) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

fun Routine.bestPerfectStreak(through: LocalDate): Int {
    var best = 0
    var run = 0
    for (day in days(through)) {
        run = if (isPerfect(day)) run + 1 else 0
        best = maxOf(best, run)
    }
    return best
}
