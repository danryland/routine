package wales.ryland.routine.core

import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun day(iso: String) = LocalDate.parse(iso)

/** A tick-box habit done on the given days. */
private fun habit(done: List<String>, createdOn: String = "2024-08-01") =
    Habit.of(name = "H", createdOn = day(createdOn), counts = done.associate { day(it) to 1 })

class StreakTest {
    @Test fun currentStreakDoesntBreakUntilTheDayIsOver() {
        val h = habit(listOf("2024-08-05", "2024-08-06", "2024-08-07"))
        assertEquals(3, h.currentStreak(day("2024-08-07"))) // done today
        assertEquals(3, h.currentStreak(day("2024-08-08"))) // today still open
        assertEquals(0, h.currentStreak(day("2024-08-09"))) // missed yesterday
    }

    @Test fun bestStreakFindsTheLongestRun() {
        val h = habit(listOf("2024-08-01", "2024-08-02", "2024-08-04", "2024-08-05", "2024-08-06", "2024-08-09"))
        assertEquals(3, h.bestStreak(day("2024-08-31")))
        assertEquals(2, h.bestStreak(day("2024-08-04")))
    }

    @Test fun partlyDoneCountersDontCount() {
        val h = Habit.of(name = "Water", goal = 3, createdOn = day("2024-08-01"))
            .withCount(3, day("2024-08-01"))
            .withCount(2, day("2024-08-02"))
        assertEquals(1, h.bestStreak(day("2024-08-02")))
    }

    @Test fun completionRateCountsOnlyTrackedFinishedDays() {
        val h = habit(listOf("2024-08-08", "2024-08-09"), createdOn = "2024-08-07")
        assertEquals(2.0 / 3.0, h.completionRate(30, day("2024-08-10"))!!, 1e-9)
        assertEquals(3.0 / 4.0, h.tapped(day("2024-08-10")).completionRate(30, day("2024-08-10"))!!, 1e-9)
        assertEquals(1.0, h.completionRate(2, day("2024-08-10"))!!, 1e-9)
        assertNull(habit(emptyList(), createdOn = "2024-08-10").completionRate(30, day("2024-08-10")))
    }

    @Test fun perfectDays() {
        var routine = Routine(startDate = day("2024-08-01"))
            .withHabitAdded("A", 1, day("2024-08-01"))
            .withHabitAdded("B", 1, day("2024-08-01"))
        val (a, b) = routine.habits
        for (d in listOf("2024-08-01", "2024-08-02", "2024-08-04")) {
            routine = routine.tapped(a.id, day(d)).tapped(b.id, day(d))
        }
        routine = routine.tapped(a.id, day("2024-08-03"))
        assertEquals(3, routine.perfectDayCount(day("2024-08-05")))
        assertEquals(2, routine.bestPerfectStreak(day("2024-08-05")))
        assertEquals(1, routine.currentPerfectStreak(day("2024-08-05")))
        assertEquals(0, routine.currentPerfectStreak(day("2024-08-06")))
    }

    @Test fun perfectStreakStopsAtTheStartOfTracking() {
        var routine = Routine(startDate = day("2024-08-01")).withHabitAdded("A", 1, day("2024-08-01"))
        val a = routine.habits[0].id
        routine = routine.tapped(a, day("2024-08-01")).tapped(a, day("2024-08-02"))
        assertEquals(2, routine.currentPerfectStreak(day("2024-08-02")))
    }
}

class PolicyTest {
    @Test fun freeTierAllowsThreeHabits() {
        var routine = Routine(startDate = day("2024-08-01"))
        for (name in listOf("A", "B")) routine = routine.withHabitAdded(name, 1, day("2024-08-01"))
        assertTrue(routine.canAddHabit(isUnlocked = false))
        routine = routine.withHabitAdded("C", 1, day("2024-08-01"))
        assertFalse(routine.canAddHabit(isUnlocked = false))
        assertTrue(routine.canAddHabit(isUnlocked = true))
    }

    @Test fun reviewIsRequestedRarelyAndOnlyAfterAFewPerfectDays() {
        assertFalse(ReviewPolicy.shouldAsk(2, null, day("2024-08-10")))
        assertTrue(ReviewPolicy.shouldAsk(3, null, day("2024-08-10")))
        assertFalse(ReviewPolicy.shouldAsk(30, day("2024-06-01"), day("2024-08-10")))
        assertTrue(ReviewPolicy.shouldAsk(30, day("2024-04-01"), day("2024-08-10")))
    }
}

class ExportTest {
    @Test fun csvHasARowPerTrackedHabitPerDay() {
        var routine = Routine(startDate = day("2024-08-01"))
            .withHabitAdded("Teeth", 1, day("2024-08-01"))
            .withHabitAdded("Water, glasses", 3, day("2024-08-02"))
        val (teeth, water) = routine.habits
        routine = routine.tapped(teeth.id, day("2024-08-01")).tapped(water.id, day("2024-08-02"))

        assertEquals(
            listOf(
                "date,habit,count,goal,done",
                "2024-08-01,Teeth,1,1,yes",
                "2024-08-02,Teeth,0,1,no",
                "2024-08-02,\"Water, glasses\",1,3,no",
                "",
            ).joinToString("\r\n"),
            RoutineCsv.export(routine, day("2024-08-02")),
        )
    }

    @Test fun csvEscaping() {
        assertEquals("plain", RoutineCsv.escape("plain"))
        assertEquals("\"say \"\"hi\"\"\"", RoutineCsv.escape("say \"hi\""))
        assertEquals("'=SUM(A1)", RoutineCsv.escape("=SUM(A1)"))
        assertEquals("\"line\nbreak\"", RoutineCsv.escape("line\nbreak"))
    }
}

class RestoreTest {
    @Test fun restoresEitherFormatButNotJunk() {
        val saved = RoutineJson.encode(Routine(startDate = day("2024-08-01")))
        assertEquals(Routine(startDate = day("2024-08-01")), restoreRoutine(saved, day("2026-10-18")))

        val legacy = """{"dates":["1 Aug 2024"],"tasks":[{"name":"Old","completed":[]}]}"""
        assertEquals(listOf("Old"), restoreRoutine(legacy, day("2026-10-18"))!!.habits.map { it.name })

        assertNull(restoreRoutine("""{"something":"else"}""", day("2026-10-18")))
        assertNull(restoreRoutine("not json", day("2026-10-18")))
    }

    @Test fun storeReportsWhereTheRoutineCameFrom() {
        val directory: File = Files.createTempDirectory("feature-test").toFile()
        val store = RoutineStore(File(directory, "routine.json")) { """{"dates":[],"tasks":[]}""" }
        assertEquals(RoutineStore.Origin.ImportedFromLegacyApp, store.loadReportingOrigin(day("2026-10-18")).origin)
        assertEquals(RoutineStore.Origin.Saved, store.loadReportingOrigin(day("2026-10-18")).origin)
        assertEquals(RoutineStore.Origin.New, RoutineStore(File(directory, "other.json")).loadReportingOrigin(day("2026-10-18")).origin)
    }
}
