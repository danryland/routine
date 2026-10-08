package wales.ryland.routine.core

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

/**
 * Saves the routine as a JSON file and, on first launch after updating from
 * the Capacitor app, imports what that app left behind.
 *
 * @param legacyJson reads the old app's saved JSON, if any.
 */
class RoutineStore(
    private val file: File,
    private val legacyJson: () -> String? = { null },
) {
    /**
     * Loads the saved routine, migrating legacy data if there's no file yet.
     *
     * A file that can't be read is moved aside rather than overwritten, so a
     * bug or a newer app version's format never silently destroys history.
     */
    fun load(today: LocalDate): Routine {
        if (file.exists()) {
            try {
                return RoutineJson.decode(file.readText())
            } catch (e: RoutineFormatException) {
                quarantineUnreadableFile()
            } catch (e: IOException) {
                quarantineUnreadableFile()
            }
        }
        val migrated = legacyJson()?.let { LegacyImport.routine(it, today) }
        if (migrated != null) {
            // The legacy data is left in place as a fallback; it's never written again.
            runCatching { save(migrated) }
            return migrated
        }
        return Routine(startDate = today)
    }

    /** Writes atomically: a crash mid-save leaves the previous file intact. */
    @Throws(IOException::class)
    fun save(routine: Routine) {
        val directory = file.absoluteFile.parentFile
        directory.mkdirs()
        val temp = File(directory, "${file.name}.tmp")
        temp.writeText(RoutineJson.encode(routine))
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun quarantineUnreadableFile() {
        val backup = File(file.absoluteFile.parentFile, "${file.nameWithoutExtension}.unreadable-${System.currentTimeMillis() / 1000}.json")
        file.renameTo(backup)
    }
}
