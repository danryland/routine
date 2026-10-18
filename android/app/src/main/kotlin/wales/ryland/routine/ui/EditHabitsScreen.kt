package wales.ryland.routine.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import wales.ryland.routine.core.Habit

private val EditRowHeight = 64.dp
private val EditRowSpacing = 8.dp

/** Rename, reorder, re-goal and delete habits. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitsScreen(habits: List<Habit>, actions: RoutineActions, onDone: () -> Unit) {
    val palette = LocalPalette.current
    var pendingDelete by remember { mutableStateOf<Habit?>(null) }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Edit habits", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDone) { Icon(RoutineIcons.ArrowBack, contentDescription = "Back") }
                    },
                    actions = { TextButton(onClick = onDone) { Text("Done") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.background),
                )
                HorizontalDivider(color = palette.track)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (habits.isEmpty()) {
                Text("No habits yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ReorderableHabits(
                habits = habits,
                onMove = actions::move,
                onRename = actions::rename,
                onSetGoal = actions::setGoal,
                onDelete = { pendingDelete = it },
            )
            Text(
                "Drag ☰ to reorder. Changing a goal never changes which past days count as done.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    pendingDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete “${habit.name.ifEmpty { "Untitled" }}”?") },
            text = { Text("Its history will be deleted too. This can’t be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    actions.delete(habit.id)
                    pendingDelete = null
                }) { Text("Delete", color = palette.accent) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ReorderableHabits(
    habits: List<Habit>,
    onMove: (from: Int, to: Int) -> Unit,
    onRename: (id: String, name: String) -> Unit,
    onSetGoal: (id: String, goal: Int) -> Unit,
    onDelete: (Habit) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val stepPx = with(LocalDensity.current) { (EditRowHeight + EditRowSpacing).toPx() }
    val currentHabits by rememberUpdatedState(habits)
    val currentOnMove by rememberUpdatedState(onMove)
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(EditRowSpacing)) {
        habits.forEachIndexed { index, habit ->
            key(habit.id) {
                val isDragging = draggingId == habit.id
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "lift")
                EditHabitRow(
                    habit = habit,
                    onRename = { onRename(habit.id, it) },
                    onSetGoal = { onSetGoal(habit.id, it) },
                    onDelete = { onDelete(habit) },
                    moveActions = listOfNotNull(
                        if (index > 0) CustomAccessibilityAction("Move up") { onMove(index, index - 1); true } else null,
                        if (index < habits.lastIndex) CustomAccessibilityAction("Move down") { onMove(index, index + 1); true } else null,
                    ),
                    handleModifier = Modifier.pointerInput(habit.id) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                draggingId = habit.id
                                dragOffset = 0f
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            },
                            onDragEnd = {
                                draggingId = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggingId = null
                                dragOffset = 0f
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                            val from = currentHabits.indexOfFirst { it.id == habit.id }
                            if (from < 0) return@detectVerticalDragGestures
                            val to = (from + (dragOffset / stepPx).roundToInt()).coerceIn(0, currentHabits.lastIndex)
                            if (to != from) {
                                currentOnMove(from, to)
                                dragOffset -= (to - from) * stepPx
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                        }
                    },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .shadow(elevation, RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}

@Composable
private fun EditHabitRow(
    habit: Habit,
    onRename: (String) -> Unit,
    onSetGoal: (Int) -> Unit,
    onDelete: () -> Unit,
    moveActions: List<CustomAccessibilityAction>,
    handleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var name by remember(habit.id) { mutableStateOf(habit.name) }
    Row(
        modifier
            .fillMaxWidth()
            .height(EditRowHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.raised)
            .semantics { customActions = moveActions },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(handleModifier.size(48.dp, EditRowHeight), contentAlignment = Alignment.Center) {
            Icon(RoutineIcons.DragHandle, contentDescription = "Reorder", tint = Color.White.copy(alpha = 0.6f))
        }
        TextField(
            value = name,
            onValueChange = {
                name = it
                onRename(it)
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Habit name") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        GoalChip(goal = habit.goal, onSetGoal = onSetGoal)
        IconButton(onClick = onDelete) {
            Icon(RoutineIcons.Close, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun GoalChip(goal: Int, onSetGoal: (Int) -> Unit) {
    val palette = LocalPalette.current
    var expanded by remember { mutableStateOf(false) }
    val options = remember(goal) { (1..20).toList().let { if (goal in it) it else it + goal } }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = CircleShape,
            color = palette.track,
            contentColor = Color.White,
            modifier = Modifier.semantics { contentDescription = "Daily goal: ${goalLong(goal)}" },
        ) {
            Text(goalShort(goal), Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = { Text(goalLong(option)) },
                    onClick = {
                        onSetGoal(option)
                        expanded = false
                    },
                    trailingIcon = if (option == goal) {
                        { Icon(RoutineIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
