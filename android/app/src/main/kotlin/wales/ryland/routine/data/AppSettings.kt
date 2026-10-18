package wales.ryland.routine.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import wales.ryland.routine.BuildConfig

/** User settings and remembered state, kept in SharedPreferences. */
class AppSettings(context: Context) {
    data class Values(
        val themeId: String,
        /** The routine was imported from the original app: everything's free. */
        val earlySupporter: Boolean,
        /** The last known purchase state, so Pro doesn't flicker off when offline. */
        val purchasedUnlock: Boolean,
        val remindersOn: Boolean,
        val reminderMinute: Int,
        val lastReviewRequest: LocalDate?,
        /** Debug builds only: lets UI tests turn Pro on. */
        val debugUnlocked: Boolean,
    ) {
        val isUnlocked: Boolean get() = earlySupporter || purchasedUnlock || debugUnlocked
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val _values = MutableStateFlow(read())
    val values: StateFlow<Values> = _values.asStateFlow()

    // Held so it isn't garbage-collected (SharedPreferences keeps a weak reference).
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _values.value = read() }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    val current: Values get() = _values.value

    fun setTheme(id: String) = edit { putString(THEME, id) }
    fun setEarlySupporter() = edit { putBoolean(EARLY_SUPPORTER, true) }
    fun setPurchasedUnlock(purchased: Boolean) = edit { putBoolean(PURCHASED, purchased) }
    fun setReminders(on: Boolean, minute: Int) = edit { putBoolean(REMINDERS_ON, on).putInt(REMINDER_MINUTE, minute) }
    fun setLastReviewRequest(day: LocalDate) = edit { putString(LAST_REVIEW, day.toString()) }

    private fun edit(change: SharedPreferences.Editor.() -> SharedPreferences.Editor) {
        prefs.edit().change().apply()
        _values.value = read()
    }

    private fun read() = Values(
        themeId = prefs.getString(THEME, null) ?: "rose",
        earlySupporter = prefs.getBoolean(EARLY_SUPPORTER, false),
        purchasedUnlock = prefs.getBoolean(PURCHASED, false),
        remindersOn = prefs.getBoolean(REMINDERS_ON, false),
        reminderMinute = prefs.getInt(REMINDER_MINUTE, 20 * 60),
        lastReviewRequest = prefs.getString(LAST_REVIEW, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        debugUnlocked = BuildConfig.DEBUG && prefs.getBoolean(DEBUG_UNLOCKED, false),
    )

    companion object {
        const val NAME = "settings"
        const val THEME = "theme"
        const val EARLY_SUPPORTER = "earlySupporter"
        const val PURCHASED = "purchasedUnlock"
        const val REMINDERS_ON = "remindersOn"
        const val REMINDER_MINUTE = "reminderMinute"
        const val LAST_REVIEW = "lastReviewRequest"
        const val DEBUG_UNLOCKED = "debugUnlocked"
    }
}
