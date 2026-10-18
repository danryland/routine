import Foundation

/// Reads the routine saved by the original Vue/Capacitor app so people who
/// update keep their history.
///
/// That app stored one JSON string via `@capacitor/preferences`, which on iOS
/// lands in `UserDefaults.standard` under `"CapacitorStorage.routine"`:
///
///     { "dates": ["23 Aug 2024", …],
///       "tasks": [ { "name": "SPF", "completed": ["28 Aug 2024"] },
///                  { "name": "Water", "target": 6,
///                    "completed": [ { "date": "26 Aug 2024", "times": 1 } ] } ] }
enum LegacyImport {
    static let userDefaultsKey = "CapacitorStorage.routine"

    private static let months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]

    /// Returns nil if `json` isn't a routine the old app could have written.
    static func routine(fromJSON json: String, today: Day) -> Routine? {
        guard let data = json.data(using: .utf8),
              let root = try? JSONDecoder().decode(JSONValue.self, from: data),
              case .object(let fields) = root
        else { return nil }

        let dates = fields["dates"]?.arrayValue?.compactMap { $0.stringValue.flatMap(parseDate) } ?? []
        let tasks = fields["tasks"]?.arrayValue ?? []

        var habits: [(name: String, goal: Int, counts: [Day: Int])] = []
        for task in tasks {
            guard case .object(let task) = task else { continue }
            let goal = task["target"]?.goalValue ?? 1
            var counts: [Day: Int] = [:]
            for entry in task["completed"]?.arrayValue ?? [] {
                guard let completion = completion(from: entry, goal: goal), completion.times > 0 else { continue }
                counts[completion.day] = max(counts[completion.day] ?? 0, completion.times)
            }
            habits.append((task["name"]?.stringValue ?? "", goal, counts))
        }

        let earliestCompletion = habits.flatMap(\.counts.keys).min()
        let startDate = [dates.min(), earliestCompletion].compactMap { $0 }.min() ?? today

        return Routine(
            startDate: startDate,
            habits: habits.map { Habit(name: $0.name, goal: $0.goal, createdOn: startDate, counts: $0.counts) }
        )
    }

    /// Parses the old app's "D MMM YYYY" dates, e.g. "3 Sep 2024".
    static func parseDate(_ text: String) -> Day? {
        let parts = text.split(separator: " ")
        guard parts.count == 3,
              let day = Int(parts[0]),
              let month = months.firstIndex(where: { $0.caseInsensitiveCompare(parts[1]) == .orderedSame }),
              let year = Int(parts[2])
        else { return nil }
        return Day(year: year, month: month + 1, day: day)
    }

    private static func completion(from entry: JSONValue, goal: Int) -> (day: Day, times: Int)? {
        switch entry {
        case .string(let text):
            // Tick-box tasks stored bare dates.
            return parseDate(text).map { ($0, goal) }
        case .object(let fields):
            // Counter tasks stored { date, times }. If the target was later
            // cleared, the old app showed it as a tick box; treat any taps as done.
            guard let day = fields["date"]?.stringValue.flatMap(parseDate),
                  let times = fields["times"]?.intValue
            else { return nil }
            return (day, goal == 1 ? min(times, 1) : times)
        default:
            return nil
        }
    }
}

/// Just enough of a JSON model to read loosely-typed legacy data.
enum JSONValue: Decodable, Equatable {
    case string(String), number(Double), bool(Bool), object([String: JSONValue]), array([JSONValue]), null

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([JSONValue].self) {
            self = .array(value)
        } else {
            self = .object(try container.decode([String: JSONValue].self))
        }
    }

    var stringValue: String? {
        if case .string(let value) = self { return value }
        return nil
    }

    var arrayValue: [JSONValue]? {
        if case .array(let value) = self { return value }
        return nil
    }

    var intValue: Int? {
        switch self {
        case .number(let value) where value.isFinite:
            return Int(value.rounded())
        case .string(let value):
            return Double(value.trimmingCharacters(in: .whitespaces)).flatMap { $0.isFinite ? Int($0.rounded()) : nil }
        default:
            return nil
        }
    }

    /// The old app treated a missing, empty, zero or one `target` as a tick box.
    var goalValue: Int? {
        guard let target = intValue, target >= 2 else { return nil }
        return Habit.clampGoal(target)
    }
}
