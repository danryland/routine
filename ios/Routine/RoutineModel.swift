import Foundation
import Observation
import WidgetKit
import os

/// Owns the routine for the UI, and saves every change straight away. Also
/// keeps the widget, iCloud and reminders in step with it.
@MainActor
@Observable
final class RoutineModel {
    private(set) var routine: Routine
    private(set) var today: Day

    /// True when this launch imported the original app's data.
    let importedFromLegacyApp: Bool

    /// Set when a rating request is due; the UI shows it and calls `didRequestReview()`.
    private(set) var isReviewRequestDue = false

    /// Mirrors the purchase state; Pro features check this.
    var isUnlocked = false {
        didSet {
            guard isUnlocked != oldValue else { return }
            applySettings()
        }
    }

    var remindersOn: Bool {
        didSet {
            defaults.set(remindersOn, forKey: SettingsKey.remindersOn)
            applySettings()
        }
    }

    var reminderMinute: Int {
        didSet {
            defaults.set(reminderMinute, forKey: SettingsKey.reminderMinute)
            applySettings()
        }
    }

    var iCloudSyncOn: Bool {
        didSet {
            defaults.set(iCloudSyncOn, forKey: SettingsKey.iCloudSyncOn)
            applySettings()
        }
    }

    @ObservationIgnored private let store: RoutineStore?
    @ObservationIgnored private let defaults: UserDefaults
    @ObservationIgnored private let cloud: CloudSync?
    @ObservationIgnored private let calendar: Calendar
    @ObservationIgnored private var lastKnownFileDate: Date?
    @ObservationIgnored private let log = Logger(subsystem: "wales.ryland.routine", category: "model")

    init(store: RoutineStore?, defaults: UserDefaults, cloud: CloudSync?, calendar: Calendar = .current) {
        let today = Day.today(calendar: calendar)
        let loaded = store?.loadReportingOrigin(today: today) ?? (routine: Routine(startDate: today), origin: RoutineStore.Origin.new)
        self.store = store
        self.defaults = defaults
        self.cloud = cloud
        self.calendar = calendar
        self.today = today
        self.routine = loaded.routine
        self.importedFromLegacyApp = loaded.origin == .importedFromLegacyApp
        self.lastKnownFileDate = store?.lastModified
        self.remindersOn = defaults.bool(forKey: SettingsKey.remindersOn)
        self.reminderMinute = defaults.object(forKey: SettingsKey.reminderMinute) as? Int ?? 20 * 60
        self.iCloudSyncOn = defaults.bool(forKey: SettingsKey.iCloudSyncOn)
        cloud?.onRemoteChange = { [weak self] in self?.syncWithCloud() }
    }

    /// An in-memory model, for previews and UI tests.
    convenience init(routine: Routine, today: Day) {
        self.init(store: nil, defaults: UserDefaults(suiteName: "preview") ?? .standard, cloud: nil)
        self.routine = routine
        self.today = today
    }

    static func live() -> RoutineModel {
        RoutineModel(store: AppGroup.store(), defaults: AppGroup.defaults, cloud: CloudSync())
    }

    var days: [Day] {
        routine.days(through: today)
    }

    var canAddHabit: Bool {
        routine.canAddHabit(isUnlocked: isUnlocked)
    }

    // MARK: - Lifecycle

    /// Call when the app comes to the foreground: picks up a new day, ticks made
    /// in the widget, and changes from other devices.
    func becameActive() {
        let now = Day.today(calendar: calendar)
        if now != today {
            today = now
        }
        if let store, let modified = store.lastModified, modified != lastKnownFileDate {
            routine = store.load(today: today)
            lastKnownFileDate = modified
        }
        syncWithCloud()
        rescheduleReminders()
    }

    // MARK: - Edits

    func tap(_ id: Habit.ID, on day: Day) {
        let wasPerfect = routine.isPerfect(on: day)
        update { $0.tap(id, on: day) }
        if !wasPerfect, routine.isPerfect(on: day), day == today {
            dayBecamePerfect()
        }
    }

    func addHabit(named name: String, goal: Int) {
        guard canAddHabit else { return }
        update { _ = $0.addHabit(named: name, goal: goal, on: today) }
    }

    func rename(_ id: Habit.ID, to name: String) {
        update { $0.rename(id, to: name) }
    }

    func setGoal(_ id: Habit.ID, to goal: Int) {
        update { $0.setGoal(id, to: goal) }
    }

