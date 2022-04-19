package ani.saikou.app.anilist

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ani.saikou.app.util.startMainActivity
import ani.saikou.core.service.LOG

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Uri? = intent?.data
        LOG.log(data.toString())
        try {
            Anilist.token =
                Regex("""(?<=access_token=).+(?=&token_type)""").find(data.toString())!!.value
            val filename = "anilistToken"
            this.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(Anilist.token!!.toByteArray())
            }
        } catch (e: Exception) {
            LOG.notify(e.toString())
        }
        startMainActivity(this)
    }
}
