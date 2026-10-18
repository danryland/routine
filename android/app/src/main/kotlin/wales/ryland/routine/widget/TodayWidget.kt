package wales.ryland.routine.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import wales.ryland.routine.MainActivity
import wales.ryland.routine.RoutineApplication
import wales.ryland.routine.core.Habit
import wales.ryland.routine.core.Routine
import wales.ryland.routine.data.Storage
import wales.ryland.routine.ui.Palette

/** Today's habits on the home screen, with tick boxes that work without opening the app. */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = LocalDate.now()
        val routine = withContext(Dispatchers.IO) { Storage.load(context, today).routine }
        val settings = (context.applicationContext as RoutineApplication).settings.current
        val palette = Palette.named(settings.themeId, settings.isUnlocked)
        provideContent {
            WidgetContent(routine, today, settings.isUnlocked, palette)
        }
    }

    companion object {
        /** Redraws every Routine widget, e.g. after the app changes the routine. */
        suspend fun refresh(context: Context) = TodayWidget().updateAll(context)
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/** Ticks a habit (or adds one to a counter) for today. */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HabitId] ?: return
        if (!(context.applicationContext as RoutineApplication).settings.current.isUnlocked) return
        val today = LocalDate.now()
        Storage.edit(context) { it.tapped(id, today) }
        TodayWidget.refresh(context)
    }

    companion object {
        val HabitId = ActionParameters.Key<String>("habitId")
    }
}

private fun text(color: Color, size: Int, bold: Boolean = false) =
    TextStyle(color = ColorProvider(color), fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

@Composable
private fun WidgetContent(routine: Routine, today: LocalDate, isUnlocked: Boolean, palette: Palette) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(palette.background)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        when {
            !isUnlocked -> {
                Text("Widgets are part of Routine Pro", style = text(Color.White, 14, bold = true))
                Spacer(GlanceModifier.height(4.dp))
                Text("Open Routine to unlock.", style = text(Color.White.copy(alpha = 0.6f), 12))
            }
            routine.habits.isEmpty() -> {
                Text("Add habits in Routine to see them here.", style = text(Color.White.copy(alpha = 0.7f), 13))
            }
            else -> HabitList(routine, today, palette)
        }
    }
}

@Composable
private fun HabitList(routine: Routine, today: LocalDate, palette: Palette) {
    val progress = routine.progress(today)
    // Each row is about 36dp; fit as many as the widget's height allows.
    val rows = ((LocalSize.current.height.value - 50) / 36).toInt().coerceAtLeast(1)

    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Today", style = text(Color.White, 16, bold = true), modifier = GlanceModifier.defaultWeight())
        Text(
            if (progress.done == progress.total) "★ All done" else "${progress.done}/${progress.total}",
            style = text(palette.accent, 14, bold = true),
        )
    }
    Spacer(GlanceModifier.height(8.dp))
    for (habit in routine.habits.take(rows)) {
        HabitRow(habit, today, palette)
    }
    if (routine.habits.size > rows) {
        Text("+${routine.habits.size - rows} more", style = text(Color.White.copy(alpha = 0.6f), 11))
    }
}

@Composable
private fun HabitRow(habit: Habit, today: LocalDate, palette: Palette) {
    val done = habit.isDone(today)
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            habit.name.ifEmpty { "Untitled" },
            style = text(Color.White, 14, bold = true),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Box(
            modifier = GlanceModifier
                .size(40.dp, 30.dp)
                .cornerRadius(8.dp)
                .background(if (done) palette.accent else palette.track)
                .clickable(actionRunCallback<ToggleHabitAction>(actionParametersOf(ToggleHabitAction.HabitId to habit.id))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    done -> "✓"
                    habit.isCounter -> "${habit.count(today)}/${habit.goal}"
                    else -> ""
                },
                style = text(Color.White, if (done) 16 else 12, bold = true),
            )
        }
    }
}
