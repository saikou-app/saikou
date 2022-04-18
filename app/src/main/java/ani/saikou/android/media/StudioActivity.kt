package ani.saikou.android.media

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.android.*
import ani.saikou.android.databinding.ActivityStudioBinding
import ani.saikou.core.model.media.Studio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudioBinding
    private val scope = lifecycleScope
    private val model: OtherDetailsViewModel by viewModels()
    private var studio: Studio? = null
    private var loaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        this.window.statusBarColor = ContextCompat.getColor(this, R.color.nav_bg)

        val screenWidth = resources.displayMetrics.run { widthPixels / density }

        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.studioRecycler.updatePadding(bottom = 64f.px + navBarHeight)
        binding.studioTitle.isSelected = true

        studio = intent.getSerializableExtra("studio") as Studio?
        binding.studioTitle.text = studio?.name

        binding.studioClose.setOnClickListener {
            onBackPressed()
        }

        model.getStudio().observe(this) {
            if (it != null) {
                studio = it
                loaded = true
                binding.studioProgressBar.visibility = View.GONE
                binding.studioRecycler.visibility = View.VISIBLE

                val titlePosition = arrayListOf<Int>()
                val concatAdapter = ConcatAdapter()
                val map = studio!!.yearMedia ?: return@observe
                val keys = map.keys.toTypedArray()
                var pos = 0

                val gridSize = (screenWidth / 124f).toInt()
                val gridLayoutManager = GridLayoutManager(this, gridSize)
                gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (position in titlePosition) {
                            true -> gridSize
                            else -> 1
                        }
                    }
                }
                for (i in keys.indices) {
                    val medias = map[keys[i]]!!
                    val empty = if (medias.size >= 4) medias.size % 4 else 4 - medias.size
                    titlePosition.add(pos)
                    pos += (empty + medias.size + 1)

                    concatAdapter.addAdapter(TitleAdapter("${keys[i]} (${medias.size})"))
                    concatAdapter.addAdapter(MediaAdaptor(0, medias, this, true))
                    concatAdapter.addAdapter(EmptyAdapter(empty))
                }

                binding.studioRecycler.adapter = concatAdapter
                binding.studioRecycler.layoutManager = gridLayoutManager
            }
        }
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            if (it) {
                scope.launch {
                    if (studio != null)
                        withContext(Dispatchers.IO) { model.loadStudio(studio!!) }
                    live.postValue(false)
                }
            }
        }
    }

    override fun onDestroy() {
        if (Refresh.activity.containsKey(this.hashCode())) {
            Refresh.activity.remove(this.hashCode())
        }
        super.onDestroy()
    }

    override fun onResume() {
        binding.studioProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }
}