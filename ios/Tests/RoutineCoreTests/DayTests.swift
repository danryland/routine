import Foundation
import Testing
@testable import RoutineCore

struct DayTests {
    @Test func epochIsDayZero() {
        #expect(Day(year: 1970, month: 1, day: 1)?.ordinal == 0)
        #expect(Day(ordinal: 0).iso == "1970-01-01")
    }

    @Test func isoRoundTrips() throws {
        for text in ["2024-02-29", "1999-12-31", "2000-01-01", "2026-10-08"] {
            #expect(try #require(Day(iso: text)).iso == text)
        }
    }

    @Test func rejectsImpossibleDates() {
        #expect(Day(iso: "2023-02-29") == nil)
        #expect(Day(iso: "2024-13-01") == nil)
        #expect(Day(iso: "2024-4-01") == nil)
        #expect(Day(iso: "not a day") == nil)
        #expect(Day(year: 2100, month: 2, day: 29) == nil)
        #expect(Day(year: 2000, month: 2, day: 29) != nil)
    }

    @Test func arithmeticCrossesMonthsAndYears() throws {
        let newYearsEve = try #require(Day(iso: "2024-12-31"))
        #expect(newYearsEve.adding(days: 1).iso == "2025-01-01")
        #expect(try #require(Day(iso: "2024-03-01")).adding(days: -1).iso == "2024-02-29")
        #expect(newYearsEve.year == 2024 && newYearsEve.month == 12 && newYearsEve.day == 31)
    }

    @Test func containingUsesTheCalendarsTimeZone() throws {
        // 23:30 UTC on 23 Aug 2024 is already the 24th in London (BST) but still the 23rd in LA.
        let instant = Date(timeIntervalSince1970: 1_724_455_800)
        var london = Calendar(identifier: .gregorian)
        london.timeZone = try #require(TimeZone(identifier: "Europe/London"))
        var losAngeles = Calendar(identifier: .gregorian)
        losAngeles.timeZone = try #require(TimeZone(identifier: "America/Los_Angeles"))

        #expect(Day.containing(instant, calendar: london).iso == "2024-08-24")
        #expect(Day.containing(instant, calendar: losAngeles).iso == "2024-08-23")
    }

    @Test func startDateIsLocalMidnight() throws {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = try #require(TimeZone(identifier: "Europe/London"))
        let day = try #require(Day(iso: "2024-03-31")) // clocks go forward this day
        let start = day.startDate(calendar: calendar)
        #expect(Day.containing(start, calendar: calendar) == day)
        #expect(calendar.component(.hour, from: start) == 0)
    }

    @Test func codesAsAnIsoString() throws {
        let data = try JSONEncoder().encode([try #require(Day(iso: "2024-08-23"))])
        #expect(String(decoding: data, as: UTF8.self) == #"["2024-08-23"]"#)
        #expect(throws: DecodingError.self) { try JSONDecoder().decode([Day].self, from: Data(#"["23 Aug"]"#.utf8)) }
    }
}