    func removeHabits(at offsets: IndexSet) {
        update { $0.removeHabits(at: offsets) }
    }

    func moveHabits(from source: IndexSet, to destination: Int) {
        update { $0.moveHabits(from: source, to: destination) }
    }

    /// Replaces everything with a backup. Returns false if the file isn't one.
    func restore(from data: Data) -> Bool {
        guard let restored = Routine.restoring(from: data, today: today) else { return false }
        update { $0 = restored }
        return true
    }

    func backupData() throws -> Data {
        try routine.encoded()
    }

    func csvExport() -> String {
        RoutineCSV.export(routine, through: today)
    }

    func didRequestReview() {
        isReviewRequestDue = false
        defaults.set(today.iso, forKey: SettingsKey.lastReviewRequest)
    }

    private func update(_ change: (inout Routine) -> Void) {
        var edited = routine
        change(&edited)
        guard edited != routine else { return }
        let todayWasPerfect = routine.isPerfect(on: today)
        routine = edited
        save()
        WidgetCenter.shared.reloadAllTimelines()
        if let data = try? routine.encoded(), iCloudSyncOn, isUnlocked {
            cloud?.push(data, modifiedAt: lastKnownFileDate ?? Date())
        }
        if routine.isPerfect(on: today) != todayWasPerfect {
            rescheduleReminders()
        }
    }

    private func save(modifiedAt date: Date? = nil) {
        guard let store else { return }
        do {
            try store.save(routine)
            if let date {
                try? FileManager.default.setAttributes([.modificationDate: date], ofItemAtPath: store.fileURL.path)
            }
            lastKnownFileDate = store.lastModified
        } catch {
            log.error("Couldn't save routine: \(error.localizedDescription, privacy: .public)")
        }
    }

    // MARK: - Pro features

    private func applySettings() {
        rescheduleReminders()
        syncWithCloud()
    }

    private func rescheduleReminders() {
        let enabled = remindersOn && isUnlocked
        let minute = reminderMinute
        let routine = routine
        let today = today
        Task {
            await Reminders.reschedule(enabled: enabled, minuteOfDay: minute, routine: routine, today: today)
        }
    }

    private func syncWithCloud() {
        guard iCloudSyncOn, isUnlocked, let cloud else { return }
        let remote = cloud.remote
        let remoteRoutine = remote.flatMap { try? Routine.decode(from: $0.data) }
        let localModified = lastKnownFileDate ?? .distantPast
        switch SyncDecision.decide(local: routine, localModified: localModified, remote: remoteRoutine, remoteModified: remote?.modifiedAt) {
        case .inSync:
            break
        case .useRemote:
            guard let remoteRoutine, let remote else { return }
            routine = remoteRoutine
            save(modifiedAt: remote.modifiedAt)
            WidgetCenter.shared.reloadAllTimelines()
            rescheduleReminders()
        case .pushLocal:
            if let data = try? routine.encoded() {
                cloud.push(data, modifiedAt: localModified)
            }
        }
    }

    private func dayBecamePerfect() {
        let lastAsked = defaults.string(forKey: SettingsKey.lastReviewRequest).flatMap(Day.init(iso:))
        if ReviewPolicy.shouldAsk(perfectDays: routine.perfectDayCount(through: today), lastAsked: lastAsked, today: today) {
            isReviewRequestDue = true
        }
    }
}

extension RoutineModel {
    /// Sample data for previews, screenshots and UI tests.
    static func sampleRoutine(today: Day = .today()) -> Routine {
        let start = today.adding(days: -9)
        var routine = Routine(startDate: start)
        let samples: [(String, Int)] = [("Teeth AM", 1), ("Face AM", 1), ("SPF", 1), ("Shower", 1), ("Daily greens", 1), ("Water", 6)]
        for (index, (name, goal)) in samples.enumerated() {
            let habit = routine.addHabit(named: name, goal: goal, on: start)
            for offset in 0...9 where (offset + index) % 3 != 0 || offset > 6 {
                for _ in 0..<(goal == 1 ? 1 : (offset * 2) % (goal + 1)) {
                    routine.tap(habit.id, on: start.adding(days: offset))
                }
            }
        }
        return routine
    }

    static var preview: RoutineModel {
        let model = RoutineModel(routine: sampleRoutine(), today: .today())
        model.isUnlocked = true
        return model
    }
}
