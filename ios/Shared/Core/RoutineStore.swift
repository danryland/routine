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

    enum Origin: Equatable {
        /// Read from this app's own file.
        case saved
        /// Imported from the original Capacitor app on this first launch.
        case importedFromLegacyApp
        /// Nothing saved anywhere: a brand-new routine.
        case new
    }

    func load(today: Day) -> Routine {
        loadReportingOrigin(today: today).routine
    }

    /// Loads the saved routine, migrating legacy data if there's no file yet.
    ///
    /// A file that can't be read is moved aside rather than overwritten, so a
    /// bug or a newer app version's format never silently destroys history.
    func loadReportingOrigin(today: Day) -> (routine: Routine, origin: Origin) {
        if let data = try? Data(contentsOf: fileURL) {
            do {
                return (try Routine.decode(from: data), .saved)
            } catch {
                quarantineUnreadableFile()
            }
        }
        if let json = legacyDefaults?.string(forKey: LegacyImport.userDefaultsKey),
           let migrated = LegacyImport.routine(fromJSON: json, today: today) {
            // The legacy key is left in place as a fallback; it's never written again.
            try? save(migrated)
            return (migrated, .importedFromLegacyApp)
        }
        return (Routine(startDate: today), .new)
    }

    /// When the file was last written, by the app or the widget.
    var lastModified: Date? {
        (try? FileManager.default.attributesOfItem(atPath: fileURL.path))?[.modificationDate] as? Date
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
