import Foundation

/// Saves the routine as a JSON file and, on first launch after updating from
/// the Capacitor app, imports what that app left in `UserDefaults`.
struct RoutineStore {
    let fileURL: URL
    var legacyDefaults: UserDefaults?

    static func applicationSupport() -> RoutineStore {
        let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return RoutineStore(
            fileURL: directory.appendingPathComponent("routine.json"),
            legacyDefaults: .standard
        )
    }

    /// Loads the saved routine, migrating legacy data if there's no file yet.
    ///
    /// A file that can't be read is moved aside rather than overwritten, so a
    /// bug or a newer app version's format never silently destroys history.
    func load(today: Day) -> Routine {
        if let data = try? Data(contentsOf: fileURL) {
            do {
                return try Routine.decode(from: data)
            } catch {
                quarantineUnreadableFile()
            }
        }
        if let json = legacyDefaults?.string(forKey: LegacyImport.userDefaultsKey),
           let migrated = LegacyImport.routine(fromJSON: json, today: today) {
            // The legacy key is left in place as a fallback; it's never written again.
            try? save(migrated)
            return migrated
        }
        return Routine(startDate: today)
    }

    func save(_ routine: Routine) throws {
        try FileManager.default.createDirectory(
            at: fileURL.deletingLastPathComponent(),
            withIntermediateDirectories: true
        )
        try routine.encoded().write(to: fileURL, options: .atomic)
    }

    private func quarantineUnreadableFile() {
        let stamp = Int(Date().timeIntervalSince1970)
        let backup = fileURL.deletingPathExtension().appendingPathExtension("unreadable-\(stamp).json")
        try? FileManager.default.moveItem(at: fileURL, to: backup)
    }
}
