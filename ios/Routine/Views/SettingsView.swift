import StoreKit
import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @Environment(RoutineModel.self) private var model
    @Environment(PurchaseStore.self) private var purchases
    @Environment(\.palette) private var palette
    @Environment(\.dismiss) private var dismiss
    @AppStorage(SettingsKey.theme, store: AppGroup.defaults) private var themeID = Palette.rose.id

    @State private var lockedFeature: LockedFeature?
    @State private var export: ExportFile?
    @State private var isExporting = false
    @State private var isImporting = false
    @State private var pendingRestore: Data?
    @State private var notice: String?

    var body: some View {
        NavigationStack {
            Form {
                proSection
                themeSection

                Section {
                    Toggle("Daily reminder", isOn: remindersBinding)
                        .accessibilityIdentifier("remindersToggle")
                    if model.remindersOn && purchases.isUnlocked {
                        DatePicker("Time", selection: reminderTimeBinding, displayedComponents: .hourAndMinute)
                    }
                } header: {
                    Text("Reminders")
                } footer: {
                    Text("Skipped on days you’ve already done everything.")
                }
                .listRowBackground(palette.raised)

                Section {
                    Toggle("iCloud sync", isOn: syncBinding)
                } header: {
                    Text("Sync")
                } footer: {
                    Text("Keeps your routine the same on your iPhone and iPad. If both change, the most recent change wins.")
                }
                .listRowBackground(palette.raised)

                Section {
                    Button("Export as CSV") { exportCSV() }
                    Button("Back up to a file") { exportBackup() }
                    Button("Restore from a backup") { isImporting = true }
                } header: {
                    Text("Your data")
                } footer: {
                    Text("A backup can be restored on any iPhone, iPad or Android phone running Routine.")
                }
                .listRowBackground(palette.raised)

                tipSection

                Section {
                    LabeledContent("Version", value: Self.appVersion)
                }
                .listRowBackground(palette.raised)
            }
            .routineBackground(palette)
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .sheet(item: $lockedFeature) { feature in
                PaywallView(reason: .feature(feature.name))
                    .environment(purchases)
                    .environment(\.palette, palette)
                    .tint(palette.accent)
            }
            .fileExporter(isPresented: $isExporting, document: export, contentType: export?.contentType ?? .json, defaultFilename: export?.filename) { result in
                if case .failure = result {
                    notice = "Couldn’t save the file."
                }
            }
            .fileImporter(isPresented: $isImporting, allowedContentTypes: [.json, .plainText]) { result in
                guard case .success(let url) = result else { return }
                let accessing = url.startAccessingSecurityScopedResource()
                defer { if accessing { url.stopAccessingSecurityScopedResource() } }
                if let data = try? Data(contentsOf: url) {
                    pendingRestore = data
                } else {
                    notice = "Couldn’t open that file."
                }
            }
            .confirmationDialog(
                "Replace your routine with this backup?",
                isPresented: Binding(get: { pendingRestore != nil }, set: { if !$0 { pendingRestore = nil } }),
                titleVisibility: .visible
            ) {
                Button("Replace", role: .destructive) {
                    if let data = pendingRestore, !model.restore(from: data) {
                        notice = "That file isn’t a Routine backup."
                    }
                    pendingRestore = nil
                }
            } message: {
                Text("Everything currently in Routine will be replaced.")
            }
            .alert(notice ?? "", isPresented: Binding(get: { notice != nil }, set: { if !$0 { notice = nil } })) {
                Button("OK", role: .cancel) {}
            }
        }
    }

    // MARK: - Sections

    private var proSection: some View {
        Section {
            if purchases.isUnlocked {
                Label(purchases.isEarlySupporter ? "Unlocked free for early supporters" : "Routine Pro is unlocked", systemImage: "checkmark.seal.fill")
                    .foregroundStyle(palette.accent)
            } else {
                Button {
                    lockedFeature = LockedFeature(name: "Everything")
                } label: {
                    Label("Unlock Routine Pro", systemImage: "star.fill")
                }
                .accessibilityIdentifier("openPaywall")
                Button("Restore purchase") {
                    Task { await purchases.restore() }
                }
            }
        } header: {
            Text("Routine Pro")
        }
        .listRowBackground(palette.raised)
    }

    private var themeSection: some View {
        Section("Theme") {
            HStack(spacing: 14) {
                ForEach(Palette.all) { option in
                    let isLocked = option != .rose && !purchases.isUnlocked
                    Button {
                        if isLocked {
                            lockedFeature = LockedFeature(name: "Colour themes")
                        } else {
                            themeID = option.id
                        }
                    } label: {
                        ZStack {
                            Circle().fill(option.background)
                            Circle().fill(option.accent).padding(9)
                            if isLocked {
                                Image(systemName: "lock.fill")
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(.white)
                            }
                        }
                        .frame(width: 44, height: 44)
                        .overlay(Circle().strokeBorder(palette.id == option.id ? Color.white : option.track, lineWidth: 2))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(option.name)
                    .accessibilityValue(palette.id == option.id ? "Selected" : isLocked ? "Locked" : "")
                }
            }
            .padding(.vertical, 4)
        }
        .listRowBackground(palette.raised)
    }

    private var tipSection: some View {
        Section {
            if purchases.tips.isEmpty {
                Text("Tips aren’t available right now.")
                    .foregroundStyle(.secondary)
            }
            ForEach(purchases.tips, id: \.id) { tip in
                Button {
                    Task {
                        if await purchases.purchase(tip) == .purchased {
                            notice = "Thank you so much! ♥"
                        }
                    }
                } label: {
                    HStack {
                        Text(tip.displayName)
                        Spacer()
                        Text(tip.displayPrice).foregroundStyle(.secondary)
                    }
                }
                .disabled(purchases.isBusy)
            }
        } header: {
            Text("Tip jar")
        } footer: {
            Text("Tips are optional and don’t unlock anything. They just say thanks.")
        }
        .listRowBackground(palette.raised)
    }

    // MARK: - Bindings

    private var remindersBinding: Binding<Bool> {
        Binding(
            get: { model.remindersOn && purchases.isUnlocked },
            set: { isOn in
                guard purchases.isUnlocked else {
                    lockedFeature = LockedFeature(name: "Daily reminders")
                    return
                }
                guard isOn else {
                    model.remindersOn = false
                    return
                }
                Task {
                    if await Reminders.requestPermission() {
                        model.remindersOn = true
                    } else {
                        notice = "To get reminders, allow notifications for Routine in the Settings app."
                    }
                }
            }
        )
    }

    private var reminderTimeBinding: Binding<Date> {
        Binding(
            get: {
                Calendar.current.date(from: DateComponents(hour: model.reminderMinute / 60, minute: model.reminderMinute % 60)) ?? Date()
            },
            set: { date in
                let parts = Calendar.current.dateComponents([.hour, .minute], from: date)
                model.reminderMinute = (parts.hour ?? 20) * 60 + (parts.minute ?? 0)
            }
        )
    }

    private var syncBinding: Binding<Bool> {
        Binding(
            get: { model.iCloudSyncOn && purchases.isUnlocked },
            set: { isOn in
                if purchases.isUnlocked {
                    model.iCloudSyncOn = isOn
                } else {
                    lockedFeature = LockedFeature(name: "iCloud sync")
                }
            }
        )
    }

    // MARK: - Files

    private func exportCSV() {
        guard purchases.isUnlocked else {
            lockedFeature = LockedFeature(name: "CSV export")
            return
        }
        export = ExportFile(data: Data(model.csvExport().utf8), contentType: .commaSeparatedText, filename: "Routine \(model.today.iso).csv")
        isExporting = true
    }

    private func exportBackup() {
        guard let data = try? model.backupData() else {
            notice = "Couldn’t create the backup."
            return
        }
        export = ExportFile(data: data, contentType: .json, filename: "Routine backup \(model.today.iso).json")
        isExporting = true
    }

    private static var appVersion: String {
        let info = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String ?? "?"
        let build = info?["CFBundleVersion"] as? String ?? "?"
        return "\(version) (\(build))"
    }
}

private struct LockedFeature: Identifiable {
    let name: String
    var id: String { name }
}

/// A file handed to the system's save dialog.
struct ExportFile: FileDocument {
    static var readableContentTypes: [UTType] { [.json, .commaSeparatedText] }

    var data: Data
    var contentType: UTType = .json
    var filename: String = "Routine"

    init(data: Data, contentType: UTType, filename: String) {
        self.data = data
        self.contentType = contentType
        self.filename = filename
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

#Preview {
    SettingsView()
        .environment(RoutineModel.preview)
        .environment(PurchaseStore())
        .preferredColorScheme(.dark)
}
