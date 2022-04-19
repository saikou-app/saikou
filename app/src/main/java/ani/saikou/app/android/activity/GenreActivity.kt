package ani.saikou.app.android.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.app.android.adapter.media.GenreAdapter
import ani.saikou.app.util.anilist.anilist.Anilist
import ani.saikou.app.util.anilist.anilist.GenresViewModel
import ani.saikou.app.databinding.ActivityGenreBinding
import ani.saikou.app.util.initActivity
import ani.saikou.app.util.navBarHeight
import ani.saikou.app.util.statusBarHeight
import ani.saikou.core.service.STORE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class GenreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGenreBinding
    val model: GenresViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)

        binding.genreContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight;bottomMargin += navBarHeight }
        val screenWidth = resources.displayMetrics.run { widthPixels / density }
        val type = intent.getStringExtra("type")
        if (type != null) {
            val adapter = GenreAdapter(type, true)
            model.doneListener = {
                MainScope().launch {
                    binding.mediaInfoGenresProgressBar.visibility = View.GONE
                }
            }
            if (model.genres != null) {
                adapter.genres = model.genres!!
                adapter.pos = ArrayList(model.genres!!.keys)
                if (model.done)
                    model.doneListener?.invoke()
            }
            binding.mediaInfoGenresRecyclerView.adapter = adapter
            binding.mediaInfoGenresRecyclerView.layoutManager =
                GridLayoutManager(this, (screenWidth / 156f).toInt())

            lifecycleScope.launch(Dispatchers.IO) {
                model.loadGenres(Anilist.genres ?: STORE.loadData("genres_list") ?: arrayListOf()) {
                    MainScope().launch {
                        adapter.addGenre(it)
                    }
                }
            }
        }
    }
}