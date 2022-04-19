package ani.saikou.app.android.activity.media

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.app.android.adapter.media.MediaAdapter
import ani.saikou.app.android.adapter.media.ProgressAdapter
import ani.saikou.app.android.adapter.media.SearchAdapter
import ani.saikou.app.util.anilist.anilist.Anilist
import ani.saikou.app.util.anilist.anilist.AnilistSearch
import ani.saikou.app.util.anilist.anilist.SearchResults
import ani.saikou.app.databinding.ActivitySearchBinding
import ani.saikou.app.util.initActivity
import ani.saikou.app.util.navBarHeight
import ani.saikou.app.util.px
import ani.saikou.app.util.statusBarHeight
import ani.saikou.core.service.STORE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private val scope = lifecycleScope
    val model: AnilistSearch by viewModels()

    var type = "ANIME"
    var style: Int = 0
    private var screenWidth: Float = 0f

    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var progressAdapter: ProgressAdapter
    private lateinit var concatAdapter: ConcatAdapter

    var searchText: String? = null
    var genre: String? = null
    var sortBy: String? = null
    var tag: String? = null
    var adult = false
    var listOnly: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initActivity(this)
        screenWidth = resources.displayMetrics.run { widthPixels / density }

        binding.searchRecyclerView.updatePaddingRelative(
            top = statusBarHeight,
            bottom = navBarHeight + 80f.px
        )

        type = intent.getStringExtra("type") ?: type
        genre = intent.getStringExtra("genre")
        sortBy = intent.getStringExtra("sortBy")
        style = STORE.loadData<Int>("searchStyle") ?: 0
        adult = if (Anilist.adult) intent.getBooleanExtra("hentai", false) else false
        listOnly = intent.getBooleanExtra("listOnly", false)
        if (!listOnly!!) listOnly = null

        val notSet = model.notSet
        if (model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults(
                type,
                isAdult = false,
                onList = null,
                results = arrayListOf(),
                hasNextPage = false
            )
        }

        progressAdapter = ProgressAdapter(searched = model.searched)
        mediaAdapter = MediaAdapter(style, model.searchResults.results, this, matchParent = true)
        val headerAdaptor = SearchAdapter(this)

        val gridSize = (screenWidth / 124f).toInt()
        val gridLayoutManager = GridLayoutManager(this, gridSize)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (position) {
                    0 -> gridSize
                    concatAdapter.itemCount - 1 -> gridSize
                    else -> when (style) {
                        0 -> 1
                        else -> gridSize
                    }
                }
            }
        }

        concatAdapter = ConcatAdapter(headerAdaptor, mediaAdapter, progressAdapter)

        binding.searchRecyclerView.layoutManager = gridLayoutManager
        binding.searchRecyclerView.adapter = concatAdapter

        binding.searchRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                if (!v.canScrollVertically(1)) {
                    if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading) {
                        scope.launch(Dispatchers.IO) {
                            model.loadNextPage(model.searchResults)
                        }
                    }
                }
                super.onScrolled(v, dx, dy)
            }
        })

        model.getSearch().observe(this) {
            if (it != null) {
                model.searchResults.apply {
                    onList = it.onList
                    isAdult = it.isAdult
                    perPage = it.perPage
                    search = it.search
                    sort = it.sort
                    genres = it.genres
                    tags = it.tags
                    format = it.format
                    page = it.page
                    hasNextPage = it.hasNextPage
                }

                val prev = model.searchResults.results.size
                model.searchResults.results.addAll(it.results)
                mediaAdapter.notifyItemRangeInserted(prev, it.results.size)

                if (it.hasNextPage)
                    progressAdapter.bar?.visibility = View.VISIBLE
                else
                    progressAdapter.bar?.visibility = View.GONE
            }
        }

        progressAdapter.ready.observe(this) {
            if (it == true) {
                if (genre != null || sortBy != null || adult) {
                    if (!model.searched) {
                        model.searched = true
                        headerAdaptor.search.run()
                    }
                } else if (notSet)
                    headerAdaptor.requestFocus.run()
            }
        }
    }

    private var searchTimer = Timer()
    private var loading = false
    fun search(
        search: String? = null,
        genre: String? = null,
        tag: String? = null,
        sort: String? = null,
        adult: Boolean = false,
        listOnly: Boolean? = null
    ) {
        val size = model.searchResults.results.size
        model.searchResults.results.clear()
        mediaAdapter.notifyItemRangeRemoved(0, size)
        progressAdapter.bar?.visibility = View.VISIBLE

        this.genre = genre
        this.sortBy = sort
        this.searchText = search
        this.adult = adult
        this.tag = tag
        this.listOnly = listOnly

        searchTimer.cancel()
        searchTimer.purge()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                scope.launch(Dispatchers.IO) {
                    loading = true
                    model.loadSearch(
                        type,
                        search,
                        if (genre != null) arrayListOf(genre) else null,
                        if (tag != null) arrayListOf(tag) else null,
                        sort,
                        adult,
                        listOnly
                    )
                    loading = false
                }
            }
        }
        searchTimer = Timer()
        searchTimer.schedule(timerTask, 500)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun recycler() {
        mediaAdapter.type = style
        mediaAdapter.notifyDataSetChanged()
    }

    var state: Parcelable? = null
    override fun onPause() {
        super.onPause()
        state = binding.searchRecyclerView.layoutManager?.onSaveInstanceState()
    }

    override fun onResume() {
        super.onResume()
        binding.searchRecyclerView.layoutManager?.onRestoreInstanceState(state)
    }
}