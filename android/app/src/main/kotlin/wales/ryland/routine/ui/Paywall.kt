package wales.ryland.routine.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Sells the one-time unlock. Closes itself once Routine is unlocked. */
@Composable
fun PaywallDialog(headline: String, state: RoutineUiState, app: AppActions, onDismiss: () -> Unit) {
    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) onDismiss()
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val palette = LocalPalette.current
        Surface(color = palette.background, modifier = Modifier.fillMaxSize().testTag("paywall")) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Not now") }
                }
                Image(RoutineIcons.GoldStar, contentDescription = null, modifier = Modifier.size(60.dp, 55.dp))
                Text("Routine Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(headline, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.raised)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Feature(Icons.Rounded.AllInclusive, "Unlimited habits", "Track everything, not just 3 things.")
                    Feature(Icons.Rounded.Widgets, "Home screen widget", "Tick habits off without opening the app.")
                    Feature(Icons.Rounded.NotificationsActive, "Daily reminders", "A nudge at your chosen time, only if there’s something left.")
                    Feature(Icons.Rounded.BarChart, "Streaks and stats", "Your best runs, monthly calendar and success rates.")
                    Feature(Icons.Rounded.Palette, "Colour themes", "Ocean, Forest, Amber and Violet.")
                    Feature(Icons.Rounded.TableChart, "CSV export", "Your whole history, ready for a spreadsheet.")
                    Feature(Icons.Rounded.Backup, "Backed up", "Google backs up your routine with your phone.")
                }

                if (state.isUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = palette.accent)
                        Text("Unlocked. Thank you!", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = app::buyUnlock,
                        enabled = state.unlockPrice != null,
                        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().height(52.dp).testTag("buyUnlock"),
                    ) {
                        Text(state.unlockPrice?.let { "Unlock for $it" } ?: "Unlock", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "One payment, yours for good. No subscription.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = app::restorePurchases) { Text("Restore purchase") }
                    if (state.unlockPrice == null) {
                        Text(
                            "Google Play isn’t reachable right now. Please try again later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Icon(icon, contentDescription = null, tint = LocalPalette.current.accent, modifier = Modifier.size(26.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
