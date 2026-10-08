package wales.ryland.routine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

private const val MAX_NEW_GOAL = 20

@Composable
fun NewHabitDialog(onDismiss: () -> Unit, onAdd: (name: String, goal: Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf(1) }
    val focus = remember { FocusRequester() }
    val trimmed = name.trim()
    val submit = { if (trimmed.isNotEmpty()) onAdd(trimmed, goal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    singleLine = true,
                    placeholder = { Text("e.g. Drink water") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily goal", style = MaterialTheme.typography.labelLarge)
                        Text(goalLong(goal), fontWeight = FontWeight.Bold)
                    }
                    FilledTonalIconButton(onClick = { goal-- }, enabled = goal > 1) {
                        Text("−", fontWeight = FontWeight.Bold)
                    }
                    FilledTonalIconButton(onClick = { goal++ }, enabled = goal < MAX_NEW_GOAL) {
                        Icon(RoutineIcons.Add, contentDescription = "More times a day")
                    }
                }
                Text(
                    if (goal == 1) "A tick box you check off once a day." else "A counter that fills up each time you tap it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = trimmed.isNotEmpty()) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )

    LaunchedEffect(Unit) { focus.requestFocus() }
}
