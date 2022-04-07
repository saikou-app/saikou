package ani.saikou.tv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CompletionInfo
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.leanback.widget.ObjectAdapter.DataObserver
import androidx.leanback.widget.SearchBar.SearchBarListener
import androidx.leanback.widget.SearchBar.SearchBarPermissionListener
import androidx.lifecycle.lifecycleScope
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistSearch
import ani.saikou.anilist.SearchResults
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.tv.components.SearchFragment
import ani.saikou.tv.presenters.AnimePresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class TVSearchFragment: SearchFragment(), SearchSupportFragment.SearchResultProvider {

    lateinit var rowAdapter: ArrayObjectAdapter
    private val scope = lifecycleScope

    val model: AnilistSearch by viewModels()
    var type = "ANIME"

    var searchText: String? = null
    var genre: String? = null
    var sortBy: String? = null
    var _tag: String? = null
    var adult = false
    var listOnly :Boolean?= null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rowAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        super.setSearchResultProvider(this)

        val intent = requireActivity().intent
        type = intent.getStringExtra("type") ?: type
        genre = intent.getStringExtra("genre")
        sortBy = intent.getStringExtra("sortBy")
        //style = loadData<Int>("searchStyle") ?: 0
        adult = if (Anilist.adult) intent.getBooleanExtra("hentai", false) else false
        listOnly = intent.getBooleanExtra("listOnly",false)

        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            val intent = Intent(requireActivity().applicationContext, TVAnimeDetailActivity::class.java)
            intent.putExtra("media", item as Media)
            startActivity(intent)
        }

        setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    loading=true
                    model.loadNextPage(model.searchResults)
                }
            }
        }

        setObservers()
        search(null,genre,tag,sortBy,adult,listOnly)
    }

    private fun setObservers(){
        if(model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults(type,
                isAdult = false,
                onList = null,
                results = arrayListOf(),
                hasNextPage = false)
        }

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

                model.searchResults.results.addAll(it.results)
                rowAdapter.addAll(rowAdapter.size(),it.results)
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
        model.searchResults.results.clear()
        rowAdapter.clear()

        this.genre = genre
        this.sortBy = sort
        this.searchText = search
        this.adult = adult
        this._tag = tag
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

    override fun getResultsAdapter(): ObjectAdapter {
        return rowAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        search(newQuery)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }


}
