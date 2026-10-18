import StoreKit
import SwiftUI

/// Sells the one-time unlock.
struct PaywallView: View {
    enum Reason: Equatable {
        case habitLimit
        case feature(String)
    }

    let reason: Reason
    @Environment(PurchaseStore.self) private var purchases
    @Environment(\.palette) private var palette
    @Environment(\.dismiss) private var dismiss
    @State private var message: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    Image(systemName: "star.fill")
                        .font(.system(size: 56))
                        .foregroundStyle(LinearGradient(colors: [.routineGold, .routineYellow], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .padding(.top, 8)

                    VStack(spacing: 8) {
                        Text("Routine Pro")
                            .font(.routine(30, relativeTo: .largeTitle))
                        Text(headline)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                    }

                    VStack(alignment: .leading, spacing: 16) {
                        Feature(icon: "infinity", title: "Unlimited habits", detail: "Track everything, not just \(FreeTier.habitLimit) things.")
                        Feature(icon: "square.grid.2x2", title: "Widgets", detail: "Tick habits off from your Home and Lock Screen.")
                        Feature(icon: "bell.badge", title: "Daily reminders", detail: "A nudge at your chosen time, skipped once you’re done.")
                        Feature(icon: "icloud", title: "iCloud sync", detail: "The same routine on your iPhone and iPad.")
                        Feature(icon: "flame", title: "Streaks and stats", detail: "Your best runs, monthly calendar and success rates.")
                        Feature(icon: "paintpalette", title: "Colour themes", detail: "Ocean, Forest, Amber and Violet.")
                        Feature(icon: "tablecells", title: "CSV export", detail: "Your whole history, ready for a spreadsheet.")
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(palette.raised))

                    if purchases.isUnlocked {
                        Label("Unlocked. Thank you!", systemImage: "checkmark.seal.fill")
                            .font(.headline)
                            .foregroundStyle(palette.accent)
                    } else {
                        VStack(spacing: 12) {
                            Button {
                                buy()
                            } label: {
                                Text(buyTitle)
                                    .font(.headline)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 6)
                            }
                            .buttonStyle(.borderedProminent)
                            .controlSize(.large)
                            .disabled(purchases.unlockProduct == nil || purchases.isBusy)
                            .accessibilityIdentifier("buyUnlock")

                            Text("One payment, yours for good. No subscription. Shared with your family.")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)

                            Button("Restore purchase") {
                                Task { await purchases.restore() }
                            }
                            .font(.footnote)
                            .disabled(purchases.isBusy)

                            if purchases.storeUnavailable {
                                Text("The App Store isn’t reachable right now. Please try again later.")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                                    .multilineTextAlignment(.center)
                            }
                        }
                    }

                    if let message {
                        Text(message)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                }
                .padding(24)
                .frame(maxWidth: 520)
                .frame(maxWidth: .infinity)
            }
            .routineBackground(palette)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(purchases.isUnlocked ? "Done" : "Not now") { dismiss() }
                }
            }
        }
        .accessibilityIdentifier("paywall")
    }

    private var headline: String {
        switch reason {
        case .habitLimit:
            return "The free version tracks up to \(FreeTier.habitLimit) habits. Unlock Routine once to add as many as you like, plus everything below."
        case .feature(let name):
            return "\(name) is part of Routine Pro. Unlock it once and it’s yours for good."
        }
    }

    private var buyTitle: String {
        purchases.unlockProduct.map { "Unlock for \($0.displayPrice)" } ?? "Unlock"
    }

    private func buy() {
        guard let product = purchases.unlockProduct else { return }
        Task {
            switch await purchases.purchase(product) {
            case .purchased:
                message = nil
            case .pending:
                message = "Waiting for approval. Routine will unlock as soon as it’s approved."
            case .cancelled:
                break
            case .failed:
                message = "That didn’t go through. You haven’t been charged."
            }
        }
    }
}

private struct Feature: View {
    @Environment(\.palette) private var palette
    let icon: String
    let title: String
    let detail: String

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(palette.accent)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text(detail).font(.subheadline).foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

#Preview {
    PaywallView(reason: .habitLimit)
        .environment(PurchaseStore())
        .preferredColorScheme(.dark)
}
