import Foundation
import Observation
import StoreKit

/// The one-time unlock and the tip jar, via StoreKit 2.
///
/// Product IDs must match App Store Connect (see docs/launch-checklist.md).
@MainActor
@Observable
final class PurchaseStore {
    static let unlockID = "wales.ryland.routine.unlock"
    static let tipIDs = ["wales.ryland.routine.tip.small", "wales.ryland.routine.tip.medium", "wales.ryland.routine.tip.large"]

    /// Builds up to and including this number were the Capacitor app. Anyone
    /// whose first download was one of them is an early supporter.
    static let lastCapacitorBuild = 1

    enum Outcome: Equatable {
        case purchased, pending, cancelled, failed
    }

    private(set) var unlockProduct: Product?
    private(set) var tips: [Product] = []
    private(set) var hasPurchasedUnlock = false
    private(set) var isEarlySupporter: Bool
    private(set) var isBusy = false
    /// Products couldn't be loaded (offline, or not set up in App Store Connect yet).
    private(set) var storeUnavailable = false

    @ObservationIgnored private let defaults: UserDefaults
    @ObservationIgnored private let forceUnlocked: Bool
    @ObservationIgnored private var updates: Task<Void, Never>?

    var isUnlocked: Bool {
        forceUnlocked || hasPurchasedUnlock || isEarlySupporter
    }

    init(defaults: UserDefaults = AppGroup.defaults, forceUnlocked: Bool = false) {
        self.defaults = defaults
        self.forceUnlocked = forceUnlocked
        let earlySupporter = defaults.bool(forKey: SettingsKey.earlySupporter)
        self.isEarlySupporter = earlySupporter
        // Trust the last known state until StoreKit answers, so Pro features
        // don't flicker off at launch or when offline.
        self.hasPurchasedUnlock = defaults.bool(forKey: SettingsKey.isUnlocked) && !earlySupporter
    }

    func start() {
        guard updates == nil else { return }
        // Purchases made on other devices, Ask to Buy approvals, refunds.
        updates = Task { [weak self] in
            for await result in Transaction.updates {
                await self?.handle(result)
            }
        }
        Task {
            await loadProducts()
            await refreshEntitlements()
            await checkFirstDownload()
        }
    }

    /// Called when the routine was imported from the original app on this device.
    func markEarlySupporter() {
        isEarlySupporter = true
        defaults.set(true, forKey: SettingsKey.earlySupporter)
        cacheUnlockState()
    }

    func loadProducts() async {
        do {
            let products = try await Product.products(for: [Self.unlockID] + Self.tipIDs)
            unlockProduct = products.first { $0.id == Self.unlockID }
            tips = products.filter { Self.tipIDs.contains($0.id) }.sorted { $0.price < $1.price }
            storeUnavailable = unlockProduct == nil
        } catch {
            storeUnavailable = true
        }
    }

    func refreshEntitlements() async {
        var owned = false
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result,
               transaction.productID == Self.unlockID,
               transaction.revocationDate == nil {
                owned = true
            }
        }
        hasPurchasedUnlock = owned
        cacheUnlockState()
    }

    /// Unlocks people who first downloaded the Capacitor version, on any device,
    /// even after reinstalling. (Sandbox and TestFlight always report "1.0", so
    /// this only runs in production.)
    func checkFirstDownload() async {
        guard let result = try? await AppTransaction.shared,
              case .verified(let app) = result,
              app.environment == .production,
              let firstBuild = Int(app.originalAppVersion),
              firstBuild <= Self.lastCapacitorBuild
        else { return }
        markEarlySupporter()
    }

    func purchase(_ product: Product) async -> Outcome {
        isBusy = true
        defer { isBusy = false }
        do {
            switch try await product.purchase() {
            case .success(let verification):
                guard case .verified(let transaction) = verification else { return .failed }
                await transaction.finish()
                await refreshEntitlements()
                return .purchased
            case .pending:
                return .pending
            case .userCancelled:
                return .cancelled
            @unknown default:
                return .failed
            }
        } catch {
            return .failed
        }
    }

    func restore() async {
        isBusy = true
        defer { isBusy = false }
        try? await AppStore.sync()
        await refreshEntitlements()
    }

    private func handle(_ result: VerificationResult<Transaction>) async {
        guard case .verified(let transaction) = result else { return }
        await transaction.finish()
        await refreshEntitlements()
    }

    private func cacheUnlockState() {
        defaults.set(isUnlocked, forKey: SettingsKey.isUnlocked)
    }
}
