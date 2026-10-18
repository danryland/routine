import XCTest

/// Drives the real app in the simulator. Set SCREENSHOT_DIR (via
/// TEST_RUNNER_SCREENSHOT_DIR on the xcodebuild command line) to save
/// screenshots of each screen.
final class RoutineUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    func testAddingAndTickingAHabit() {
        let app = launch()
        addHabit("Drink water", in: app, first: true)

        let cell = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'Drink water'")).firstMatch
        XCTAssert(cell.waitForExistence(timeout: 5))
        XCTAssertEqual(cell.value as? String, "Not done")
        cell.tap()
        XCTAssertEqual(cell.value as? String, "Done")
        screenshot("01-first-habit-done")
    }

    func testFreeVersionOffersTheUnlockAfterThreeHabits() {
        let app = launch()
        addHabit("Teeth", in: app, first: true)
        addHabit("Stretch", in: app)
        addHabit("Read", in: app)

        app.buttons["newHabit"].tap()
        XCTAssert(app.buttons["buyUnlock"].waitForExistence(timeout: 5))
        screenshot("06-paywall")
    }

    func testLockedStatsOfferTheUnlock() {
        let app = launch("-sample")
        app.buttons["stats"].tap()
        XCTAssert(app.buttons["unlockStats"].waitForExistence(timeout: 5))
        screenshot("07-stats-locked")
    }

    func testEveryScreen() {
        let app = launch("-sample", "-unlocked")
        XCTAssert(app.navigationBars["Routine"].waitForExistence(timeout: 5))
        screenshot("02-routine")

        open("stats", title: "Stats", in: app)
        screenshot("03-stats")
        app.buttons["Done"].tap()

        open("settings", title: "Settings", in: app)
        screenshot("04-settings")
        app.buttons["Done"].tap()

        open("editHabits", title: "Edit habits", in: app)
        screenshot("05-edit-habits")
        app.buttons["Done"].tap()
    }

    // MARK: - Helpers

    private func launch(_ arguments: String...) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uitest"] + arguments
        app.launch()
        return app
    }

    private func addHabit(_ name: String, in app: XCUIApplication, first: Bool = false) {
        app.buttons[first ? "addFirstHabit" : "newHabit"].tap()
        let field = app.textFields["newHabitName"]
        XCTAssert(field.waitForExistence(timeout: 5))
        field.tap()
        field.typeText(name)
        app.buttons["confirmNewHabit"].tap()
        XCTAssert(field.waitForNonExistence(timeout: 5))
    }

    private func open(_ button: String, title: String, in app: XCUIApplication) {
        app.buttons[button].tap()
        XCTAssert(app.navigationBars[title].waitForExistence(timeout: 5), "\(title) didn't open")
    }

    private func screenshot(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
        if let directory = ProcessInfo.processInfo.environment["SCREENSHOT_DIR"] {
            try? shot.pngRepresentation.write(to: URL(fileURLWithPath: directory).appendingPathComponent("\(name).png"))
        }
    }
}
