package wales.ryland.routine.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import wales.ryland.routine.core.DayProgress
import wales.ryland.routine.core.Habit

/**
 * Habit names pinned on the left, one column per day scrolling sideways,
 * opening on today.
 */
@Composable
fun RoutineGrid(
    state: RoutineUiState,
    onTap: (habitId: String, day: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val routine = state.routine
    val haptics = LocalHapticFeedback.current

    // Haptics are chosen here, where we can see the whole day: finishing the
    // last habit of the day gets a bigger "well done" than a regular tick.
    val tap: (Habit, LocalDate) -> Unit = { habit, day ->
        val finishesDay = routine.progress(day).tier != DayProgress.Tier.Perfect &&
            routine.tapped(habit.id, day).progress(day).tier == DayProgress.Tier.Perfect
        haptics.performHapticFeedback(
            when {
                finishesDay -> HapticFeedbackType.Confirm
                habit.isCounter -> HapticFeedbackType.SegmentTick
                habit.isDone(day) -> HapticFeedbackType.ToggleOff
                else -> HapticFeedbackType.ToggleOn
            },
        )
        onTap(habit.id, day)
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val nameWidth = min(200.dp, maxWidth * 0.4f)
        Row(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = RoutineLayout.Spacing, bottom = RoutineLayout.Cell),
        ) {
            NameColumn(routine.habits, Modifier.width(nameWidth))

            Box(Modifier.weight(1f)) {
                // Newest day first with a reversed layout, so it opens on today
                // and stays there when a new day is added at midnight.
                LazyRow(
                    state = rememberLazyListState(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(RoutineLayout.Spacing),
                ) {
                    items(state.days.asReversed(), key = { it.toEpochDay() }) { day ->
                        DayColumn(
                            day = day,
                            habits = routine.habits,
                            progress = routine.progress(day),
                            isToday = day == state.today,
                            onTap = { habit -> tap(habit, day) },
                        )
                    }
                }
                // Cells fade out as they slide under the names.
                Box(Modifier.matchParentSize()) {
                    Box(
                        Modifier
                            .width(12.dp)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(palette.background, Color.Transparent))),
                    )
                }
            }
        }
    }
}

@Composable
private fun NameColumn(habits: List<Habit>, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(start = 16.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(RoutineLayout.Spacing),
        horizontalAlignment = Alignment.End,
    ) {
        Spacer(Modifier.height(RoutineLayout.BadgeHeight))
        RowLabel("Date", alpha = 0.5f)
        for (habit in habits) {
            key(habit.id) {
                RowLabel(habit.name.ifEmpty { "Untitled" }, alpha = if (habit.name.isEmpty()) 0.4f else 1f)
            }
        }
    }
}

@Composable
private fun RowLabel(text: String, alpha: Float) {
    Box(
        Modifier
            .height(RoutineLayout.Cell)
            .fillMaxWidth()
            // Each cell already says which habit it is.
            .clearAndSetSemantics {},
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = alpha),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DayColumn(
    day: LocalDate,
    habits: List<Habit>,
    progress: DayProgress,
    isToday: Boolean,
    onTap: (Habit) -> Unit,
) {
    Column(
        Modifier.width(RoutineLayout.Cell),
        verticalArrangement = Arrangement.spacedBy(RoutineLayout.Spacing),
    ) {
        ProgressBadge(progress, Modifier.height(RoutineLayout.BadgeHeight).fillMaxWidth())
        DayHeader(day, isToday)
        for (habit in habits) {
            key(habit.id) {
                HabitCell(habit, day, onClick = { onTap(habit) })
            }
        }
    }
}

@Composable
private fun ProgressBadge(progress: DayProgress, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val isPerfect = progress.tier == DayProgress.Tier.Perfect
    AnimatedContent(
        targetState = isPerfect,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = { (scaleIn(initialScale = 0.4f) + fadeIn()) togetherWith fadeOut() },
        label = "progress badge",
    ) { perfect ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (perfect) {
                Image(RoutineIcons.GoldStar, contentDescription = "Everything done", modifier = Modifier.size(28.dp, 26.dp))
            } else {
                Text(
                    "${progress.percent}%",
                    color = progress.color(palette),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = "${progress.percent} percent done" },
                )
            }
        }
    }
}

private fun DayProgress.color(palette: Palette): Color = when (tier) {
    DayProgress.Tier.NearlyThere -> RoutineColors.Mint
    DayProgress.Tier.High -> RoutineColors.Green
    DayProgress.Tier.Medium -> palette.accent.copy(alpha = 0.8f)
    DayProgress.Tier.Low -> palette.accent.copy(alpha = 0.6f)
    DayProgress.Tier.None, DayProgress.Tier.NothingToDo, DayProgress.Tier.Perfect -> Color.White.copy(alpha = 0.4f)
}

@Composable
private fun DayHeader(day: LocalDate, isToday: Boolean) {
    val locale = Locale.getDefault()
    val label = remember(day, locale) { day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)) }
    Column(
        Modifier
            .size(RoutineLayout.Cell)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isToday) Color.White.copy(alpha = 0.07f) else Color.Transparent)
            .clearAndSetSemantics {
                contentDescription = label
                heading()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val color = Color.White.copy(alpha = if (isToday) 1f else 0.5f)
        Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("${day.dayOfMonth}", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun HabitCell(habit: Habit, day: LocalDate, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val count = habit.count(day)
    val isDone = count >= habit.goal
    val shape = RoundedCornerShape(8.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "press")
    val fill by animateColorAsState(if (isDone) palette.accent else Color.Transparent, label = "fill")
    val border by animateColorAsState(if (isDone) palette.accent else palette.track, label = "border")
    val dateLabel = remember(day) { day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) }

    Box(
        Modifier
            .size(RoutineLayout.Cell)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Days before a habit existed are dimmed (but can still be filled in).
            .alpha(if (habit.isTracked(day)) 1f else 0.4f)
            .clip(shape)
            .background(fill)
            .border(2.dp, border, shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "${habit.name}, $dateLabel"
                stateDescription = when {
                    isDone -> "Done"
                    habit.isCounter -> "$count of ${habit.goal}"
                    else -> "Not done"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isDone || !habit.isCounter) {
            Icon(
                RoutineIcons.Check,
                contentDescription = null,
                tint = if (isDone) Color.White else palette.track,
                modifier = Modifier.size(32.dp),
            )
        } else {
            CounterRing(count, habit.goal)
        }
    }
}

@Composable
private fun CounterRing(count: Int, goal: Int, strokeWidth: Dp = 5.dp) {
    val palette = LocalPalette.current
    val progress by animateFloatAsState(count.toFloat() / goal, label = "ring")
    Box(Modifier.fillMaxSize().padding(11.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            inset(stroke / 2) {
                drawArc(palette.track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(stroke))
                drawArc(
                    palette.accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text("$count", color = palette.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
