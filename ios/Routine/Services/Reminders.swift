import Foundation
import UserNotifications

/// A daily "don't forget" notification at the user's chosen time.
///
/// iOS can't check the routine when a notification fires, so a week of them is
/// scheduled ahead, and today's is dropped as soon as everything's done.
enum Reminders {
    private static let prefix = "reminder-"

    static func requestPermission() async -> Bool {
        (try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    static func reschedule(enabled: Bool, minuteOfDay: Int, routine: Routine, today: Day, calendar: Calendar = .current) async {
        let center = UNUserNotificationCenter.current()
        let pending = await center.pendingNotificationRequests().map(\.identifier).filter { $0.hasPrefix(prefix) }
        center.removePendingNotificationRequests(withIdentifiers: pending)
        guard enabled else { return }

        let now = calendar.dateComponents([.hour, .minute], from: Date())
        let days = ReminderSchedule.days(
            today: today,
            minuteOfDayNow: (now.hour ?? 0) * 60 + (now.minute ?? 0),
            reminderMinuteOfDay: minuteOfDay,
            todayIsPerfect: routine.isPerfect(on: today) || routine.habits.isEmpty
        )
        for day in days {
            let content = UNMutableNotificationContent()
            content.title = "Routine"
            content.body = "Don’t forget today’s habits."
            content.sound = .default
            let trigger = UNCalendarNotificationTrigger(
                dateMatching: DateComponents(year: day.year, month: day.month, day: day.day, hour: minuteOfDay / 60, minute: minuteOfDay % 60),
                repeats: false
            )
            try? await center.add(UNNotificationRequest(identifier: prefix + day.iso, content: content, trigger: trigger))
        }
    }
}
