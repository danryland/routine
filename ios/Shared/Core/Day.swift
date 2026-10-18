import Foundation

/// A calendar day with no time or time zone attached.
///
/// Stored as the number of days since 1970-01-01 so comparisons and
/// arithmetic never drift across daylight-saving changes or time zones.
struct Day: Hashable, Comparable, Sendable {
    let ordinal: Int

    init(ordinal: Int) {
        self.ordinal = ordinal
    }

    init?(year: Int, month: Int, day: Int) {
        guard (1...12).contains(month), day >= 1, day <= Day.daysIn(month: month, year: year) else {
            return nil
        }
        self.ordinal = Day.daysFromCivil(year: year, month: month, day: day)
    }

    var year: Int { Day.civilFromDays(ordinal).year }
    var month: Int { Day.civilFromDays(ordinal).month }
    var day: Int { Day.civilFromDays(ordinal).day }

    func adding(days: Int) -> Day {
        Day(ordinal: ordinal + days)
    }

    static func < (lhs: Day, rhs: Day) -> Bool {
        lhs.ordinal < rhs.ordinal
    }

    // MARK: - Calendar bridging

    /// The day `date` falls on in `calendar`'s time zone.
    static func containing(_ date: Date, calendar: Calendar = .current) -> Day {
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return Day(ordinal: daysFromCivil(year: parts.year!, month: parts.month!, day: parts.day!))
    }

    static func today(calendar: Calendar = .current) -> Day {
        containing(Date(), calendar: calendar)
    }

    /// Midnight at the start of this day in `calendar`'s time zone.
    func startDate(calendar: Calendar = .current) -> Date {
        let parts = Day.civilFromDays(ordinal)
        return calendar.date(from: DateComponents(year: parts.year, month: parts.month, day: parts.day))!
    }

    // MARK: - ISO 8601 ("2024-08-23")

    init?(iso: String) {
        let parts = iso.split(separator: "-", omittingEmptySubsequences: false)
        guard parts.count == 3,
              parts[0].count == 4, parts[1].count == 2, parts[2].count == 2,
              let year = Int(parts[0]), let month = Int(parts[1]), let day = Int(parts[2])
        else { return nil }
        self.init(year: year, month: month, day: day)
    }

    var iso: String {
        let parts = Day.civilFromDays(ordinal)
        let year = String(parts.year)
        let month = parts.month < 10 ? "0\(parts.month)" : "\(parts.month)"
        let day = parts.day < 10 ? "0\(parts.day)" : "\(parts.day)"
        return "\(String(repeating: "0", count: max(0, 4 - year.count)))\(year)-\(month)-\(day)"
    }

    // MARK: - Proleptic Gregorian conversions (Howard Hinnant's algorithms)

    private static func daysFromCivil(year: Int, month: Int, day: Int) -> Int {
        let y = month <= 2 ? year - 1 : year
        let era = (y >= 0 ? y : y - 399) / 400
        let yearOfEra = y - era * 400
        let dayOfYear = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1
        let dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146_097 + dayOfEra - 719_468
    }

    private static func civilFromDays(_ days: Int) -> (year: Int, month: Int, day: Int) {
        let z = days + 719_468
        let era = (z >= 0 ? z : z - 146_096) / 146_097
        let dayOfEra = z - era * 146_097
        let yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146_096) / 365
        let dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        let mp = (5 * dayOfYear + 2) / 153
        let day = dayOfYear - (153 * mp + 2) / 5 + 1
        let month = mp < 10 ? mp + 3 : mp - 9
        return (yearOfEra + era * 400 + (month <= 2 ? 1 : 0), month, day)
    }

    private static func daysIn(month: Int, year: Int) -> Int {
        switch month {
        case 2:
            let leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
            return leap ? 29 : 28
        case 4, 6, 9, 11:
            return 30
        default:
            return 31
        }
    }
}

extension Day: CustomStringConvertible {
    var description: String { iso }
}

extension Day: Codable {
    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let text = try container.decode(String.self)
        guard let day = Day(iso: text) else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid day \"\(text)\"")
        }
        self = day
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(iso)
    }
}
