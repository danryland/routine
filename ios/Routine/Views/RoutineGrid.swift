import SwiftUI

/// Habit names pinned on the left, one column per day scrolling sideways,
/// opening on today.
struct RoutineGrid: View {
    @Environment(\.palette) private var palette
    @Environment(RoutineModel.self) private var model

    var body: some View {
        let habits = model.routine.habits
        GeometryReader { proxy in
            ScrollView(.vertical) {
                HStack(alignment: .top, spacing: 0) {
                    NameColumn(habits: habits)
                        .frame(width: min(200, proxy.size.width * 0.4))

                    ScrollView(.horizontal) {
                        LazyHStack(alignment: .top, spacing: Layout.spacing) {
                            ForEach(model.days, id: \.self) { day in
                                DayColumn(
                                    day: day,
                                    habits: habits,
                                    progress: model.routine.progress(on: day),
                                    isToday: day == model.today
                                ) { habitID in
                                    model.tap(habitID, on: day)
                                }
                            }
                        }
                        .padding(.horizontal, 12)
                    }
                    .scrollIndicators(.hidden)
                    .defaultScrollAnchor(.trailing)
                    .overlay(alignment: .leading) {
                        // Cells fade out as they slide under the names.
                        LinearGradient(colors: [palette.background, palette.background.opacity(0)], startPoint: .leading, endPoint: .trailing)
                            .frame(width: 12)
                            .allowsHitTesting(false)
                    }
                }
                .padding(.top, Layout.spacing)
                .padding(.bottom, Layout.cell)
            }
        }
        .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
    }
}

private struct NameColumn: View {
    let habits: [Habit]

    var body: some View {
        VStack(alignment: .trailing, spacing: Layout.spacing) {
            Color.clear
                .frame(height: Layout.badgeHeight)

            Text("Date")
                .font(.routine(16))
                .foregroundStyle(.white.opacity(0.5))
                .frame(maxWidth: .infinity, alignment: .trailing)
                .frame(height: Layout.cell)
                .accessibilityHidden(true)

            ForEach(habits) { habit in
                Text(habit.name.isEmpty ? "Untitled" : habit.name)
                    .font(.routine(16, relativeTo: .body))
                    .foregroundStyle(.white.opacity(habit.name.isEmpty ? 0.4 : 1))
                    .multilineTextAlignment(.trailing)
                    .lineLimit(2)
                    .minimumScaleFactor(0.75)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .frame(height: Layout.cell)
                    // Each cell already says which habit it is.
                    .accessibilityHidden(true)
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, 8)
    }
}

private struct DayColumn: View {
    let day: Day
    let habits: [Habit]
    let progress: DayProgress
    let isToday: Bool
    let tap: (Habit.ID) -> Void

    var body: some View {
        VStack(spacing: Layout.spacing) {
            ProgressBadge(progress: progress)
                .frame(height: Layout.badgeHeight)

            DayHeader(day: day, isToday: isToday)

            ForEach(habits) { habit in
                HabitCell(habit: habit, day: day) {
                    tap(habit.id)
                }
            }
        }
        .frame(width: Layout.cell)
        .sensoryFeedback(.success, trigger: progress.tier == .perfect) { wasPerfect, isPerfect in
            isPerfect && !wasPerfect
        }
    }
}

private struct DayHeader: View {
    let day: Day
    let isToday: Bool

    var body: some View {
        let date = day.startDate()
        VStack(spacing: 0) {
            Text(date, format: .dateTime.weekday(.abbreviated))
            Text(date, format: .dateTime.day())
        }
        .font(.routine(16))
        .foregroundStyle(.white.opacity(isToday ? 1 : 0.5))
        .frame(width: Layout.cell, height: Layout.cell)
        .background {
            if isToday {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(.white.opacity(0.07))
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(date, format: .dateTime.weekday(.wide).day().month(.wide)))
        .accessibilityAddTraits(.isHeader)
    }
}

private struct ProgressBadge: View {
    @Environment(\.palette) private var palette
    let progress: DayProgress

    var body: some View {
        Group {
            if progress.tier == .perfect {
                Image(systemName: "star.fill")
                    .font(.system(size: 26))
                    .foregroundStyle(LinearGradient(colors: [.routineGold, .routineYellow], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .transition(.scale(scale: 0.4).combined(with: .opacity))
                    .accessibilityLabel("Everything done")
            } else {
                Text("\(progress.percent)%")
                    .font(.routine(16))
                    .foregroundStyle(color)
                    .accessibilityLabel("\(progress.percent) percent done")
            }
        }
        .animation(.spring(duration: 0.35, bounce: 0.5), value: progress.tier == .perfect)
    }

    private var color: Color {
        switch progress.tier {
        case .nearlyThere: Color.routineMint
        case .high: Color.routineGreen
        case .medium: palette.accent.opacity(0.8)
        case .low: palette.accent.opacity(0.6)
        case .none, .nothingToDo, .perfect: Color.white.opacity(0.4)
        }
    }
}

struct HabitCell: View {
    @Environment(\.palette) private var palette
    let habit: Habit
    let day: Day
    let action: () -> Void

    private var count: Int { habit.count(on: day) }
    private var isDone: Bool { habit.isDone(on: day) }

    var body: some View {
        Button(action: action) {
            ZStack {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(isDone ? palette.accent : Color.clear)
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .strokeBorder(isDone ? palette.accent : palette.track, lineWidth: 2)

                if isDone || !habit.isCounter {
                    Image(systemName: "checkmark")
                        .font(.system(size: 26, weight: .black))
                        .foregroundStyle(isDone ? Color.white : palette.track)
                } else {
                    CounterRing(count: count, goal: habit.goal)
                }
            }
            .frame(width: Layout.cell, height: Layout.cell)
            // Days before a habit existed are dimmed (but can still be filled in).
            .opacity(habit.isTracked(on: day) ? 1 : 0.4)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressableButtonStyle())
        .animation(.snappy(duration: 0.2), value: count)
        .sensoryFeedback(.selection, trigger: count)
        .accessibilityLabel(Text(verbatim: "\(habit.name), ") + Text(day.startDate(), format: .dateTime.weekday(.wide).day().month(.wide)))
        .accessibilityValue(accessibilityValue)
    }

    private var accessibilityValue: String {
        if isDone { return "Done" }
        return habit.isCounter ? "\(count) of \(habit.goal)" : "Not done"
    }
}

private struct CounterRing: View {
    @Environment(\.palette) private var palette
    let count: Int
    let goal: Int

    var body: some View {
        ZStack {
            Circle()
                .stroke(palette.track, lineWidth: 5)
            Circle()
                .trim(from: 0, to: CGFloat(count) / CGFloat(goal))
                .stroke(palette.accent, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Text("\(count)")
                .font(.routine(18))
                .monospacedDigit()
                .foregroundStyle(palette.accent)
        }
        .padding(11)
    }
}

#Preview {
    RoutineGrid()
        .environment(RoutineModel.preview)
        .background(Palette.rose.background)
        .preferredColorScheme(.dark)
}
