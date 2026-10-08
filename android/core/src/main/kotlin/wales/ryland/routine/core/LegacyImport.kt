package wales.ryland.routine.core

import java.time.DateTimeException
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * Reads the routine saved by the original Vue/Capacitor app so people who
 * update keep their history.
 *
 * That app stored one JSON string via `@capacitor/preferences`, which on Android
 * lands in the `CapacitorStorage` SharedPreferences under the key `routine`:
 *
 * ```
 * { "dates": ["23 Aug 2024", …],
 *   "tasks": [ { "name": "SPF", "completed": ["28 Aug 2024"] },
 *              { "name": "Water", "target": 6,
 *                "completed": [ { "date": "26 Aug 2024", "times": 1 } ] } ] }
 * ```
 */
object LegacyImport {
    const val PREFERENCES_NAME = "CapacitorStorage"
    const val PREFERENCES_KEY = "routine"

    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** Returns null if [json] isn't a routine the old app could have written. */
    fun routine(json: String, today: LocalDate): Routine? {
        val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null

        val dates = (root["dates"] as? JsonArray).orEmpty().mapNotNull { it.string?.let(::parseDate) }
        val tasks = (root["tasks"] as? JsonArray).orEmpty().filterIsInstance<JsonObject>()

        val parsed = tasks.map { task ->
            val goal = task["target"]?.goal ?: 1
            val counts = mutableMapOf<LocalDate, Int>()
            for (entry in (task["completed"] as? JsonArray).orEmpty()) {
                val (day, times) = completion(entry, goal) ?: continue
                if (times > 0) counts[day] = maxOf(counts[day] ?: 0, times)
            }
            Triple(task["name"]?.string ?: "", goal, counts)
        }

        val earliestCompletion = parsed.flatMap { it.third.keys }.minOrNull()
        val startDate = listOfNotNull(dates.minOrNull(), earliestCompletion).minOrNull() ?: today

        return Routine(
            startDate = startDate,
            habits = parsed.map { (name, goal, counts) ->
                Habit.of(name = name, goal = goal, createdOn = startDate, counts = counts)
            },
        )
    }

    /** Parses the old app's "D MMM YYYY" dates, e.g. "3 Sep 2024". */
    fun parseDate(text: String): LocalDate? {
        val parts = text.split(" ").filter { it.isNotEmpty() }
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = months.indexOfFirst { it.equals(parts[1], ignoreCase = true) }.takeIf { it >= 0 } ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        return try {
            LocalDate.of(year, month + 1, day)
        } catch (e: DateTimeException) {
            null
        }
    }

    private fun completion(entry: JsonElement, goal: Int): Pair<LocalDate, Int>? = when (entry) {
        // Tick-box tasks stored bare dates.
        is JsonPrimitive -> entry.string?.let(::parseDate)?.let { it to goal }
        // Counter tasks stored { date, times }. If the target was later cleared,
        // the old app showed it as a tick box; treat any taps as done.
        is JsonObject -> {
            val day = entry["date"]?.string?.let(::parseDate)
            val times = entry["times"]?.int
            if (day == null || times == null) null else day to if (goal == 1) minOf(times, 1) else times
        }
        else -> null
    }

    private val JsonElement.string: String?
        get() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

    private val JsonElement.int: Int?
        get() {
            val primitive = this as? JsonPrimitive ?: return null
            val value = if (primitive.isString) primitive.content.trim().toDoubleOrNull() else primitive.doubleOrNull
            return value?.takeIf { it.isFinite() }?.roundToInt()
        }

    /** The old app treated a missing, empty, zero or one `target` as a tick box. */
    private val JsonElement.goal: Int?
        get() = int?.takeIf { it >= 2 }?.let(Habit::clampGoal)
}
