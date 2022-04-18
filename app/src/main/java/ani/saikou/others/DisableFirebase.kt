package ani.saikou.others

import com.google.firebase.crashlytics.FirebaseCrashlytics

object DisableFirebase {
    fun handle() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!DisabledReports)
    }
}