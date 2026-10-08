// swift-tools-version: 6.0

// Builds the app's platform-independent logic (Routine/Core) on its own so it
// can be unit-tested anywhere Swift runs, including Linux CI:
//
//     cd ios && swift test
//
// The app itself is built from Routine.xcodeproj, which compiles the same files.
import PackageDescription

let package = Package(
    name: "RoutineCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    targets: [
        .target(name: "RoutineCore", path: "Routine/Core"),
        .testTarget(name: "RoutineCoreTests", dependencies: ["RoutineCore"], path: "Tests/RoutineCoreTests"),
    ]
)
