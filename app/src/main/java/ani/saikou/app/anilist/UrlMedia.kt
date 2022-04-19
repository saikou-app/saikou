package ani.saikou.app.anilist

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ani.saikou.app.util.loadIsMAL
import ani.saikou.app.util.loadMedia
import ani.saikou.app.util.startMainActivity
import ani.saikou.core.service.LOG

class UrlMedia : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Uri? = intent?.data
        if (data?.host != "anilist.co") loadIsMAL = true
        try {
            if (data?.pathSegments?.get(1) != null) loadMedia =
                data.pathSegments?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            LOG.notify(e.toString())
        }
        startMainActivity(this)
    }
}