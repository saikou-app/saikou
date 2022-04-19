package ani.saikou.app

import android.app.Activity
import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import ani.saikou.app.service.SeikouLoggingService
import ani.saikou.app.service.SeikouStorageService
import ani.saikou.app.util.FTActivityLifecycleCallbacks

/**
 * The **best**.
 */
class Saikou : MultiDexApplication() {
    companion object {
        private lateinit var instance: Saikou

        fun currentActivity(): Activity? =
            instance.lifecycleCallbacks.currentActivity
    }

    val lifecycleCallbacks = FTActivityLifecycleCallbacks()

    init {
        instance = this

        // Initialize core services
        SeikouLoggingService
        SeikouStorageService
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}