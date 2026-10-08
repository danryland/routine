import Foundation
import Testing
@testable import RoutineCore

private func day(_ iso: String) -> Day { Day(iso: iso)! }

/// The repository's shared fixtures (also used by the Android tests).
private func fixture(_ name: String) throws -> Data {
    let repoRoot = URL(fileURLWithPath: #filePath)
        .deletingLastPathComponent() // RoutineCoreTests
        .deletingLastPathComponent() // Tests
        .deletingLastPathComponent() // ios
        .deletingLastPathComponent()
    return try Data(contentsOf: repoRoot.appendingPathComponent("fixtures/\(name)"))
}

struct CodingTests {
    @Test func readsTheSharedFormat() throws {
        let routine = try Routine.decode(from: fixture("routine-v1.json"))
        #expect(routine.startDate == day("2024-08-23"))
        #expect(routine.habits.map(\.name) == ["Teeth AM", "Water"])
        let water = routine.habits[1]
        #expect(water.id == UUID(uuidString: "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b"))
        #expect(water.goal == 6)
        #expect(water.createdOn == day("2024-08-25"))
        #expect(water.count(on: day("2024-08-26")) == 2)
        #expect(water.isDone(on: day("2024-08-28")))
    }

    @Test func writesTheSharedFormatByteForByte() throws {
        let data = try fixture("routine-v1.json")
        let reencoded = try Routine.decode(from: data).encoded()
        let normalized = { (d: Data) in String(decoding: d, as: UTF8.self).trimmingCharacters(in: .whitespacesAndNewlines) }
        #expect(normalized(reencoded) == normalized(data))
    }

    @Test func refusesNewerVersionsInsteadOfGuessing() {
        let json = #"{"version": 2, "startDate": "2024-08-23", "habits": []}"#
        #expect(throws: Routine.FormatError.unsupportedVersion(2)) { try Routine.decode(from: Data(json.utf8)) }
    }
}

struct LegacyImportTests {
    @Test func parsesTheOldDateFormat() {
        #expect(LegacyImport.parseDate("3 Sep 2024") == day("2024-09-03"))
        #expect(LegacyImport.parseDate("29 Feb 2024") == day("2024-02-29"))
        #expect(LegacyImport.parseDate("31 Feb 2024") == nil)
        #expect(LegacyImport.parseDate("2024-09-03") == nil)
    }

    @Test func importsEverythingTheOldAppCouldSave() throws {
        let json = String(decoding: try fixture("legacy-capacitor.json"), as: UTF8.self)
        let routine = try #require(LegacyImport.routine(fromJSON: json, today: day("2026-10-08")))
        #expect(routine.startDate == day("2024-08-23"))

        let habits = Dictionary(uniqueKeysWithValues: routine.habits.map { ($0.name, $0) })
        #expect(routine.habits.map(\.name) == ["Teeth AM", "SPF", "Water x6", "Lowered target", "Typed target", "Cleared target", "Garbage", ""])
        #expect(routine.habits.allSatisfy { $0.createdOn == day("2024-08-23") })

        #expect(habits["Teeth AM"]?.counts == [day("2024-08-26"): 1, day("2024-08-27"): 1])
        #expect(habits["SPF"]?.counts == [day("2024-08-28"): 1])
        #expect(habits["Water x6"]?.goal == 6)
        #expect(habits["Water x6"]?.counts == [day("2024-08-26"): 1, day("2024-08-27"): 2, day("2024-08-28"): 6])
        // Doing more than a lowered target counts as done (the old app said it wasn't).
        #expect(habits["Lowered target"]?.goal == 3)
        #expect(habits["Lowered target"]?.isDone(on: day("2024-08-27")) == true)
        #expect(habits["Typed target"]?.goal == 4)
        #expect(habits["Typed target"]?.count(on: day("2024-08-27")) == 2)
        #expect(habits["Cleared target"]?.goal == 1)
        #expect(habits["Cleared target"]?.counts == [day("2024-08-27"): 1])
        #expect(habits["Garbage"]?.counts.isEmpty == true)

        #expect(routine.progress(on: day("2024-08-28")) == DayProgress(done: 2, total: 8))
    }

    @Test func startsTodayWhenTheOldAppHadNothing() throws {
        let routine = try #require(LegacyImport.routine(fromJSON: "{}", today: day("2026-10-08")))
        #expect(routine == Routine(startDate: day("2026-10-08")))
    }

    @Test func ignoresThingsThatArentARoutine() {
        #expect(LegacyImport.routine(fromJSON: "", today: day("2026-10-08")) == nil)
        #expect(LegacyImport.routine(fromJSON: "[1, 2]", today: day("2026-10-08")) == nil)
    }
}

struct StoreTests {
    private func makeStore(legacy: String? = nil) throws -> RoutineStore {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        let defaults = try #require(UserDefaults(suiteName: "RoutineTests.\(UUID().uuidString)"))
        if let legacy { defaults.set(legacy, forKey: LegacyImport.userDefaultsKey) }
        return RoutineStore(fileURL: directory.appendingPathComponent("routine.json"), legacyDefaults: defaults)
    }

    @Test func firstLaunchStartsEmptyToday() throws {
        let store = try makeStore()
        #expect(store.load(today: day("2026-10-08")) == Routine(startDate: day("2026-10-08")))
    }

    @Test func savesAndLoads() throws {
        let store = try makeStore()
        var routine = Routine(startDate: day("2026-10-01"))
        let habit = routine.addHabit(named: "Stretch", goal: 2, on: day("2026-10-01"))
        routine.tap(habit.id, on: day("2026-10-02"))
        try store.save(routine)
        #expect(store.load(today: day("2026-10-08")) == routine)
    }

    @Test func migratesLegacyDataOnceAndPrefersTheNewFile() throws {
        let legacy = #"{"dates":["1 Oct 2026"],"tasks":[{"name":"Old","completed":["2 Oct 2026"]}]}"#
        let store = try makeStore(legacy: legacy)

        let migrated = store.load(today: day("2026-10-08"))
        #expect(migrated.habits.map(\.name) == ["Old"])
        #expect(FileManager.default.fileExists(atPath: store.fileURL.path))

        var edited = migrated
        edited.rename(edited.habits[0].id, to: "Renamed")
        try store.save(edited)
        #expect(store.load(today: day("2026-10-08")).habits.map(\.name) == ["Renamed"])
    }

    @Test func movesAnUnreadableFileAsideInsteadOfOverwritingIt() throws {
        let store = try makeStore()
        try FileManager.default.createDirectory(at: store.fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
        try Data("{ not json".utf8).write(to: store.fileURL)

        #expect(store.load(today: day("2026-10-08")) == Routine(startDate: day("2026-10-08")))
        let siblings = try FileManager.default.contentsOfDirectory(atPath: store.fileURL.deletingLastPathComponent().path)
        #expect(siblings.contains { $0.hasPrefix("routine.unreadable-") })
    }
}
