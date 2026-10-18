import Foundation

/// Storage shared by the app and its widget through an App Group.
enum AppGroup {
    static let identifier = "group.wales.ryland.routine"

    /// Settings both the app and the widget read. Falls back to the app's own
    /// defaults if the App Group isn't available (e.g. an unsigned build).
    static let defaults: UserDefaults = UserDefaults(suiteName: identifier) ?? .standard

    private static var containerURL: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: identifier)
    }

    private static var applicationSupportURL: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
    }

    /// The routine store. Lives in the App Group so the widget can read and tick
    /// habits; earlier builds kept it in Application Support, so it's moved over once.
    static func store() -> RoutineStore {
        guard let container = containerURL else {
            return RoutineStore(fileURL: applicationSupportURL.appendingPathComponent("routine.json"), legacyDefaults: .standard)
        }
        let shared = container.appendingPathComponent("routine.json")
        let old = applicationSupportURL.appendingPathComponent("routine.json")
        let files = FileManager.default
        if !files.fileExists(atPath: shared.path), files.fileExists(atPath: old.path) {
            try? files.moveItem(at: old, to: shared)
        }
        return RoutineStore(fileURL: shared, legacyDefaults: .standard)
    }
}

/// Settings keys, shared so the widget sees the same theme and unlock state.
enum SettingsKey {
    static let theme = "theme"
    /// The last known unlock state (purchase or early-supporter), for the widget
    /// and for launching offline.
    static let isUnlocked = "isUnlocked"
    /// Set when the routine was imported from the original app: early supporters
    /// get everything free.
    static let earlySupporter = "earlySupporter"
    static let remindersOn = "remindersOn"
    static let reminderMinute = "reminderMinute"
    static let iCloudSyncOn = "iCloudSyncOn"
    static let lastReviewRequest = "lastReviewRequest"
}
