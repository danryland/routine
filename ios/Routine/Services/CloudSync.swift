import Foundation

/// Keeps the routine in step across the user's iPhone and iPad through iCloud
/// key-value storage (up to 1 MB, which is decades of habits).
///
/// Whole-routine, last-change-wins: whichever device changed it most recently
/// supplies the copy everyone uses.
@MainActor
final class CloudSync {
    private enum Key {
        static let routine = "routine"
        static let modifiedAt = "modifiedAt"
    }

    private let store = NSUbiquitousKeyValueStore.default
    private var observer: NSObjectProtocol?

    /// Called when another device pushes a change.
    var onRemoteChange: (@MainActor () -> Void)?

    init() {
        observer = NotificationCenter.default.addObserver(
            forName: NSUbiquitousKeyValueStore.didChangeExternallyNotification,
            object: store,
            queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.onRemoteChange?() }
        }
        store.synchronize()
    }

    var remote: (data: Data, modifiedAt: Date)? {
        guard let data = store.data(forKey: Key.routine) else { return nil }
        return (data, Date(timeIntervalSince1970: store.double(forKey: Key.modifiedAt)))
    }

    func push(_ data: Data, modifiedAt: Date) {
        store.set(data, forKey: Key.routine)
        store.set(modifiedAt.timeIntervalSince1970, forKey: Key.modifiedAt)
        store.synchronize()
    }
}
