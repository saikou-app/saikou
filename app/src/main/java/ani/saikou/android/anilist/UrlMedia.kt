package ani.saikou.android.anilist

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ani.saikou.android.loadIsMAL
import ani.saikou.android.loadMedia
import ani.saikou.android.startMainActivity
import ani.saikou.android.toastString

class UrlMedia : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Uri? = intent?.data
        if (data?.host != "anilist.co") loadIsMAL = true
        try {
            if (data?.pathSegments?.get(1) != null) loadMedia =
                data.pathSegments?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            toastString(e.toString())
        }
        startMainActivity(this)
    }
}