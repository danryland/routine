package wales.ryland.routine.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object RoutineColors {
    val Background = Color(0xFF150107)
    val Maroon = Color(0xFF4C0519)
    val Rose = Color(0xFFF43F5E)
    val Green = Color(0xFF059669)
    val Mint = Color(0xFF08E5A0)
    val Gold = Color(0xFFF4D849)
    val Yellow = Color(0xFFFFD500)
    val Raised = Color(0xFF2A0610)
}

object RoutineLayout {
    val Cell = 64.dp
    val Spacing = 8.dp
    val BadgeHeight = 40.dp
}

private val colorScheme = darkColorScheme(
    primary = RoutineColors.Rose,
    onPrimary = Color.White,
    primaryContainer = RoutineColors.Rose,
    onPrimaryContainer = Color.White,
    secondaryContainer = RoutineColors.Maroon,
    onSecondaryContainer = Color.White,
    background = RoutineColors.Background,
    onBackground = Color.White,
    surface = RoutineColors.Background,
    onSurface = Color.White,
    surfaceVariant = RoutineColors.Maroon,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    surfaceContainerLowest = RoutineColors.Background,
    surfaceContainerLow = RoutineColors.Background,
    surfaceContainer = RoutineColors.Background,
    surfaceContainerHigh = RoutineColors.Raised,
    surfaceContainerHighest = RoutineColors.Raised,
    outline = RoutineColors.Maroon,
    outlineVariant = RoutineColors.Maroon,
)

/** Routine is always dark, matching the original design. */
@Composable
fun RoutineTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}

fun goalShort(goal: Int): String = if (goal == 1) "Once" else "$goal×"

fun goalLong(goal: Int): String = if (goal == 1) "Once a day" else "$goal times a day"
