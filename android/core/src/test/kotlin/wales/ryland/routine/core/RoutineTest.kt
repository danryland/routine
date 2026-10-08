package wales.ryland.routine.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun day(iso: String) = LocalDate.parse(iso)

class HabitTest {
    @Test fun tickBoxToggles() {
        var habit = Habit.of(name = "SPF", createdOn = day("2024-08-01"))
        habit = habit.tapped(day("2024-08-02"))
        assertTrue(habit.isDone(day("2024-08-02")))
        habit = habit.tapped(day("2024-08-02"))
        assertFalse(habit.isDone(day("2024-08-02")))
        assertTrue(habit.counts.isEmpty())
    }

    @Test fun counterFillsThenWrapsToZero() {
        var habit = Habit.of(name = "Water", goal = 3, createdOn = day("2024-08-01"))
        val today = day("2024-08-02")
        val seen = (1..5).map { habit = habit.tapped(today); habit.count(today) }
        assertEquals(listOf(1, 2, 3, 0, 1), seen)
    }

    @Test fun changingGoalKeepsWhetherPastDaysWereDone() {
        var habit = Habit.of(
            name = "Water", goal = 6, createdOn = day("2024-08-01"),
            counts = mapOf(day("2024-08-01") to 6, day("2024-08-02") to 5, day("2024-08-03") to 2),
        )
        habit = habit.withGoal(4)
        assertTrue(habit.isDone(day("2024-08-01")))
        assertEquals(3, habit.count(day("2024-08-02"))) // was 5/6: still not done
        assertEquals(2, habit.count(day("2024-08-03")))

        habit = habit.withGoal(8)
        assertTrue(habit.isDone(day("2024-08-01")))
        assertFalse(habit.isDone(day("2024-08-02")))

        habit = habit.withGoal(1)
        assertTrue(habit.isDone(day("2024-08-01")))
        assertFalse(habit.isDone(day("2024-08-02")))
        assertEquals(1, habit.counts.size) // a tick box has no "partly done", so partial days clear
    }

    @Test fun goalIsClamped() {
        assertEquals(1, Habit.of(name = "", goal = 0, createdOn = day("2024-08-01")).goal)
        assertEquals(99, Habit.of(name = "", goal = 500, createdOn = day("2024-08-01")).goal)
    }

    @Test fun backfillingExtendsTrackingEarlier() {
        var habit = Habit.of(name = "Read", createdOn = day("2024-08-10"))
        assertFalse(habit.isTracked(day("2024-08-09")))
        habit = habit.tapped(day("2024-08-05"))
        assertEquals(day("2024-08-05"), habit.firstTrackedDay)
        assertTrue(habit.isTracked(day("2024-08-09")))
    }
}

class ProgressTest {
    @Test fun tiers() {
        val cases = listOf(
            Triple(0, 0, DayProgress.Tier.NothingToDo),
            Triple(0, 4, DayProgress.Tier.None),
            Triple(1, 4, DayProgress.Tier.Low),
            Triple(2, 4, DayProgress.Tier.Medium),
            Triple(3, 4, DayProgress.Tier.High),
            Triple(4, 5, DayProgress.Tier.NearlyThere),
            Triple(5, 5, DayProgress.Tier.Perfect),
        )
        for ((done, total, tier) in cases) {
            assertEquals("$done/$total", tier, DayProgress(done, total).tier)
        }
    }

    @Test fun percentRoundsDownSoOnlyEverythingIsAHundred() {
        assertEquals(99, DayProgress(199, 200).percent)
        assertEquals(DayProgress.Tier.NearlyThere, DayProgress(199, 200).tier)
        assertEquals(33, DayProgress(1, 3).percent)
    }
}

class RoutineTest {
    @Test fun daysRunFromStartThroughToday() {
        val routine = Routine(startDate = day("2024-02-27"))
        assertEquals(
            listOf("2024-02-27", "2024-02-28", "2024-02-29", "2024-03-01"),
            routine.days(through = day("2024-03-01")).map(LocalDate::toString),
        )
        // If the clock goes backwards, still show today rather than nothing.
        assertEquals(day("2024-02-20"), routine.days(through = day("2024-02-20")).last())
    }

    @Test fun newHabitsDontLowerPastScores() {
        var routine = Routine(startDate = day("2024-08-01")).withHabitAdded("Teeth", 1, day("2024-08-01"))
        routine = routine.tapped(routine.habits[0].id, day("2024-08-01"))
        routine = routine.withHabitAdded("Floss", 1, day("2024-08-02"))

        assertEquals(DayProgress.Tier.Perfect, routine.progress(day("2024-08-01")).tier)
        assertEquals(DayProgress(0, 2), routine.progress(day("2024-08-02")))
    }

    @Test fun moveTakesTheFinalIndex() {
        var routine = Routine(startDate = day("2024-08-01"))
        for (name in listOf("A", "B", "C", "D")) routine = routine.withHabitAdded(name, 1, day("2024-08-01"))

        routine = routine.moving(from = 0, to = 2)
        assertEquals(listOf("B", "C", "A", "D"), routine.habits.map { it.name })
        routine = routine.moving(from = 3, to = 0)
        assertEquals(listOf("D", "B", "C", "A"), routine.habits.map { it.name })
        assertEquals(routine, routine.moving(from = 0, to = 9))
    }

    @Test fun editsTargetTheRightHabit() {
        var routine = Routine(startDate = day("2024-08-01"))
            .withHabitAdded("A", 1, day("2024-08-01"))
            .withHabitAdded("B", 1, day("2024-08-01"))
        val (a, b) = routine.habits
        routine = routine.renamed(b.id, "Bee").withGoal(a.id, 4).removing(a.id)
        assertEquals(listOf("Bee"), routine.habits.map { it.name })
        assertEquals(1, routine.habits[0].goal)
    }
}
