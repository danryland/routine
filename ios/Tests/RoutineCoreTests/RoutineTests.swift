import Foundation
import Testing
@testable import RoutineCore

private func day(_ iso: String) -> Day { Day(iso: iso)! }

struct HabitTests {
    @Test func tickBoxToggles() {
        var habit = Habit(name: "SPF", createdOn: day("2024-08-01"))
        habit.tap(on: day("2024-08-02"))
        #expect(habit.isDone(on: day("2024-08-02")))
        habit.tap(on: day("2024-08-02"))
        #expect(!habit.isDone(on: day("2024-08-02")))
        #expect(habit.counts.isEmpty)
    }

    @Test func counterFillsThenWrapsToZero() {
        var habit = Habit(name: "Water", goal: 3, createdOn: day("2024-08-01"))
        let today = day("2024-08-02")
        var seen: [Int] = []
        for _ in 0..<5 {
            habit.tap(on: today)
            seen.append(habit.count(on: today))
        }
        #expect(seen == [1, 2, 3, 0, 1])
    }

    @Test func changingGoalKeepsWhetherPastDaysWereDone() {
        var habit = Habit(
            name: "Water", goal: 6, createdOn: day("2024-08-01"),
            counts: [day("2024-08-01"): 6, day("2024-08-02"): 5, day("2024-08-03"): 2]
        )
        habit.setGoal(4)
        #expect(habit.isDone(on: day("2024-08-01")))
        #expect(habit.count(on: day("2024-08-02")) == 3) // was 5/6: still not done
        #expect(habit.count(on: day("2024-08-03")) == 2)

        habit.setGoal(8)
        #expect(habit.isDone(on: day("2024-08-01")))
        #expect(!habit.isDone(on: day("2024-08-02")))

        habit.setGoal(1)
        #expect(habit.isDone(on: day("2024-08-01")))
        #expect(!habit.isDone(on: day("2024-08-02")))
        #expect(habit.counts.count == 1) // a tick box has no "partly done", so partial days clear
    }

    @Test func goalIsClamped() {
        #expect(Habit(name: "", goal: 0, createdOn: day("2024-08-01")).goal == 1)
        #expect(Habit(name: "", goal: 500, createdOn: day("2024-08-01")).goal == 99)
    }

    @Test func backfillingExtendsTrackingEarlier() {
        var habit = Habit(name: "Read", createdOn: day("2024-08-10"))
        #expect(!habit.isTracked(on: day("2024-08-09")))
        habit.tap(on: day("2024-08-05"))
        #expect(habit.firstTrackedDay == day("2024-08-05"))
        #expect(habit.isTracked(on: day("2024-08-09")))
    }
}

struct ProgressTests {
    @Test(arguments: [
        (0, 0, DayProgress.Tier.nothingToDo),
        (0, 4, .none),
        (1, 4, .low),
        (2, 4, .medium),
        (3, 4, .high),
        (4, 5, .nearlyThere),
        (5, 5, .perfect),
    ])
    func tiers(done: Int, total: Int, tier: DayProgress.Tier) {
        #expect(DayProgress(done: done, total: total).tier == tier)
    }

    @Test func percentRoundsDownSoOnlyEverythingIsAHundred() {
        #expect(DayProgress(done: 199, total: 200).percent == 99)
        #expect(DayProgress(done: 199, total: 200).tier == .nearlyThere)
        #expect(DayProgress(done: 1, total: 3).percent == 33)
    }
}

struct RoutineTests {
    @Test func daysRunFromStartThroughToday() {
        let routine = Routine(startDate: day("2024-02-27"))
        #expect(routine.days(through: day("2024-03-01")).map(\.iso) == ["2024-02-27", "2024-02-28", "2024-02-29", "2024-03-01"])
        // If the clock goes backwards, still show today rather than nothing.
        #expect(routine.days(through: day("2024-02-20")).map(\.iso).last == "2024-02-20")
    }

    @Test func newHabitsDontLowerPastScores() {
        var routine = Routine(startDate: day("2024-08-01"))
        let teeth = routine.addHabit(named: "Teeth", on: day("2024-08-01"))
        routine.tap(teeth.id, on: day("2024-08-01"))
        routine.addHabit(named: "Floss", on: day("2024-08-02"))

        #expect(routine.progress(on: day("2024-08-01")).tier == .perfect)
        #expect(routine.progress(on: day("2024-08-02")) == DayProgress(done: 0, total: 2))
    }

    @Test func moveMatchesSwiftUISemantics() {
        var routine = Routine(startDate: day("2024-08-01"))
        for name in ["A", "B", "C", "D"] { routine.addHabit(named: name, on: day("2024-08-01")) }

        routine.moveHabits(from: [0], to: 3)
        #expect(routine.habits.map(\.name) == ["B", "C", "A", "D"])
        routine.moveHabits(from: [3], to: 0)
        #expect(routine.habits.map(\.name) == ["D", "B", "C", "A"])
        routine.moveHabits(from: [0, 2], to: 4)
        #expect(routine.habits.map(\.name) == ["B", "A", "D", "C"])
    }

    @Test func editsTargetTheRightHabit() {
        var routine = Routine(startDate: day("2024-08-01"))
        let a = routine.addHabit(named: "A", on: day("2024-08-01"))
        let b = routine.addHabit(named: "B", on: day("2024-08-01"))
        routine.rename(b.id, to: "Bee")
        routine.setGoal(a.id, to: 4)
        routine.removeHabits(at: [0])
        #expect(routine.habits.map(\.name) == ["Bee"])
        #expect(routine.habits[0].goal == 1)
    }
}
