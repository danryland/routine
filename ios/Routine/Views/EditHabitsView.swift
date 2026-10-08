import SwiftUI

/// Rename, reorder, re-goal and delete habits.
struct EditHabitsView: View {
    @Environment(RoutineModel.self) private var model
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(model.routine.habits) { habit in
                        EditHabitRow(habit: habit)
                    }
                    .onMove { model.moveHabits(from: $0, to: $1) }
                    .onDelete { model.removeHabits(at: $0) }
                    .listRowBackground(Color.routineMaroon.opacity(0.35))
                } footer: {
                    Text("Drag to reorder. Changing a goal never changes which past days count as done.")
                }
            }
            .environment(\.editMode, .constant(.active))
            .scrollContentBackground(.hidden)
            .background(Color.routineBackground)
            .navigationTitle("Edit habits")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

private struct EditHabitRow: View {
    @Environment(RoutineModel.self) private var model
    let habit: Habit
    @State private var name: String

    init(habit: Habit) {
        self.habit = habit
        _name = State(initialValue: habit.name)
    }

    var body: some View {
        HStack(spacing: 12) {
            TextField("Habit name", text: $name)
                .font(.routine(16, relativeTo: .body))
                .submitLabel(.done)
                .onChange(of: name) { _, newName in
                    model.rename(habit.id, to: newName)
                }

            GoalMenu(goal: habit.goal) { model.setGoal(habit.id, to: $0) }
        }
    }
}

struct GoalMenu: View {
    let goal: Int
    let setGoal: (Int) -> Void

    private var options: [Int] {
        let common = Array(1...20)
        return common.contains(goal) ? common : common + [goal]
    }

    var body: some View {
        Menu {
            Picker("Daily goal", selection: Binding(get: { goal }, set: setGoal)) {
                ForEach(options, id: \.self) { option in
                    Text(GoalText.long(option)).tag(option)
                }
            }
        } label: {
            Text(GoalText.short(goal))
                .font(.subheadline.weight(.semibold))
                .monospacedDigit()
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Capsule().fill(Color.routineMaroon))
        }
        .accessibilityLabel("Daily goal")
        .accessibilityValue(GoalText.long(goal))
    }
}

#Preview {
    EditHabitsView()
        .environment(RoutineModel.preview)
        .preferredColorScheme(.dark)
}
