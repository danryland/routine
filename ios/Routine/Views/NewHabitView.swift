import SwiftUI

struct NewHabitView: View {
    @Environment(RoutineModel.self) private var model
    @Environment(\.palette) private var palette
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
                        .accessibilityIdentifier("newHabitName")
                        .submitLabel(.done)
                        .onSubmit(add)
                }
                .listRowBackground(palette.raised)

                Section {
                    Stepper(value: $goal, in: 1...20) {
                        LabeledContent("Daily goal", value: GoalText.long(goal))
                    }
                } footer: {
                    Text(goal == 1
                         ? "A tick box you check off once a day."
                         : "A counter that fills up each time you tap it.")
                }
                .listRowBackground(palette.raised)
            }
            .routineBackground(palette)
            .navigationTitle("New habit")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add", action: add)
                        .disabled(trimmedName.isEmpty)
                        .accessibilityIdentifier("confirmNewHabit")
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
