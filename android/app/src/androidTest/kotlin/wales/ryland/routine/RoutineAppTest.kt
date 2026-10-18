package wales.ryland.routine

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import wales.ryland.routine.core.LegacyImport
import wales.ryland.routine.data.AppSettings
import wales.ryland.routine.data.Storage
import wales.ryland.routine.ui.sampleRoutine

/**
 * Drives the real app on a device or emulator. Screenshots of each screen are
 * saved to the app's files/screenshots folder (CI pulls them off the emulator).
 */
@RunWith(AndroidJUnit4::class)
class RoutineAppTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun addingAndTickingAHabit() {
        launch()
        addHabit("Drink water", first = true)

        val cell = compose.onNode(hasContentDescription("Drink water", substring = true) and hasClickAction())
        cell.assert(state("Not done"))
        cell.performClick()
        cell.assert(state("Done"))
        screenshot("01-first-habit-done")
    }

    @Test
    fun freeVersionOffersTheUnlockAfterThreeHabits() {
        launch()
        addHabit("Teeth", first = true)
        addHabit("Stretch")
        addHabit("Read")

        compose.onNodeWithTag("newHabit").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("buyUnlock").fetchSemanticsNodes().isNotEmpty() }
        screenshot("06-paywall", compose.onNodeWithTag("paywall"))
    }

    @Test
    fun lockedStatsOfferTheUnlock() {
        launch(sample = true)
        compose.onNodeWithContentDescription("Stats").performClick()
        compose.onNodeWithText("Unlock Routine Pro").assertExists()
        screenshot("07-stats-locked")
    }

    @Test
    fun everyScreen() {
        launch(sample = true, unlocked = true)
        screenshot("02-routine")

        compose.onNodeWithContentDescription("Stats").performClick()
        compose.onNodeWithContentDescription("Perfect days", substring = true).assertExists()
        screenshot("03-stats")
        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Tip jar".uppercase()).assertExists()
        screenshot("04-settings")
        compose.onNodeWithContentDescription("Back").performClick()

        compose.onNodeWithContentDescription("Edit habits").performClick()
        compose.onNodeWithText("Done").assertExists()
        screenshot("05-edit-habits")
    }

    // Helpers

    private fun launch(sample: Boolean = false, unlocked: Boolean = false) {
        Storage.file(context).delete()
        context.getSharedPreferences(LegacyImport.PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(AppSettings.NAME, Context.MODE_PRIVATE).edit()
            .clear()
            .putBoolean(AppSettings.DEBUG_UNLOCKED, unlocked)
            .commit()
        if (sample) Storage.store(context).save(sampleRoutine())
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Routine")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun addHabit(name: String, first: Boolean = false) {
        if (first) compose.onNodeWithText("Add a habit").performClick() else compose.onNodeWithTag("newHabit").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(name)
        compose.onNodeWithText("Add").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasContentDescription(name, substring = true) and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun state(description: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)

    private fun screenshot(name: String, node: SemanticsNodeInteraction = compose.onAllNodes(isRoot())[0]) {
        compose.waitForIdle()
        val bitmap = node.captureToImage().asAndroidBitmap()
        val directory = File(context.filesDir, "screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
