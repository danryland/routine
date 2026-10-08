package wales.ryland.routine.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** The one Android-only API the UI uses, kept here so the rest stays platform-neutral. */
@Composable
internal fun SystemBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
