import AppIntents
import Foundation

/// Ticks a habit (or adds one to a counter) for today, straight from the widget.
struct ToggleHabitIntent: AppIntent {
    static var title: LocalizedStringResource = "Tick off a habit"
    static var isDiscoverable: Bool = false

    @Parameter(title: "Habit")
    var habitID: String

    init() {}

    init(habitID: String) {
        self.habitID = habitID
    }

    func perform() async throws -> some IntentResult {
        guard AppGroup.defaults.bool(forKey: SettingsKey.isUnlocked), let id = UUID(uuidString: habitID) else {
            return .result()
        }
        let store = AppGroup.store()
        let today = Day.today()
        var routine = store.load(today: today)
        routine.tap(id, on: today)
        try store.save(routine)
        return .result()
    }
}
