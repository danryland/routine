import Combine
import SwiftUI

struct ContentView: View {
    @Environment(RoutineModel.self) private var model
    @Environment(\.scenePhase) private var scenePhase
    @State private var isEditing = false
    @State private var isAddingHabit = false

    var body: some View {
        NavigationStack {
            Group {
                if model.routine.habits.isEmpty {
                    EmptyStateView { isAddingHabit = true }
                } else {
                    RoutineGrid()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.routineBackground)
            .navigationTitle("Routine")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(Color.routineBackground, for: .navigationBar, .bottomBar)
            .toolbarBackground(.visible, for: .navigationBar, .bottomBar)
            .toolbar {
                ToolbarItemGroup(placement: .bottomBar) {
                    Button {
                        isEditing = true
                    } label: {
                        Label("Edit habits", systemImage: "pencil")
                    }
                    .disabled(model.routine.habits.isEmpty)

                    Spacer()

                    Button {
                        isAddingHabit = true
                    } label: {
                        Label("New habit", systemImage: "plus")
                    }
                }
            }
        }
        .sheet(isPresented: $isEditing) {
            EditHabitsView()
                .environment(model)
        }
        .sheet(isPresented: $isAddingHabit) {
            NewHabitView()
                .environment(model)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                model.refreshToday()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: RunLoop.main)) { _ in
            model.refreshToday()
        }
    }
}

struct EmptyStateView: View {
    let addHabit: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.square.fill")
                .font(.system(size: 56))
                .foregroundStyle(Color.routineRose)
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
        }
        .padding(32)
        .frame(maxWidth: 420)
    }
}

#Preview("Routine") {
    ContentView()
        .environment(RoutineModel.preview)
        .preferredColorScheme(.dark)
        .tint(.routineRose)
}

#Preview("Empty") {
    ContentView()
        .environment(RoutineModel(routine: Routine(startDate: .today()), today: .today()))
        .preferredColorScheme(.dark)
        .tint(.routineRose)
}
