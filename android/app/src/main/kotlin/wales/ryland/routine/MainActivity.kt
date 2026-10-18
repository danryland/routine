package wales.ryland.routine

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.play.core.review.ReviewManagerFactory
import java.time.LocalDate
import kotlinx.coroutines.launch
import wales.ryland.routine.billing.Billing
import wales.ryland.routine.core.restoreRoutine
import wales.ryland.routine.reminders.Reminders
import wales.ryland.routine.ui.AppActions
import wales.ryland.routine.ui.Palette
import wales.ryland.routine.ui.RoutineApp
import wales.ryland.routine.ui.RoutineTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RoutineViewModel by viewModels()
    private val app get() = application as RoutineApplication

    /** A backup the user picked, waiting for them to confirm replacing everything. */
    private var pendingRestore by mutableStateOf<String?>(null)
    private var pendingReminderMinute = 20 * 60

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setReminders(true, pendingReminderMinute)
        } else {
            toast("To get reminders, allow notifications for Routine in Settings.")
        }
    }
    private val createCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) write(uri, viewModel.csv())
    }
    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) write(uri, viewModel.backupJson())
    }
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) readBackup(uri)
    }

    private val appActions = object : AppActions {
        override fun buyUnlock() = buy(Billing.UNLOCK)
        override fun buyTip(id: String) = buy(id)

        override fun restorePurchases() {
            app.billing.refresh()
            toast("Checking Google Play for your purchase…")
        }

        override fun setReminders(on: Boolean, minuteOfDay: Int) {
            if (on && !Reminders.hasPermission(this@MainActivity)) {
                pendingReminderMinute = minuteOfDay
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setReminders(on, minuteOfDay)
            }
        }

        override fun exportCsv() = createCsv.launch("Routine ${LocalDate.now()}.csv")
        override fun backUp() = createBackup.launch("Routine backup ${LocalDate.now()}.json")
        override fun restoreBackup() = openBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark, so system bar icons are always light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            val reviewDue by viewModel.reviewDue.collectAsState()
            val palette = state?.palette ?: Palette.Rose

            Box(Modifier.fillMaxSize().background(palette.background)) {
                state?.let { RoutineApp(it, viewModel, appActions) }
            }

            pendingRestore?.let { text ->
                RoutineTheme(palette) {
                    AlertDialog(
                        onDismissRequest = { pendingRestore = null },
                        title = { Text("Replace your routine with this backup?") },
                        text = { Text("Everything currently in Routine will be replaced.") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.restore(text)
                                pendingRestore = null
                            }) { Text("Replace", color = palette.accent) }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingRestore = null }) { Text("Cancel") }
                        },
                    )
                }
            }

            LaunchedEffect(reviewDue) {
                if (reviewDue) requestReview()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.billing.thanks.collect { toast("Thank you so much! ♥") }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun buy(productId: String) {
        if (!app.billing.buy(this, productId)) {
            toast("Google Play isn’t reachable right now. Please try again later.")
        }
    }

    private fun requestReview() {
        val manager = ReviewManagerFactory.create(this)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) manager.launchReviewFlow(this, request.result)
        }
        viewModel.didRequestReview()
    }

    private fun write(uri: Uri, text: String?) {
        val ok = text != null && runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
        }.getOrDefault(false)
        toast(if (ok) "Saved." else "Couldn’t save the file.")
    }

    private fun readBackup(uri: Uri) {
        val text = runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
        if (text == null || restoreRoutine(text, LocalDate.now()) == null) {
            toast("That file isn’t a Routine backup.")
        } else {
            pendingRestore = text
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
