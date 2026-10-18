import Foundation
import Testing
@testable import RoutineCore

private func day(_ iso: String) -> Day { Day(iso: iso)! }

/// A tick-box habit done on the given days.
private func habit(done days: [String], createdOn: String = "2024-08-01", goal: Int = 1) -> Habit {
    Habit(name: "H", goal: goal, createdOn: day(createdOn), counts: Dictionary(uniqueKeysWithValues: days.map { (day($0), goal) }))
}

struct StreakTests {
    @Test func currentStreakDoesntBreakUntilTheDayIsOver() {
        let h = habit(done: ["2024-08-05", "2024-08-06", "2024-08-07"])
        #expect(h.currentStreak(today: day("2024-08-07")) == 3) // done today
        #expect(h.currentStreak(today: day("2024-08-08")) == 3) // today still open
        #expect(h.currentStreak(today: day("2024-08-09")) == 0) // missed yesterday
    }

    @Test func bestStreakFindsTheLongestRun() {
        let h = habit(done: ["2024-08-01", "2024-08-02", "2024-08-04", "2024-08-05", "2024-08-06", "2024-08-09"])
        #expect(h.bestStreak(through: day("2024-08-31")) == 3)
        #expect(h.bestStreak(through: day("2024-08-04")) == 2)
    }

    @Test func partlyDoneCountersDontCount() {
        var h = Habit(name: "Water", goal: 3, createdOn: day("2024-08-01"))
        h.set(3, on: day("2024-08-01"))
        h.set(2, on: day("2024-08-02"))
        #expect(h.bestStreak(through: day("2024-08-02")) == 1)
    }

    @Test func completionRateCountsOnlyTrackedFinishedDays() {
        let h = habit(done: ["2024-08-08", "2024-08-09"], createdOn: "2024-08-07")
        // Tracked 7th–9th (today, the 10th, isn't over and isn't done): 2 of 3.
        #expect(h.completionRate(lastDays: 30, today: day("2024-08-10")) == 2.0 / 3.0)
        // Doing it today counts straight away: 3 of 4.
        var done = h
        done.tap(on: day("2024-08-10"))
        #expect(done.completionRate(lastDays: 30, today: day("2024-08-10")) == 3.0 / 4.0)
        // Only the window counts.
        #expect(h.completionRate(lastDays: 2, today: day("2024-08-10")) == 1.0)
        // Created today, nothing done: no rate yet.
        #expect(habit(done: [], createdOn: "2024-08-10").completionRate(lastDays: 30, today: day("2024-08-10")) == nil)
    }

    @Test func perfectDays() {
        var routine = Routine(startDate: day("2024-08-01"))
        let a = routine.addHabit(named: "A", on: day("2024-08-01"))
        let b = routine.addHabit(named: "B", on: day("2024-08-01"))
        for d in ["2024-08-01", "2024-08-02", "2024-08-04"] {
            routine.tap(a.id, on: day(d))
            routine.tap(b.id, on: day(d))
        }
        routine.tap(a.id, on: day("2024-08-03")) // only half done
        #expect(routine.perfectDayCount(through: day("2024-08-05")) == 3)
        #expect(routine.bestPerfectStreak(through: day("2024-08-05")) == 2)
        #expect(routine.currentPerfectStreak(today: day("2024-08-05")) == 1) // the 4th; the 5th is still open
        #expect(routine.currentPerfectStreak(today: day("2024-08-06")) == 0)
    }

    @Test func perfectStreakStopsAtTheStartOfTracking() {
        var routine = Routine(startDate: day("2024-08-01"))
        let a = routine.addHabit(named: "A", on: day("2024-08-01"))
        routine.tap(a.id, on: day("2024-08-01"))
        routine.tap(a.id, on: day("2024-08-02"))
        #expect(routine.currentPerfectStreak(today: day("2024-08-02")) == 2)
    }
}

struct CalendarTests {
    @Test func weekdays() {
        #expect(day("1970-01-01").isoWeekday == 4) // Thursday
        #expect(day("2026-10-12").isoWeekday == 1) // Monday
        #expect(day("2026-10-18").isoWeekday == 7) // Sunday
        #expect(day("1969-12-29").isoWeekday == 1) // before the epoch
    }

    @Test func months() {
        #expect(day("2024-02-17").startOfMonth == day("2024-02-01"))
        #expect(day("2024-02-17").daysInMonth == 29)
        #expect(day("2023-02-17").daysInMonth == 28)
        #expect(day("2024-12-31").daysInMonth == 31)
        #expect(day("2024-11-30").addingMonths(1) == day("2024-12-01"))
        #expect(day("2024-12-31").addingMonths(1) == day("2025-01-01"))
        #expect(day("2025-01-15").addingMonths(-1) == day("2024-12-01"))
        #expect(day("2025-01-15").addingMonths(-13) == day("2023-12-01"))
        #expect(day("2024-04-10").monthDays.count == 30)
        #expect(day("2024-04-10").monthDays.last == day("2024-04-30"))
    }
}

struct PolicyTests {
    @Test func freeTierAllowsThreeHabits() {
        var routine = Routine(startDate: day("2024-08-01"))
        for name in ["A", "B"] { routine.addHabit(named: name, on: day("2024-08-01")) }
        #expect(routine.canAddHabit(isUnlocked: false))
        routine.addHabit(named: "C", on: day("2024-08-01"))
        #expect(!routine.canAddHabit(isUnlocked: false))
        #expect(routine.canAddHabit(isUnlocked: true))
    }

