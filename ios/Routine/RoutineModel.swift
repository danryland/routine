import Foundation
import Observation
import os

/// Owns the routine for the UI, and saves every change straight away.
@MainActor
@Observable
final class RoutineModel {
    private(set) var routine: Routine
    private(set) var today: Day

    @ObservationIgnored private let store: RoutineStore?
    @ObservationIgnored private let calendar: Calendar
    @ObservationIgnored private let log = Logger(subsystem: "wales.ryland.routine", category: "storage")

    init(store: RoutineStore?, calendar: Calendar = .current) {
        let today = Day.today(calendar: calendar)
        self.store = store
        self.calendar = calendar
        self.today = today
        self.routine = store?.load(today: today) ?? Routine(startDate: today)
    }

    /// An in-memory model, for previews.
    init(routine: Routine, today: Day) {
        self.store = nil
        self.calendar = .current
        self.today = today
        self.routine = routine
    }

    static func live() -> RoutineModel {
        RoutineModel(store: .applicationSupport())
    }

    var days: [Day] {
        routine.days(through: today)
    }

    /// Call when the app comes to the foreground or the date changes, so a new
    /// column appears at midnight.
    func refreshToday() {
        let now = Day.today(calendar: calendar)
        if now != today {
            today = now
        }
    }

    // MARK: - Edits

    func tap(_ id: Habit.ID, on day: Day) {
        update { $0.tap(id, on: day) }
    }

    func addHabit(named name: String, goal: Int) {
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

    private func update(_ change: (inout Routine) -> Void) {
        var edited = routine
        change(&edited)
        guard edited != routine else { return }
        routine = edited
        guard let store else { return }
        do {
            try store.save(routine)
        } catch {
            log.error("Couldn't save routine: \(error.localizedDescription, privacy: .public)")
        }
    }
}

extension RoutineModel {
    /// Sample data for SwiftUI previews.
    static var preview: RoutineModel {
        let today = Day.today()
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
        return RoutineModel(routine: routine, today: today)
    }
}
