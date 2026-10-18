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

    // Colours that don't change with the theme.
    static let routineGreen = Color(hex: 0x059669)
    static let routineMint = Color(hex: 0x08E5A0)
    static let routineGold = Color(hex: 0xF4D849)
    static let routineYellow = Color(hex: 0xFFD500)
}

/// A colour theme. Rose is the original design; the rest come with the unlock.
struct Palette: Identifiable, Equatable {
    let id: String
    let name: String
    /// Page background.
    let background: Color
    /// Empty cells, rings' tracks, dividers.
    let track: Color
    /// Done cells, highlights, buttons.
    let accent: Color
    /// Cards and rows that sit on the background.
    let raised: Color

    static let rose = Palette(id: "rose", name: "Rose", background: Color(hex: 0x150107), track: Color(hex: 0x4C0519), accent: Color(hex: 0xF43F5E), raised: Color(hex: 0x2A0610))
    static let ocean = Palette(id: "ocean", name: "Ocean", background: Color(hex: 0x030C15), track: Color(hex: 0x0C2A4A), accent: Color(hex: 0x38BDF8), raised: Color(hex: 0x071A2D))
    static let forest = Palette(id: "forest", name: "Forest", background: Color(hex: 0x03100A), track: Color(hex: 0x0B3B24), accent: Color(hex: 0xA3E635), raised: Color(hex: 0x062415))
    static let amber = Palette(id: "amber", name: "Amber", background: Color(hex: 0x150C02), track: Color(hex: 0x4A2A06), accent: Color(hex: 0xF59E0B), raised: Color(hex: 0x2B1803))
    static let violet = Palette(id: "violet", name: "Violet", background: Color(hex: 0x10061C), track: Color(hex: 0x3B1466), accent: Color(hex: 0xA78BFA), raised: Color(hex: 0x220C3B))

    static let all: [Palette] = [.rose, .ocean, .forest, .amber, .violet]

    /// The saved theme, or Rose if it's locked or unknown.
    static func named(_ id: String?, isUnlocked: Bool) -> Palette {
        guard isUnlocked, let match = all.first(where: { $0.id == id }) else { return .rose }
        return match
    }
}

private struct PaletteKey: EnvironmentKey {
    static let defaultValue = Palette.rose
}

extension EnvironmentValues {
    var palette: Palette {
        get { self[PaletteKey.self] }
        set { self[PaletteKey.self] = newValue }
    }
}
