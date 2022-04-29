package ani.saikou.anilist

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ani.saikou.logger
import ani.saikou.others.logError
import ani.saikou.startMainActivity

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Uri? = intent?.data
        logger(data.toString())
        try {
            Anilist.token = Regex("""(?<=access_token=).+(?=&token_type)""").find(data.toString())!!.value
            val filename = "anilistToken"
            this.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(Anilist.token!!.toByteArray())
            }
        } catch (e: Exception) {
            logError(e)
        }
        startMainActivity(this)
    }
}
