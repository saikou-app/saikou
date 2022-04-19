package ani.saikou.app

import android.app.Activity
import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import ani.saikou.app.service.SeikouLoggingService
import ani.saikou.app.service.SeikouStorageService
import ani.saikou.app.util.FTActivityLifecycleCallbacks

class Saikou : MultiDexApplication() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    init {
        instance = this

        // Initialize core services
        SeikouLoggingService
        SeikouStorageService
    }

    val mFTActivityLifecycleCallbacks = FTActivityLifecycleCallbacks()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(mFTActivityLifecycleCallbacks)
    }

    companion object {
        private lateinit var instance: Saikou

        fun currentActivity(): Activity? {
            return instance.mFTActivityLifecycleCallbacks.currentActivity
        }
    }
}