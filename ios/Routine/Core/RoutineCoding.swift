import Foundation

// The on-disk format is shared with the Android app, so a routine exported on
// one platform can be read on the other:
//
// {
//   "version": 1,
//   "startDate": "2024-08-23",
//   "habits": [
//     { "id": "…", "name": "Water", "goal": 6, "createdOn": "2024-08-23",
//       "counts": { "2024-08-26": 2, "2024-08-27": 6 } }
//   ]
// }

extension Habit: Codable {
    private enum CodingKeys: String, CodingKey {
        case id, name, goal, createdOn, counts
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let idText = try container.decode(String.self, forKey: .id)
        guard let id = UUID(uuidString: idText) else {
            throw DecodingError.dataCorruptedError(forKey: .id, in: container, debugDescription: "Invalid id \"\(idText)\"")
        }
        let rawCounts = try container.decodeIfPresent([String: Int].self, forKey: .counts) ?? [:]
        var counts: [Day: Int] = [:]
        for (key, value) in rawCounts {
            guard let day = Day(iso: key) else {
                throw DecodingError.dataCorruptedError(forKey: .counts, in: container, debugDescription: "Invalid day \"\(key)\"")
            }
            counts[day] = value
        }
        self.init(
            id: id,
            name: try container.decode(String.self, forKey: .name),
            goal: try container.decode(Int.self, forKey: .goal),
            createdOn: try container.decode(Day.self, forKey: .createdOn),
            counts: counts
        )
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id.uuidString.lowercased(), forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(goal, forKey: .goal)
        try container.encode(createdOn, forKey: .createdOn)
        try container.encode(
            Dictionary(uniqueKeysWithValues: counts.map { ($0.key.iso, $0.value) }),
            forKey: .counts
        )
    }
}

extension Routine: Codable {
    enum FormatError: Error, Equatable {
        case unsupportedVersion(Int)
    }

    private enum CodingKeys: String, CodingKey {
        case version, startDate, habits
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let version = try container.decode(Int.self, forKey: .version)
        guard version <= Routine.currentVersion else {
            throw FormatError.unsupportedVersion(version)
        }
        self.init(
            startDate: try container.decode(Day.self, forKey: .startDate),
            habits: try container.decode([Habit].self, forKey: .habits)
        )
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(Routine.currentVersion, forKey: .version)
        try container.encode(startDate, forKey: .startDate)
        try container.encode(habits, forKey: .habits)
    }

    static func decode(from data: Data) throws -> Routine {
        try JSONDecoder().decode(Routine.self, from: data)
    }

    func encoded() throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try encoder.encode(self)
    }
}
