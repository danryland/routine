import SwiftUI

extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    static let routineBackground = Color(hex: 0x150107)
    static let routineMaroon = Color(hex: 0x4C0519)
    static let routineRose = Color(hex: 0xF43F5E)
    static let routineGreen = Color(hex: 0x059669)
    static let routineMint = Color(hex: 0x08E5A0)
    static let routineGold = Color(hex: 0xF4D849)
    static let routineYellow = Color(hex: 0xFFD500)
}

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
