import SwiftUI

enum Layout {
    static let cell: CGFloat = 64
    static let spacing: CGFloat = 8
    static let badgeHeight: CGFloat = 40
}

extension Font {
    /// Avenir Next Bold at a fixed size, for the grid where every cell is 64pt.
    static func routine(_ size: CGFloat) -> Font {
        .custom("AvenirNext-Bold", fixedSize: size)
    }

    /// Avenir Next Bold that follows the user's text size setting.
    static func routine(_ size: CGFloat, relativeTo style: Font.TextStyle) -> Font {
        .custom("AvenirNext-Bold", size: size, relativeTo: style)
    }
}

enum GoalText {
    static func short(_ goal: Int) -> String {
        goal == 1 ? "Once" : "\(goal)×"
    }

    static func long(_ goal: Int) -> String {
        goal == 1 ? "Once a day" : "\(goal) times a day"
    }
}

/// Shrinks slightly while pressed, like a physical key.
struct PressableButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.9 : 1)
            .animation(.snappy(duration: 0.15), value: configuration.isPressed)
    }
}

extension View {
    /// The themed page background and list styling used by every screen.
    func routineBackground(_ palette: Palette) -> some View {
        scrollContentBackground(.hidden)
            .background(palette.background)
    }
}
