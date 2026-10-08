package wales.ryland.routine.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The handful of icons the app needs, so it doesn't pull in an icon library. */
object RoutineIcons {
    /** The check from the app icon. */
    val Check = icon("Check", "M2,12.45L9.06,19.17L22,6.88L20.06,4.83L9.07,15.27L3.95,10.4Z")
    val Add = icon("Add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    val Edit = icon(
        "Edit",
        "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41" +
            "l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z",
    )
    val Close = icon(
        "Close",
        "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z",
    )
    val DragHandle = icon("DragHandle", "M20,9H4v2h16V9zM4,15h16v-2H4v2z")
    val ArrowBack = icon("ArrowBack", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")

    /** The gold star from the original app, shown when every habit is done. */
    val GoldStar: ImageVector = ImageVector.Builder(
        name = "GoldStar",
        defaultWidth = 32.dp,
        defaultHeight = 29.dp,
        viewportWidth = 32f,
        viewportHeight = 29f,
    ).addPath(
        pathData = addPathNodes(
            "M16,0L19.59,11.06H31.22L21.81,17.89L25.4,28.94L16,22.11L6.6,28.94L10.19,17.89L0.78,11.06H12.41L16,0Z",
        ),
        fill = Brush.linearGradient(
            colors = listOf(RoutineColors.Gold, RoutineColors.Yellow),
            start = Offset(0.22f, 1.18f),
            end = Offset(34.43f, 4.91f),
        ),
    ).build()

    private fun icon(name: String, path: String): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()
}
