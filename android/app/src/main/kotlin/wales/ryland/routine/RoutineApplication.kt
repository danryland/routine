package wales.ryland.routine

import android.app.Application
import wales.ryland.routine.billing.Billing
import wales.ryland.routine.data.AppSettings

class RoutineApplication : Application() {
    val settings: AppSettings by lazy { AppSettings(this) }
    val billing: Billing by lazy { Billing(this, settings) }
}
