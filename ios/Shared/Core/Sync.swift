import Foundation

/// Which copy of the routine wins when syncing between devices.
///
/// Whole-routine, last-change-wins, with one safeguard: a routine with no
/// habits never overwrites one that has some, so turning sync on for a new
/// device can't wipe the others.
enum SyncDecision: Equatable {
    case inSync
    case useRemote
    case pushLocal

    static func decide(local: Routine, localModified: Date, remote: Routine?, remoteModified: Date?) -> SyncDecision {
        guard let remote, let remoteModified else { return local.habits.isEmpty ? .inSync : .pushLocal }
        if remote == local { return .inSync }
        if local.habits.isEmpty { return .useRemote }
        if remote.habits.isEmpty { return .pushLocal }
        return remoteModified > localModified ? .useRemote : .pushLocal
    }
}
