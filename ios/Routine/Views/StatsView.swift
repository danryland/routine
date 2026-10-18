import SwiftUI

/// Streaks, a monthly calendar and per-habit success rates. Part of Pro.
struct StatsView: View {
    @Environment(RoutineModel.self) private var model
    @Environment(PurchaseStore.self) private var purchases
    @Environment(\.palette) private var palette
    @Environment(\.dismiss) private var dismiss
    @State private var month: Day?
    @State private var showPaywall = false

    var body: some View {
        NavigationStack {
            ScrollView {
                content
                    .padding(16)
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
                    .blur(radius: purchases.isUnlocked ? 0 : 8)
                    .allowsHitTesting(purchases.isUnlocked)
                    .accessibilityHidden(!purchases.isUnlocked)
            }
            .overlay {
                if !purchases.isUnlocked {
                    locked
                }
            }
            .routineBackground(palette)
            .navigationTitle("Stats")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(isPresented: $showPaywall) {
                PaywallView(reason: .feature("Stats"))
                    .environment(purchases)
                    .environment(\.palette, palette)
                    .tint(palette.accent)
            }
        }
    }

    private var content: some View {
        let routine = model.routine
        let today = model.today
        return VStack(spacing: 20) {
            HStack(spacing: 12) {
                StatTile(value: routine.perfectDayCount(through: today), label: "Perfect days", icon: "star.fill")
                StatTile(value: routine.currentPerfectStreak(today: today), label: "Current run", icon: "flame.fill")
                StatTile(value: routine.bestPerfectStreak(through: today), label: "Best run", icon: "trophy.fill")
            }

            MonthCalendar(month: Binding(get: { month ?? today.startOfMonth }, set: { month = $0 }), routine: routine, today: today)

            VStack(spacing: 10) {
                ForEach(routine.habits) { habit in
                    HabitStatsRow(habit: habit, today: today)
                }
            }
        }
    }

    private var locked: some View {
        VStack(spacing: 14) {
            Image(systemName: "lock.fill")
                .font(.system(size: 36))
                .foregroundStyle(palette.accent)
            Text("Streaks and stats are part of Routine Pro")
                .font(.headline)
                .multilineTextAlignment(.center)
            Button("Unlock Routine Pro") { showPaywall = true }
                .buttonStyle(.borderedProminent)
                .accessibilityIdentifier("unlockStats")
        }
        .padding(28)
        .background(RoundedRectangle(cornerRadius: 20, style: .continuous).fill(palette.raised))
        .padding(32)
    }
}

private struct StatTile: View {
    @Environment(\.palette) private var palette
    let value: Int
    let label: String
    let icon: String

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .foregroundStyle(palette.accent)
            Text("\(value)")
                .font(.routine(26, relativeTo: .title))
                .monospacedDigit()
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(palette.raised))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label): \(value)")
    }
}

/// A month of days, each shaded by how much got done.
private struct MonthCalendar: View {
    @Environment(\.palette) private var palette
    @Binding var month: Day
    let routine: Routine
    let today: Day

    private let calendar = Calendar.current

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Button {
                    month = month.addingMonths(-1)
                } label: {
                    Image(systemName: "chevron.left")
                }
                .disabled(month <= routine.startDate.startOfMonth)
                .accessibilityLabel("Previous month")

                Spacer()
                Text(month.startDate(), format: .dateTime.month(.wide).year())
                    .font(.headline)
                Spacer()

                Button {
                    month = month.addingMonths(1)
                } label: {
                    Image(systemName: "chevron.right")
                }
                .disabled(month >= today.startOfMonth)
                .accessibilityLabel("Next month")
            }

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
                ForEach(Array(weekdaySymbols.enumerated()), id: \.offset) { _, symbol in
                    Text(symbol)
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
                ForEach(0..<leadingBlanks, id: \.self) { _ in
                    Color.clear.frame(height: 36)
                }
                ForEach(month.monthDays, id: \.self) { day in
                    DayCell(day: day, routine: routine, today: today)
                }
            }
        }
        .padding(16)
        .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(palette.raised))
    }

    private var weekdaySymbols: [String] {
        let symbols = calendar.veryShortStandaloneWeekdaySymbols
        let first = calendar.firstWeekday - 1
        return Array(symbols[first...] + symbols[..<first])
    }

    private var leadingBlanks: Int {
        // ISO weekday (Mon = 1) to Calendar weekday (Sun = 1).
        let weekday = month.isoWeekday % 7 + 1
        return (weekday - calendar.firstWeekday + 7) % 7
    }
}

private struct DayCell: View {
    @Environment(\.palette) private var palette
    let day: Day
    let routine: Routine
    let today: Day

    var body: some View {
        let isTracked = day >= routine.startDate && day <= today
        let progress = routine.progress(on: day)
        ZStack {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(isTracked && progress.total > 0 ? palette.accent.opacity(0.12 + 0.88 * Double(progress.percent) / 100) : palette.track.opacity(0.35))
            if isTracked && progress.tier == .perfect {
                Image(systemName: "star.fill")
                    .font(.caption)
                    .foregroundStyle(.white)
            } else {
                Text("\(day.day)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(isTracked ? 0.9 : 0.35))
            }
        }
        .frame(height: 36)
        .overlay {
            if day == today {
                RoundedRectangle(cornerRadius: 8, style: .continuous).strokeBorder(.white.opacity(0.8), lineWidth: 1.5)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(day.startDate(), format: .dateTime.day().month(.wide)))
        .accessibilityValue(isTracked ? "\(progress.percent) percent done" : "Not tracked")
    }
}

private struct HabitStatsRow: View {
    @Environment(\.palette) private var palette
    let habit: Habit
    let today: Day

    var body: some View {
        let rate = habit.completionRate(lastDays: 30, today: today)
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(habit.name.isEmpty ? "Untitled" : habit.name)
                    .font(.routine(16, relativeTo: .body))
                    .lineLimit(1)
                Spacer()
                Label("\(habit.currentStreak(today: today))", systemImage: "flame.fill")
                    .labelStyle(.titleAndIcon)
                    .foregroundStyle(palette.accent)
                    .monospacedDigit()
                    .accessibilityLabel("Current streak \(habit.currentStreak(today: today)) days")
            }
            ProgressView(value: rate ?? 0)
                .tint(palette.accent)
            HStack {
                Text(rate.map { "\(Int(($0 * 100).rounded()))% of the last 30 days" } ?? "Not enough days yet")
                Spacer()
                Text("Best \(habit.bestStreak(through: today))")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(palette.raised))
    }
}

#Preview {
    StatsView()
        .environment(RoutineModel.preview)
        .environment(PurchaseStore(forceUnlocked: true))
        .preferredColorScheme(.dark)
}
