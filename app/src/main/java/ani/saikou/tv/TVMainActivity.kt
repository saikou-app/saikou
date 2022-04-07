package ani.saikou.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistHomeViewModel
import ani.saikou.media.MediaDetailsActivity
import ani.saikou.others.AppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class TVMainActivity : FragmentActivity() {
    private val scope = lifecycleScope
    private var load = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_activity_main)

        if (!isOnline(this)) {
            //toastString("No Internet Connection")
            startActivity(Intent(this, NoInternet::class.java))
        } else {
            val  model : AnilistHomeViewModel by viewModels()
            model.genres.observe(this) {
                if (it!=null) {
                    if(it) {
                        if (loadMedia != null) {
                            scope.launch {
                                val media = withContext(Dispatchers.IO) {
                                    Anilist.query.getMedia(
                                        loadMedia!!,
                                        loadIsMAL
                                    )
                                }
                                if (media != null) {
                                    startActivity(
                                        Intent(
                                            this@TVMainActivity,
                                            MediaDetailsActivity::class.java
                                        ).putExtra("media", media as Serializable)
                                    )
                                } else {
                                    //toastString("Seems like that wasn't found on Anilist.")
                                }
                            }
                        }
                    }
                    else {
                        //toastString("Error Loading Tags & Genres.")
                    }
                }
            }

            //Load Data
            if (!load) {
                Anilist.getSavedToken(this)
                scope.launch(Dispatchers.IO) {
                    model.genres.postValue(Anilist.query.getGenresAndTags(this@TVMainActivity))
                    AppUpdater.check(this@TVMainActivity)
                }
                load = true
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, TVAnimeFragment())
                .commitNow()
        }
    }
}