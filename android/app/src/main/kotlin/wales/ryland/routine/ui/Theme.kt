package wales.ryland.routine.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Colours that don't change with the theme. */
object RoutineColors {
    val Green = Color(0xFF059669)
    val Mint = Color(0xFF08E5A0)
    val Gold = Color(0xFFF4D849)
    val Yellow = Color(0xFFFFD500)
}

/** A colour theme. Rose is the original design; the rest come with the unlock. */
data class Palette(
    val id: String,
    val name: String,
    /** Page background. */
    val background: Color,
    /** Empty cells, rings' tracks, dividers. */
    val track: Color,
    /** Done cells, highlights, buttons. */
    val accent: Color,
    /** Cards and rows that sit on the background. */
    val raised: Color,
) {
    companion object {
        val Rose = Palette("rose", "Rose", Color(0xFF150107), Color(0xFF4C0519), Color(0xFFF43F5E), Color(0xFF2A0610))
        val Ocean = Palette("ocean", "Ocean", Color(0xFF030C15), Color(0xFF0C2A4A), Color(0xFF38BDF8), Color(0xFF071A2D))
        val Forest = Palette("forest", "Forest", Color(0xFF03100A), Color(0xFF0B3B24), Color(0xFFA3E635), Color(0xFF062415))
        val Amber = Palette("amber", "Amber", Color(0xFF150C02), Color(0xFF4A2A06), Color(0xFFF59E0B), Color(0xFF2B1803))
        val Violet = Palette("violet", "Violet", Color(0xFF10061C), Color(0xFF3B1466), Color(0xFFA78BFA), Color(0xFF220C3B))

        val All = listOf(Rose, Ocean, Forest, Amber, Violet)

        /** The saved theme, or Rose if it's locked or unknown. */
        fun named(id: String?, isUnlocked: Boolean): Palette =
            if (isUnlocked) All.firstOrNull { it.id == id } ?: Rose else Rose
    }
}

val LocalPalette = staticCompositionLocalOf { Palette.Rose }

object RoutineLayout {
    val Cell = 64.dp
    val Spacing = 8.dp
    val BadgeHeight = 40.dp
}

/** Routine is always dark, matching the original design. */
@Composable
fun RoutineTheme(palette: Palette = Palette.Rose, content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color.White,
        primaryContainer = palette.accent,
        onPrimaryContainer = Color.White,
        secondaryContainer = palette.track,
        onSecondaryContainer = Color.White,
        background = palette.background,
        onBackground = Color.White,
        surface = palette.background,
        onSurface = Color.White,
        surfaceVariant = palette.track,
        onSurfaceVariant = Color.White.copy(alpha = 0.7f),
        surfaceContainerLowest = palette.background,
        surfaceContainerLow = palette.background,
        surfaceContainer = palette.background,
        surfaceContainerHigh = palette.raised,
        surfaceContainerHighest = palette.raised,
        outline = palette.track,
        outlineVariant = palette.track,
    )
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

fun goalShort(goal: Int): String = if (goal == 1) "Once" else "$goal×"

fun goalLong(goal: Int): String = if (goal == 1) "Once a day" else "$goal times a day"
