import Combine
import StoreKit
import SwiftUI

struct ContentView: View {
    enum Sheet: String, Identifiable {
        case editHabits, newHabit, paywall, stats, settings
        var id: String { rawValue }
    }

    @Environment(RoutineModel.self) private var model
    @Environment(PurchaseStore.self) private var purchases
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.requestReview) private var requestReview
    @State private var sheet: Sheet?

    var body: some View {
        NavigationStack {
            Group {
                if model.routine.habits.isEmpty {
                    EmptyStateView { addHabit() }
                } else {
                    RoutineGrid()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(palette.background)
            .navigationTitle("Routine")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(palette.background, for: .navigationBar, .bottomBar)
            .toolbarBackground(.visible, for: .navigationBar, .bottomBar)
            .toolbar {
                ToolbarItemGroup(placement: .bottomBar) {
                    Button {
                        sheet = .editHabits
                    } label: {
                        Label("Edit habits", systemImage: "pencil")
                    }
                    .disabled(model.routine.habits.isEmpty)
                    .accessibilityIdentifier("editHabits")

                    Button {
                        sheet = .stats
                    } label: {
                        Label("Stats", systemImage: "chart.bar.xaxis")
                    }
                    .accessibilityIdentifier("stats")

                    Button {
                        sheet = .settings
                    } label: {
                        Label("Settings", systemImage: "gearshape")
                    }
                    .accessibilityIdentifier("settings")

                    Spacer()

                    Button {
                        addHabit()
                    } label: {
                        Label("New habit", systemImage: "plus")
                    }
                    .accessibilityIdentifier("newHabit")
                }
            }
        }
        .sheet(item: $sheet) { sheet in
            Group {
                switch sheet {
                case .editHabits: EditHabitsView()
                case .newHabit: NewHabitView()
                case .paywall: PaywallView(reason: .habitLimit)
                case .stats: StatsView()
                case .settings: SettingsView()
                }
            }
            .environment(model)
            .environment(purchases)
            .environment(\.palette, palette)
            .tint(palette.accent)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                model.becameActive()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: RunLoop.main)) { _ in
            model.becameActive()
        }
        .onChange(of: model.isReviewRequestDue) { _, due in
            guard due else { return }
            requestReview()
            model.didRequestReview()
        }
    }

    private func addHabit() {
        sheet = model.canAddHabit ? .newHabit : .paywall
    }
}

struct EmptyStateView: View {
    @Environment(\.palette) private var palette
    let addHabit: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.square.fill")
                .font(.system(size: 56))
                .foregroundStyle(palette.accent)
            Text("Build your routine")
                .font(.routine(24, relativeTo: .title2))
            Text("Add the small things you want to do every day, then tick them off as you go.")
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Add a habit", action: addHabit)
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .padding(.top, 8)
                .accessibilityIdentifier("addFirstHabit")
        }
        .padding(32)
        .frame(maxWidth: 420)
    }
}

#Preview("Routine") {
    ContentView()
        .environment(RoutineModel.preview)
        .environment(PurchaseStore(forceUnlocked: true))
        .preferredColorScheme(.dark)
}

#Preview("Empty") {
    ContentView()
        .environment(RoutineModel(routine: Routine(startDate: .today()), today: .today()))
        .environment(PurchaseStore())
        .preferredColorScheme(.dark)
}