    @Test func reviewIsRequestedRarelyAndOnlyAfterAFewPerfectDays() {
        #expect(!ReviewPolicy.shouldAsk(perfectDays: 2, lastAsked: nil, today: day("2024-08-10")))
        #expect(ReviewPolicy.shouldAsk(perfectDays: 3, lastAsked: nil, today: day("2024-08-10")))
        #expect(!ReviewPolicy.shouldAsk(perfectDays: 30, lastAsked: day("2024-06-01"), today: day("2024-08-10")))
        #expect(ReviewPolicy.shouldAsk(perfectDays: 30, lastAsked: day("2024-04-01"), today: day("2024-08-10")))
    }

    @Test func remindersSkipTodayWhenItsDoneOrTooLate() {
        let today = day("2024-08-10")
        let evening = 20 * 60
        let scheduled = ReminderSchedule.days(today: today, minuteOfDayNow: 9 * 60, reminderMinuteOfDay: evening, todayIsPerfect: false)
        #expect(scheduled.first == today)
        #expect(scheduled.count == 7)
        #expect(ReminderSchedule.days(today: today, minuteOfDayNow: 9 * 60, reminderMinuteOfDay: evening, todayIsPerfect: true).first == today.adding(days: 1))
        #expect(ReminderSchedule.days(today: today, minuteOfDayNow: 21 * 60, reminderMinuteOfDay: evening, todayIsPerfect: false).first == today.adding(days: 1))
    }
}

struct ExportTests {
    @Test func csvHasARowPerTrackedHabitPerDay() {
        var routine = Routine(startDate: day("2024-08-01"))
        let teeth = routine.addHabit(named: "Teeth", on: day("2024-08-01"))
        let water = routine.addHabit(named: "Water, glasses", goal: 3, on: day("2024-08-02"))
        routine.tap(teeth.id, on: day("2024-08-01"))
        routine.tap(water.id, on: day("2024-08-02"))

        #expect(RoutineCSV.export(routine, through: day("2024-08-02")) == [
            "date,habit,count,goal,done",
            "2024-08-01,Teeth,1,1,yes",
            "2024-08-02,Teeth,0,1,no",
            "2024-08-02,\"Water, glasses\",1,3,no",
            "",
        ].joined(separator: "\r\n"))
    }

    @Test func csvEscaping() {
        #expect(RoutineCSV.escape("plain") == "plain")
        #expect(RoutineCSV.escape("say \"hi\"") == "\"say \"\"hi\"\"\"")
        #expect(RoutineCSV.escape("=SUM(A1)") == "'=SUM(A1)")
        #expect(RoutineCSV.escape("line\nbreak") == "\"line\nbreak\"")
    }
}

struct RestoreTests {
    @Test func restoresEitherFormatButNotJunk() throws {
        let saved = try Routine(startDate: day("2024-08-01")).encoded()
        #expect(Routine.restoring(from: saved, today: day("2026-10-18")) == Routine(startDate: day("2024-08-01")))

        let legacy = Data(#"{"dates":["1 Aug 2024"],"tasks":[{"name":"Old","completed":[]}]}"#.utf8)
        #expect(Routine.restoring(from: legacy, today: day("2026-10-18"))?.habits.map(\.name) == ["Old"])

        #expect(Routine.restoring(from: Data(#"{"something":"else"}"#.utf8), today: day("2026-10-18")) == nil)
        #expect(Routine.restoring(from: Data("not json".utf8), today: day("2026-10-18")) == nil)
    }

    @Test func storeReportsWhereTheRoutineCameFrom() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        let defaults = try #require(UserDefaults(suiteName: "FeatureTests.\(UUID().uuidString)"))
        defaults.set(#"{"dates":[],"tasks":[]}"#, forKey: LegacyImport.userDefaultsKey)
        let store = RoutineStore(fileURL: directory.appendingPathComponent("routine.json"), legacyDefaults: defaults)

        #expect(store.lastModified == nil)
        #expect(store.loadReportingOrigin(today: day("2026-10-18")).origin == .importedFromLegacyApp)
        #expect(store.loadReportingOrigin(today: day("2026-10-18")).origin == .saved)
        #expect(store.lastModified != nil)

        let fresh = RoutineStore(fileURL: directory.appendingPathComponent("other.json"), legacyDefaults: nil)
        #expect(fresh.loadReportingOrigin(today: day("2026-10-18")).origin == .new)
    }
}

struct SyncTests {
    private let earlier = Date(timeIntervalSince1970: 1_000)
    private let later = Date(timeIntervalSince1970: 2_000)

    private func routine(_ names: String...) -> Routine {
        var r = Routine(startDate: day("2024-08-01"))
        for name in names { r.addHabit(named: name, on: day("2024-08-01")) }
        return r
    }

    @Test func newestChangeWins() {
        let mine = routine("A"), theirs = routine("B")
        #expect(SyncDecision.decide(local: mine, localModified: earlier, remote: theirs, remoteModified: later) == .useRemote)
        #expect(SyncDecision.decide(local: mine, localModified: later, remote: theirs, remoteModified: earlier) == .pushLocal)
        #expect(SyncDecision.decide(local: mine, localModified: later, remote: mine, remoteModified: earlier) == .inSync)
    }

    @Test func anEmptyRoutineNeverOverwritesARealOne() {
        // A brand-new device (newer, but empty) adopts the cloud copy.
        #expect(SyncDecision.decide(local: routine(), localModified: later, remote: routine("A"), remoteModified: earlier) == .useRemote)
        // And an empty cloud copy never replaces this device's habits.
        #expect(SyncDecision.decide(local: routine("A"), localModified: earlier, remote: routine(), remoteModified: later) == .pushLocal)
    }

    @Test func firstSyncUploadsOnlyRealData() {
        #expect(SyncDecision.decide(local: routine("A"), localModified: earlier, remote: nil, remoteModified: nil) == .pushLocal)
        #expect(SyncDecision.decide(local: routine(), localModified: earlier, remote: nil, remoteModified: nil) == .inSync)
    }
}
