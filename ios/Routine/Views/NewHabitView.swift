import SwiftUI

struct NewHabitView: View {
    @Environment(RoutineModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var goal = 1
    @FocusState private var isNameFocused: Bool

    private var trimmedName: String {
        name.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("e.g. Drink water", text: $name)
                        .font(.routine(17, relativeTo: .body))
                        .focused($isNameFocused)
                        .submitLabel(.done)
                        .onSubmit(add)
                }
                .listRowBackground(Color.routineMaroon.opacity(0.35))

                Section {
                    Stepper(value: $goal, in: 1...20) {
                        LabeledContent("Daily goal", value: GoalText.long(goal))
                    }
                } footer: {
                    Text(goal == 1
                         ? "A tick box you check off once a day."
                         : "A counter that fills up each time you tap it.")
                }
                .listRowBackground(Color.routineMaroon.opacity(0.35))
            }
            .scrollContentBackground(.hidden)
            .background(Color.routineBackground)
            .navigationTitle("New habit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add", action: add)
                        .disabled(trimmedName.isEmpty)
                }
            }
            .onAppear { isNameFocused = true }
        }
        .presentationDetents([.medium, .large])
    }

    private func add() {
        guard !trimmedName.isEmpty else { return }
        model.addHabit(named: trimmedName, goal: goal)
        dismiss()
    }
}

#Preview {
    NewHabitView()
        .environment(RoutineModel.preview)
        .preferredColorScheme(.dark)
}
