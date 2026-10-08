import SwiftUI

@main
struct RoutineApp: App {
    @State private var model = RoutineModel.live()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(model)
                .preferredColorScheme(.dark)
                .tint(.routineRose)
        }
    }
}
