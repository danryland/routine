package wales.ryland.routine.core

import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RoutineFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The on-disk format, shared with the iOS app (see `fixtures/routine-v1.json`):
 *
 * ```
 * { "version": 1, "startDate": "2024-08-23",
 *   "habits": [ { "id": "…", "name": "Water", "goal": 6, "createdOn": "2024-08-23",
 *                 "counts": { "2024-08-26": 2 } } ] }
 * ```
 */
object RoutineJson {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Serializable
    private data class RoutineDto(val version: Int, val startDate: String, val habits: List<HabitDto>)

    @Serializable
    private data class HabitDto(
        val id: String,
        val name: String,
        val goal: Int,
        val createdOn: String,
        val counts: Map<String, Int> = emptyMap(),
    )

    fun encode(routine: Routine): String = json.encodeToString(
        RoutineDto.serializer(),
        RoutineDto(
            version = Routine.CURRENT_VERSION,
            startDate = routine.startDate.toString(),
            habits = routine.habits.map { habit ->
                HabitDto(
                    id = habit.id,
                    name = habit.name,
                    goal = habit.goal,
                    createdOn = habit.createdOn.toString(),
                    counts = habit.counts.toSortedMap().mapKeys { (day, _) -> day.toString() },
                )
            },
        ),
    )

    /** @throws RoutineFormatException if [text] isn't a routine this version can read. */
    fun decode(text: String): Routine {
        val dto = try {
            json.decodeFromString(RoutineDto.serializer(), text)
        } catch (e: SerializationException) {
            throw RoutineFormatException("Not a saved routine", e)
        } catch (e: IllegalArgumentException) {
            throw RoutineFormatException("Not a saved routine", e)
        }
        if (dto.version > Routine.CURRENT_VERSION) {
            throw RoutineFormatException("Saved by a newer version (format ${dto.version})")
        }
        return Routine(
            startDate = parseDay(dto.startDate),
            habits = dto.habits.map { habit ->
                Habit.of(
                    id = habit.id,
                    name = habit.name,
                    goal = habit.goal,
                    createdOn = parseDay(habit.createdOn),
                    counts = habit.counts.mapKeys { (day, _) -> parseDay(day) },
                )
            },
        )
    }

    private fun parseDay(text: String): LocalDate = try {
        LocalDate.parse(text)
    } catch (e: DateTimeParseException) {
        throw RoutineFormatException("Invalid day \"$text\"", e)
    }
}
