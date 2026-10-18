import Foundation

// Streaks and completion rates. "Today" is treated as still in progress: a
// habit not yet done today doesn't break a streak or drag down a rate until
// the day is over.

extension Habit {
    /// Consecutive done days up to today, or up to yesterday if today isn't done yet.
    func currentStreak(today: Day) -> Int {
        var day = isDone(on: today) ? today : today.adding(days: -1)
        var streak = 0
        while isDone(on: day) {
            streak += 1
            day = day.adding(days: -1)
        }
        return streak
    }

    func bestStreak(through today: Day) -> Int {
        let doneDays = counts.keys.filter { $0 <= today && isDone(on: $0) }.sorted()
        var best = 0
        var run = 0
        var previous: Day?
        for day in doneDays {
            run = previous.map { day.ordinal == $0.ordinal + 1 } == true ? run + 1 : 1
            best = max(best, run)
            previous = day
        }
        return best
    }

    /// The share of tracked days in the last `days` days that were done, or nil
    /// if none of those days count yet.
    func completionRate(lastDays days: Int, today: Day) -> Double? {
        let end = isDone(on: today) ? today : today.adding(days: -1)
        let start = max(today.adding(days: -(days - 1)), firstTrackedDay)
        guard start <= end else { return nil }
        let done = (start.ordinal...end.ordinal).filter { isDone(on: Day(ordinal: $0)) }.count
        return Double(done) / Double(end.ordinal - start.ordinal + 1)
    }
}

extension Routine {
    func isPerfect(on day: Day) -> Bool {
        progress(on: day).tier == .perfect
    }

    func perfectDayCount(through today: Day) -> Int {
        days(through: today).filter(isPerfect(on:)).count
    }

    /// Consecutive perfect days up to today, or up to yesterday if today isn't perfect yet.
    func currentPerfectStreak(today: Day) -> Int {
        let first = min(startDate, today)
        var day = isPerfect(on: today) ? today : today.adding(days: -1)
        var streak = 0
        while day >= first, isPerfect(on: day) {
            streak += 1
            day = day.adding(days: -1)
        }
        return streak
    }

    func bestPerfectStreak(through today: Day) -> Int {
        var best = 0
        var run = 0
        for day in days(through: today) {
            run = isPerfect(on: day) ? run + 1 : 0
            best = max(best, run)
        }
        return best
    }
}

extension Day {
    /// ISO weekday: Monday is 1, Sunday is 7.
    var isoWeekday: Int {
        // 1970-01-01 was a Thursday.
        ((ordinal % 7 + 7 + 3) % 7) + 1
    }

    var startOfMonth: Day {
        Day(year: year, month: month, day: 1)!
    }

    var daysInMonth: Int {
        startOfMonth.addingMonths(1).ordinal - startOfMonth.ordinal
    }

    /// The first day of the month `months` away from this one.
    func addingMonths(_ months: Int) -> Day {
        let index = year * 12 + (month - 1) + months
        let newYear = index >= 0 ? index / 12 : (index - 11) / 12
        return Day(year: newYear, month: index - newYear * 12 + 1, day: 1)!
    }

    /// Every day of this day's month.
    var monthDays: [Day] {
        let first = startOfMonth
        return (0..<daysInMonth).map { first.adding(days: $0) }
    }
}
