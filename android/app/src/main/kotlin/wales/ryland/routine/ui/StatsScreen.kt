package wales.ryland.routine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt
import wales.ryland.routine.core.Habit
import wales.ryland.routine.core.Routine
import wales.ryland.routine.core.bestPerfectStreak
import wales.ryland.routine.core.bestStreak
import wales.ryland.routine.core.completionRate
import wales.ryland.routine.core.currentPerfectStreak
import wales.ryland.routine.core.currentStreak
import wales.ryland.routine.core.perfectDayCount

/** Streaks, a monthly calendar and per-habit success rates. Part of Pro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(state: RoutineUiState, onBack: () -> Unit, onUnlock: () -> Unit) {
    val palette = LocalPalette.current
    Scaffold(
        containerColor = palette.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Stats", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.background),
                )
                HorizontalDivider(color = palette.track)
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (state.isUnlocked) Modifier else Modifier.blur(10.dp).clearAndSetSemantics {})
                    .verticalScroll(rememberScrollState(), enabled = state.isUnlocked)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatsContent(state.routine, state.today)
            }
            if (!state.isUnlocked) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.raised)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.accent, modifier = Modifier.size(36.dp))
                    Text("Streaks and stats are part of Routine Pro", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Button(onClick = onUnlock) { Text("Unlock Routine Pro") }
                }
            }
        }
    }
}

@Composable
private fun StatsContent(routine: Routine, today: LocalDate) {
    val wide = Modifier.widthIn(max = 560.dp).fillMaxWidth()
    Row(wide, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(routine.perfectDayCount(today), "Perfect days", Icons.Rounded.Star, Modifier.weight(1f))
        StatTile(routine.currentPerfectStreak(today), "Current run", Icons.Rounded.LocalFireDepartment, Modifier.weight(1f))
        StatTile(routine.bestPerfectStreak(today), "Best run", Icons.Rounded.EmojiEvents, Modifier.weight(1f))
    }
    MonthCalendar(routine, today, wide)
    for (habit in routine.habits) {
        HabitStatsRow(habit, today, wide)
    }
}

@Composable
private fun StatTile(value: Int, label: String, icon: ImageVector, modifier: Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.raised)
            .padding(vertical = 14.dp)
            .clearAndSetSemantics { contentDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = palette.accent)
        Text("$value", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A month of days, each shaded by how much got done. */
@Composable
private fun MonthCalendar(routine: Routine, today: LocalDate, modifier: Modifier) {
    val palette = LocalPalette.current
    val locale = Locale.getDefault()
    var month by rememberSaveable { mutableStateOf(today.withDayOfMonth(1).toEpochDay()) }
    val first = LocalDate.ofEpochDay(month)
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val blanks = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells: List<LocalDate?> = List(blanks) { null } + (0 until first.lengthOfMonth()).map { first.plusDays(it.toLong()) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.raised)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = first.minusMonths(1).toEpochDay() }, enabled = first > routine.startDate.withDayOfMonth(1)) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                first.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { month = first.plusMonths(1).toEpochDay() }, enabled = first < today.withDayOfMonth(1)) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (offset in 0 until 7) {
                Text(
                    firstDayOfWeek.plus(offset.toLong()).getDisplayName(DateTextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (week in cells.chunked(7)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (index in 0 until 7) {
                    val day = week.getOrNull(index)
                    Box(Modifier.weight(1f).aspectRatio(1.1f)) {
                        if (day != null) CalendarDay(day, routine, today)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(day: LocalDate, routine: Routine, today: LocalDate) {
    val palette = LocalPalette.current
    val isTracked = day >= routine.startDate && day <= today
    val progress = routine.progress(day)
    val shape = RoundedCornerShape(8.dp)
    val label = day.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                if (isTracked && progress.total > 0) palette.accent.copy(alpha = 0.12f + 0.88f * progress.percent / 100f)
                else palette.track.copy(alpha = 0.35f),
            )
            .then(if (day == today) Modifier.border(1.5.dp, Color.White.copy(alpha = 0.8f), shape) else Modifier)
            .clearAndSetSemantics {
                contentDescription = if (isTracked) "$label, ${progress.percent} percent done" else "$label, not tracked"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isTracked && progress.done == progress.total && progress.total > 0) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        } else {
            Text("${day.dayOfMonth}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = if (isTracked) 0.9f else 0.35f))
        }
    }
}

@Composable
private fun HabitStatsRow(habit: Habit, today: LocalDate, modifier: Modifier) {
    val palette = LocalPalette.current
    val rate = habit.completionRate(30, today)
    val streak = habit.currentStreak(today)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.raised)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(habit.name.ifEmpty { "Untitled" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text("$streak", color = palette.accent, fontWeight = FontWeight.Bold, modifier = Modifier.clearAndSetSemantics { contentDescription = "Current streak $streak days" })
        }
        LinearProgressIndicator(
            progress = { (rate ?: 0.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = palette.accent,
            trackColor = palette.track,
        )
        Row {
            Text(
                rate?.let { "${(it * 100).roundToInt()}% of the last 30 days" } ?: "Not enough days yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text("Best ${habit.bestStreak(today)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
