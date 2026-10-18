import SwiftUI

@main
struct RoutineApp: App {
    @State private var model: RoutineModel
    @State private var purchases: PurchaseStore
    @AppStorage(SettingsKey.theme, store: AppGroup.defaults) private var themeID = Palette.rose.id

    init() {
        #if DEBUG
        if let testing = UITestLaunch.fromArguments() {
            _model = State(initialValue: testing.model)
            _purchases = State(initialValue: testing.purchases)
            return
        }
        #endif
        let model = RoutineModel.live()
        let purchases = PurchaseStore()
        if model.importedFromLegacyApp {
            purchases.markEarlySupporter()
        }
        _model = State(initialValue: model)
        _purchases = State(initialValue: purchases)
    }

    var body: some Scene {
        WindowGroup {
            let palette = Palette.named(themeID, isUnlocked: purchases.isUnlocked)
            ContentView()
                .environment(model)
                .environment(purchases)
                .environment(\.palette, palette)
                .preferredColorScheme(.dark)
                .tint(palette.accent)
                .task { purchases.start() }
                .onChange(of: purchases.isUnlocked, initial: true) { _, unlocked in
                    model.isUnlocked = unlocked
                }
        }
    }
}

#if DEBUG
/// Launch arguments for UI tests and screenshots: `-uitest` runs on an
/// in-memory routine, `-sample` fills it with sample data, `-unlocked` turns
/// on everything.
struct UITestLaunch {
    let model: RoutineModel
    let purchases: PurchaseStore

    @MainActor
    static func fromArguments() -> UITestLaunch? {
        let arguments = ProcessInfo.processInfo.arguments
        guard arguments.contains("-uitest") else { return nil }
        let defaults = UserDefaults(suiteName: "uitest") ?? .standard
        defaults.removePersistentDomain(forName: "uitest")
        let today = Day.today()
        let routine = arguments.contains("-sample") ? RoutineModel.sampleRoutine(today: today) : Routine(startDate: today)
        return UITestLaunch(
            model: RoutineModel(routine: routine, today: today),
            purchases: PurchaseStore(defaults: defaults, forceUnlocked: arguments.contains("-unlocked"))
        )
    }
}
#endif
