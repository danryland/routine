import Foundation

/// What's free and what needs the one-time unlock.
enum FreeTier {
    /// Free users can track this many habits; the unlock removes the limit.
    static let habitLimit = 3
}

extension Routine {
    func canAddHabit(isUnlocked: Bool) -> Bool {
        isUnlocked || habits.count < FreeTier.habitLimit
    }
}

/// When to ask for an App Store / Play Store rating: only on a day when
/// everything got done, once the user has had a few of them, and rarely.
enum ReviewPolicy {
    static let minimumPerfectDays = 3
    static let daysBetweenRequests = 120

    static func shouldAsk(perfectDays: Int, lastAsked: Day?, today: Day) -> Bool {
        guard perfectDays >= minimumPerfectDays else { return false }
        guard let lastAsked else { return true }
        return today.ordinal - lastAsked.ordinal >= daysBetweenRequests
    }
}

/// Which days to schedule a daily reminder on, when reminders can't check the
/// routine at the moment they fire (iOS schedules them ahead of time).
enum ReminderSchedule {
    /// The next `count` days from today, skipping today if its reminder time has
    /// passed or everything is already done.
    static func days(
        today: Day,
        minuteOfDayNow: Int,
        reminderMinuteOfDay: Int,
        todayIsPerfect: Bool,
        count: Int = 7
    ) -> [Day] {
        let skipToday = todayIsPerfect || minuteOfDayNow >= reminderMinuteOfDay
        let first = skipToday ? 1 : 0
        return (first..<(first + count)).map { today.adding(days: $0) }
    }
}

/// Exports every tracked day as CSV, one row per habit per day, for spreadsheets.
enum RoutineCSV {
    static let header = "date,habit,count,goal,done"

    static func export(_ routine: Routine, through today: Day) -> String {
        var lines = [header]
        for day in routine.days(through: today) {
            for habit in routine.habits where habit.isTracked(on: day) {
                lines.append([
                    day.iso,
                    escape(habit.name),
                    String(habit.count(on: day)),
                    String(habit.goal),
                    habit.isDone(on: day) ? "yes" : "no",
                ].joined(separator: ","))
            }
        }
        return lines.joined(separator: "\r\n") + "\r\n"
    }

    /// RFC 4180 quoting, plus a leading apostrophe on anything a spreadsheet
    /// would otherwise run as a formula.
    static func escape(_ field: String) -> String {
        var value = field
        // Checked as Unicode scalars: in Swift "\r\n" is a single Character.
        if let first = value.unicodeScalars.first, "=+-@\t\r".unicodeScalars.contains(first) {
            value = "'" + value
        }
        guard value.unicodeScalars.contains(where: { ",\"\r\n".unicodeScalars.contains($0) }) else { return value }
        return "\"" + value.replacingOccurrences(of: "\"", with: "\"\"") + "\""
    }
}
