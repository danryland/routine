package wales.ryland.routine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.text.format.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import wales.ryland.routine.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: RoutineUiState,
    actions: RoutineActions,
    app: AppActions,
    onBack: () -> Unit,
    /** Called with the feature's name when a locked feature is tapped. */
    onLocked: (String) -> Unit,
) {
    val palette = LocalPalette.current
    var pickingTime by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.background),
                )
                HorizontalDivider(color = palette.track)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("Routine Pro") {
                if (state.isUnlocked) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = palette.accent)
                        Text(if (state.isEarlySupporter) "Unlocked free for early supporters" else "Routine Pro is unlocked", color = palette.accent)
                    }
                } else {
                    ActionRow("Unlock Routine Pro", leading = { Icon(Icons.Rounded.Star, contentDescription = null, tint = palette.accent) }) { onLocked("Everything") }
                    Divider()
                    ActionRow("Restore purchase") { app.restorePurchases() }
                }
            }

            Section("Theme") {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (option in Palette.All) {
                        val locked = option != Palette.Rose && !state.isUnlocked
                        val selected = option.id == palette.id
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(option.background)
                                .border(2.dp, if (selected) Color.White else option.track, CircleShape)
                                .clickable(role = Role.RadioButton) { if (locked) onLocked("Colour themes") else actions.setTheme(option.id) }
                                .semantics {
                                    contentDescription = option.name
                                    stateDescription = when {
                                        selected -> "Selected"
                                        locked -> "Locked"
                                        else -> "Not selected"
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(26.dp).clip(CircleShape).background(option.accent))
                            if (locked) Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Section("Reminders", footer = "Only sent if there’s something left to do that day.") {
                SwitchRow("Daily reminder", checked = state.remindersOn && state.isUnlocked) { on ->
                    if (!state.isUnlocked) onLocked("Daily reminders") else app.setReminders(on, state.reminderMinute)
                }
                if (state.remindersOn && state.isUnlocked) {
                    Divider()
                    ActionRow("Time", trailing = formatTime(state.reminderMinute)) { pickingTime = true }
                }
            }

            Section("Your data", footer = "A backup can be restored on any Android phone, iPhone or iPad running Routine. Google also backs up your routine with the rest of your phone.") {
                ActionRow("Export as CSV", leading = if (state.isUnlocked) null else LockLeading) {
                    if (state.isUnlocked) app.exportCsv() else onLocked("CSV export")
                }
                Divider()
                ActionRow("Back up to a file") { app.backUp() }
                Divider()
                ActionRow("Restore from a backup") { app.restoreBackup() }
            }

            Section("Tip jar", footer = "Tips are optional and don’t unlock anything. They just say thanks.") {
                if (state.tips.isEmpty()) {
                    Text("Tips aren’t available right now.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
                state.tips.forEachIndexed { index, tip ->
                    if (index > 0) Divider()
                    ActionRow(tip.title, trailing = tip.price) { app.buyTip(tip.id) }
                }
            }

            Section("About") {
                ActionRow("Version", trailing = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", onClick = null)
            }
        }
    }

    if (pickingTime) {
        val context = LocalContext.current
        val picker = rememberTimePickerState(
            initialHour = state.reminderMinute / 60,
            initialMinute = state.reminderMinute % 60,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    app.setReminders(true, picker.hour * 60 + picker.minute)
                    pickingTime = false
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) { Text("Cancel") }
            },
        )
    }
}

private fun formatTime(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Composable
private fun Section(title: String, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalPalette.current
    Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.raised), content = content)
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    trailing: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Text(title, color = if (onClick != null) LocalPalette.current.accent else Color.White, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private val LockLeading: @Composable () -> Unit = {
    Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
}

@Composable
private fun Divider() {
    HorizontalDivider(color = LocalPalette.current.track, modifier = Modifier.padding(start = 16.dp))
}
