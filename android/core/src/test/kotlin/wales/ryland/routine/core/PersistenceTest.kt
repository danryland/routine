package wales.ryland.routine.core

import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private fun day(iso: String) = LocalDate.parse(iso)

/** The repository's shared fixtures (also used by the iOS tests). */
private fun fixture(name: String): String {
    val dir = System.getProperty("fixturesDir") ?: error("Run via Gradle, which sets -DfixturesDir")
    return File(dir, name).readText()
}

class RoutineJsonTest {
    @Test fun readsTheSharedFormat() {
        val routine = RoutineJson.decode(fixture("routine-v1.json"))
        assertEquals(day("2024-08-23"), routine.startDate)
        assertEquals(listOf("Teeth AM", "Water"), routine.habits.map { it.name })
        val water = routine.habits[1]
        assertEquals("1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b", water.id)
        assertEquals(6, water.goal)
        assertEquals(day("2024-08-25"), water.createdOn)
        assertEquals(2, water.count(day("2024-08-26")))
        assertTrue(water.isDone(day("2024-08-28")))
    }

    @Test fun writesTheSharedFormat() {
        val text = fixture("routine-v1.json")
        val reencoded = RoutineJson.encode(RoutineJson.decode(text))
        assertEquals(Json.parseToJsonElement(text), Json.parseToJsonElement(reencoded))
    }

    @Test fun acceptsUppercaseIdsFromApplePlatforms() {
        val json = """{"version":1,"startDate":"2024-08-23","habits":[{"id":"8A0F1C9E-2A4B-4C55-9F1E-3B2A1D0C9E88","name":"A","goal":1,"createdOn":"2024-08-23"}]}"""
        assertEquals("8a0f1c9e-2a4b-4c55-9f1e-3b2a1d0c9e88", RoutineJson.decode(json).habits[0].id)
    }

    @Test fun refusesNewerVersionsInsteadOfGuessing() {
        assertThrows(RoutineFormatException::class.java) {
            RoutineJson.decode("""{"version": 2, "startDate": "2024-08-23", "habits": []}""")
        }
        assertThrows(RoutineFormatException::class.java) { RoutineJson.decode("{ not json") }
        assertThrows(RoutineFormatException::class.java) {
            RoutineJson.decode("""{"version": 1, "startDate": "23 Aug", "habits": []}""")
        }
    }
}

class LegacyImportTest {
    @Test fun parsesTheOldDateFormat() {
        assertEquals(day("2024-09-03"), LegacyImport.parseDate("3 Sep 2024"))
        assertEquals(day("2024-02-29"), LegacyImport.parseDate("29 Feb 2024"))
        assertNull(LegacyImport.parseDate("31 Feb 2024"))
        assertNull(LegacyImport.parseDate("2024-09-03"))
    }

    @Test fun importsEverythingTheOldAppCouldSave() {
        val routine = LegacyImport.routine(fixture("legacy-capacitor.json"), today = day("2026-10-08"))!!
        assertEquals(day("2024-08-23"), routine.startDate)

        assertEquals(
            listOf("Teeth AM", "SPF", "Water x6", "Lowered target", "Typed target", "Cleared target", "Garbage", ""),
            routine.habits.map { it.name },
        )
        assertTrue(routine.habits.all { it.createdOn == day("2024-08-23") })
        val habits = routine.habits.associateBy { it.name }

        assertEquals(mapOf(day("2024-08-26") to 1, day("2024-08-27") to 1), habits.getValue("Teeth AM").counts)
        assertEquals(mapOf(day("2024-08-28") to 1), habits.getValue("SPF").counts)
        assertEquals(6, habits.getValue("Water x6").goal)
        assertEquals(
            mapOf(day("2024-08-26") to 1, day("2024-08-27") to 2, day("2024-08-28") to 6),
            habits.getValue("Water x6").counts,
        )
        // Doing more than a lowered target counts as done (the old app said it wasn't).
        assertEquals(3, habits.getValue("Lowered target").goal)
        assertTrue(habits.getValue("Lowered target").isDone(day("2024-08-27")))
        assertEquals(4, habits.getValue("Typed target").goal)
        assertEquals(2, habits.getValue("Typed target").count(day("2024-08-27")))
        assertEquals(1, habits.getValue("Cleared target").goal)
        assertEquals(mapOf(day("2024-08-27") to 1), habits.getValue("Cleared target").counts)
        assertTrue(habits.getValue("Garbage").counts.isEmpty())

        assertEquals(DayProgress(2, 8), routine.progress(day("2024-08-28")))
    }

    @Test fun startsTodayWhenTheOldAppHadNothing() {
        assertEquals(Routine(startDate = day("2026-10-08")), LegacyImport.routine("{}", today = day("2026-10-08")))
    }

    @Test fun ignoresThingsThatArentARoutine() {
        assertNull(LegacyImport.routine("", today = day("2026-10-08")))
        assertNull(LegacyImport.routine("[1, 2]", today = day("2026-10-08")))
    }
}

class RoutineStoreTest {
    private val directory: File = Files.createTempDirectory("routine-test").toFile()
    private val file = File(directory, "routine.json")

    @Test fun firstLaunchStartsEmptyToday() {
        assertEquals(Routine(startDate = day("2026-10-08")), RoutineStore(file).load(day("2026-10-08")))
    }

    @Test fun savesAndLoads() {
        val store = RoutineStore(file)
        var routine = Routine(startDate = day("2026-10-01")).withHabitAdded("Stretch", 2, day("2026-10-01"))
        routine = routine.tapped(routine.habits[0].id, day("2026-10-02"))
        store.save(routine)
        assertEquals(routine, store.load(day("2026-10-08")))
        assertEquals(listOf("routine.json"), directory.list()!!.toList())
    }

    @Test fun migratesLegacyDataOnceAndPrefersTheNewFile() {
        val legacy = """{"dates":["1 Oct 2026"],"tasks":[{"name":"Old","completed":["2 Oct 2026"]}]}"""
        val store = RoutineStore(file) { legacy }

        val migrated = store.load(day("2026-10-08"))
        assertEquals(listOf("Old"), migrated.habits.map { it.name })
        assertTrue(file.exists())

        store.save(migrated.renamed(migrated.habits[0].id, "Renamed"))
        assertEquals(listOf("Renamed"), store.load(day("2026-10-08")).habits.map { it.name })
    }

    @Test fun movesAnUnreadableFileAsideInsteadOfOverwritingIt() {
        file.writeText("{ not json")
        assertEquals(Routine(startDate = day("2026-10-08")), RoutineStore(file).load(day("2026-10-08")))
        assertTrue(directory.list()!!.any { it.startsWith("routine.unreadable-") })
    }
}
