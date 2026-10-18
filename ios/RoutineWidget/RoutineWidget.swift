import AppIntents
import SwiftUI
import WidgetKit

@main
struct RoutineWidgetBundle: WidgetBundle {
    var body: some Widget {
        TodayWidget()
    }
}

struct TodayWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "TodayWidget", provider: TodayProvider()) { entry in
            TodayWidgetView(entry: entry)
        }
        .configurationDisplayName("Today")
        .description("Today’s habits. Tap to tick them off.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryCircular, .accessoryRectangular])
    }
}

struct TodayEntry: TimelineEntry {
    let date: Date
    let today: Day
    let routine: Routine
    let isUnlocked: Bool
    let palette: Palette

    var progress: DayProgress {
        routine.progress(on: today)
    }

    static func current() -> TodayEntry {
        let today = Day.today()
        let defaults = AppGroup.defaults
        let isUnlocked = defaults.bool(forKey: SettingsKey.isUnlocked)
        return TodayEntry(
            date: Date(),
            today: today,
            routine: AppGroup.store().load(today: today),
            isUnlocked: isUnlocked,
            palette: Palette.named(defaults.string(forKey: SettingsKey.theme), isUnlocked: isUnlocked)
        )
    }

    static var sample: TodayEntry {
        let today = Day.today()
        var routine = Routine(startDate: today)
        for (name, goal) in [("Teeth", 1), ("Stretch", 1), ("Read", 1), ("Water", 6)] {
            routine.addHabit(named: name, goal: goal, on: today)
        }
        routine.tap(routine.habits[0].id, on: today)
        routine.tap(routine.habits[3].id, on: today)
        routine.tap(routine.habits[3].id, on: today)
        return TodayEntry(date: Date(), today: today, routine: routine, isUnlocked: true, palette: .rose)
    }
}

struct TodayProvider: TimelineProvider {
    func placeholder(in context: Context) -> TodayEntry {
        .sample
    }

    func getSnapshot(in context: Context, completion: @escaping (TodayEntry) -> Void) {
        completion(context.isPreview ? .sample : .current())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TodayEntry>) -> Void) {
        let entry = TodayEntry.current()
        // Refresh at midnight for the new day; ticks and app changes reload sooner.
        let midnight = entry.today.adding(days: 1).startDate()
        completion(Timeline(entries: [entry], policy: .after(midnight)))
    }
}

struct TodayWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: TodayEntry

    var body: some View {
        content
            .environment(\.palette, entry.palette)
            .containerBackground(for: .widget) {
                entry.palette.background
            }
    }

    @ViewBuilder
    private var content: some View {
        switch family {
        case .accessoryCircular:
            CircularAccessory(entry: entry)
        case .accessoryRectangular:
            RectangularAccessory(entry: entry)
        default:
            if !entry.isUnlocked {
                LockedWidget()
            } else if entry.routine.habits.isEmpty {
                Text("Add habits in Routine to see them here.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            } else if family == .systemSmall {
                SmallWidget(entry: entry)
            } else {
                HabitListWidget(entry: entry, maxRows: family == .systemLarge ? 8 : 4)
            }
        }
    }
}

private struct ProgressRing: View {
    @Environment(\.palette) private var palette
    let progress: DayProgress
    var lineWidth: CGFloat = 8

    var body: some View {
        ZStack {
            Circle().stroke(palette.track, lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: progress.total == 0 ? 0 : CGFloat(progress.done) / CGFloat(progress.total))
                .stroke(palette.accent, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
            if progress.tier == .perfect {
                Image(systemName: "star.fill")
                    .font(.title2)
                    .foregroundStyle(LinearGradient(colors: [.routineGold, .routineYellow], startPoint: .topLeading, endPoint: .bottomTrailing))
            } else {
                Text("\(progress.done)/\(progress.total)")
                    .font(.system(.title3, design: .rounded, weight: .bold))
                    .monospacedDigit()
            }
        }
    }
}

private struct SmallWidget: View {
    let entry: TodayEntry

    var body: some View {
        VStack(spacing: 8) {
            ProgressRing(progress: entry.progress, lineWidth: 10)
                .padding(4)
            Text("Today")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
        }
    }
}

private struct HabitListWidget: View {
    @Environment(\.palette) private var palette
    let entry: TodayEntry
    let maxRows: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Today")
                    .font(.headline)
                Spacer()
                if entry.progress.tier == .perfect {
                    Image(systemName: "star.fill").foregroundStyle(Color.routineGold)
                } else {
                    Text("\(entry.progress.percent)%")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(palette.accent)
                }
            }
            ForEach(entry.routine.habits.prefix(maxRows)) { habit in
                HStack(spacing: 10) {
                    Text(habit.name.isEmpty ? "Untitled" : habit.name)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                    Spacer(minLength: 4)
                    Button(intent: ToggleHabitIntent(habitID: habit.id.uuidString)) {
                        WidgetCell(habit: habit, day: entry.today)
                    }
                    .buttonStyle(.plain)
                }
            }
            if entry.routine.habits.count > maxRows {
                Text("+\(entry.routine.habits.count - maxRows) more")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
    }
}

private struct WidgetCell: View {
    @Environment(\.palette) private var palette
    let habit: Habit
    let day: Day

    var body: some View {
        let isDone = habit.isDone(on: day)
        ZStack {
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(isDone ? palette.accent : Color.clear)
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .strokeBorder(isDone ? palette.accent : palette.track, lineWidth: 2)
            if isDone || !habit.isCounter {
                Image(systemName: "checkmark")
                    .font(.system(size: 13, weight: .black))
                    .foregroundStyle(isDone ? Color.white : palette.track)
            } else {
                Text("\(habit.count(on: day))/\(habit.goal)")
                    .font(.system(size: 10, weight: .bold, design: .rounded))
                    .foregroundStyle(palette.accent)
            }
        }
        .frame(width: 30, height: 26)
        .accessibilityLabel("\(habit.name), \(isDone ? "done" : habit.isCounter ? "\(habit.count(on: day)) of \(habit.goal)" : "not done")")
    }
}

private struct LockedWidget: View {
    @Environment(\.palette) private var palette

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "lock.fill")
                .font(.title2)
                .foregroundStyle(palette.accent)
            Text("Widgets are part of Routine Pro")
                .font(.footnote.weight(.semibold))
                .multilineTextAlignment(.center)
            Text("Open Routine to unlock.")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}

private struct CircularAccessory: View {
    let entry: TodayEntry

    var body: some View {
        if entry.isUnlocked {
            Gauge(value: Double(entry.progress.done), in: 0...Double(max(entry.progress.total, 1))) {
                Image(systemName: "checkmark")
            } currentValueLabel: {
                Text("\(entry.progress.done)")
            }
            .gaugeStyle(.accessoryCircularCapacity)
        } else {
            ZStack {
                AccessoryWidgetBackground()
                Image(systemName: "lock.fill")
            }
        }
    }
}

private struct RectangularAccessory: View {
    let entry: TodayEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Routine").font(.headline)
            if entry.isUnlocked {
                Text(entry.progress.tier == .perfect ? "Everything done ★" : "\(entry.progress.done) of \(entry.progress.total) done today")
                Gauge(value: Double(entry.progress.done), in: 0...Double(max(entry.progress.total, 1))) { EmptyView() }
                    .gaugeStyle(.accessoryLinearCapacity)
            } else {
                Text("Unlock Routine Pro to use widgets")
            }
        }
    }
}

#Preview(as: .systemMedium) {
    TodayWidget()
} timeline: {
    TodayEntry.sample
}
