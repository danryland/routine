import Foundation

/// Something the user wants to do every day.
///
/// `goal` is how many times a day it needs doing: `1` is a simple tick box,
/// anything higher is a counter (e.g. "Water ×6") that fills up as it's tapped.
struct Habit: Identifiable, Hashable, Sendable {
    static let goalRange = 1...99

    var id: UUID
    var name: String
    private(set) var goal: Int
    /// The day the habit was added. Days before this don't count against it.
    var createdOn: Day
    /// How many times the habit was done on each day. Days with zero are absent.
    private(set) var counts: [Day: Int]

    init(id: UUID = UUID(), name: String, goal: Int = 1, createdOn: Day, counts: [Day: Int] = [:]) {
        let goal = Habit.clampGoal(goal)
        self.id = id
        self.name = name
        self.goal = goal
        self.createdOn = createdOn
        self.counts = counts.filter { $0.value > 0 }.mapValues { min($0, goal) }
    }

    var isCounter: Bool { goal > 1 }

    func count(on day: Day) -> Int {
        counts[day] ?? 0
    }

    func isDone(on day: Day) -> Bool {
        count(on: day) >= goal
    }

    /// The first day this habit counts towards the daily score: the day it was
    /// created, or earlier if the user back-filled a day before that.
    var firstTrackedDay: Day {
        counts.keys.min().map { min($0, createdOn) } ?? createdOn
    }

    func isTracked(on day: Day) -> Bool {
        day >= firstTrackedDay
    }

    /// A tick box toggles. A counter goes up by one, then wraps back to zero
    /// after it reaches its goal (so a mistaken tap can always be undone).
    mutating func tap(on day: Day) {
        let current = count(on: day)
        set(current >= goal ? 0 : current + 1, on: day)
    }

    mutating func set(_ count: Int, on day: Day) {
        let clamped = max(0, min(count, goal))
        counts[day] = clamped == 0 ? nil : clamped
    }

    /// Changes the daily goal without rewriting history: days that were done
    /// stay done, and days that weren't stay not done.
    mutating func setGoal(_ newValue: Int) {
        let newGoal = Habit.clampGoal(newValue)
        guard newGoal != goal else { return }
        let oldGoal = goal
        counts = counts.compactMapValues { count in
            if count >= oldGoal { return newGoal }
            let partial = min(count, newGoal - 1)
            return partial > 0 ? partial : nil
        }
        goal = newGoal
    }

    static func clampGoal(_ goal: Int) -> Int {
        min(max(goal, goalRange.lowerBound), goalRange.upperBound)
    }
}

/// How much of the routine was done on one day.
struct DayProgress: Equatable, Sendable {
    enum Tier: Equatable, Sendable {
        case nothingToDo, none, low, medium, high, nearlyThere, perfect
    }

    let done: Int
    let total: Int

    /// Whole percent, rounded down so that 100% only ever means everything.
    var percent: Int {
        total == 0 ? 0 : done * 100 / total
    }

    var tier: Tier {
        if total == 0 { return .nothingToDo }
        if done == total { return .perfect }
        switch percent {
        case 76...: return .nearlyThere
        case 51...: return .high
        case 26...: return .medium
        case 1...: return .low
        default: return .none
        }
    }
}

/// The user's whole routine: an ordered list of habits and the day tracking began.
struct Routine: Equatable, Sendable {
    static let currentVersion = 1

    var startDate: Day
    var habits: [Habit]

    init(startDate: Day, habits: [Habit] = []) {
        self.startDate = startDate
        self.habits = habits
    }

    /// Every day from the start of tracking up to and including `today`.
    func days(through today: Day) -> [Day] {
        let first = min(startDate, today)
        return (first.ordinal...today.ordinal).map(Day.init(ordinal:))
    }

    func progress(on day: Day) -> DayProgress {
        let tracked = habits.filter { $0.isTracked(on: day) }
        return DayProgress(done: tracked.filter { $0.isDone(on: day) }.count, total: tracked.count)
    }

    // MARK: - Editing

    func index(of id: Habit.ID) -> Int? {
        habits.firstIndex { $0.id == id }
    }

    @discardableResult
    mutating func addHabit(named name: String, goal: Int = 1, on today: Day) -> Habit {
        let habit = Habit(name: name, goal: goal, createdOn: today)
        habits.append(habit)
        return habit
    }

    mutating func tap(_ id: Habit.ID, on day: Day) {
        guard let index = index(of: id) else { return }
        habits[index].tap(on: day)
    }

    mutating func rename(_ id: Habit.ID, to name: String) {
        guard let index = index(of: id) else { return }
        habits[index].name = name
    }

    mutating func setGoal(_ id: Habit.ID, to goal: Int) {
        guard let index = index(of: id) else { return }
        habits[index].setGoal(goal)
    }

    mutating func removeHabits(at offsets: IndexSet) {
        for index in offsets.sorted(by: >) where habits.indices.contains(index) {
            habits.remove(at: index)
        }
    }

    /// Same semantics as SwiftUI's `move(fromOffsets:toOffset:)`: `destination`
    /// is an index in the array *before* the move.
    mutating func moveHabits(from source: IndexSet, to destination: Int) {
        let moving = source.filter { habits.indices.contains($0) }.map { habits[$0] }
        let insertAt = destination - source.filter { $0 < destination }.count
        removeHabits(at: source)
        habits.insert(contentsOf: moving, at: max(0, min(insertAt, habits.count)))
    }
}
