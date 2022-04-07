package ani.saikou.tv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import ani.saikou.R
import ani.saikou.Refresh
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistAnimeViewModel
import ani.saikou.anilist.GenresViewModel
import ani.saikou.anilist.SearchResults
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.tv.components.CustomListRowPresenter
import ani.saikou.tv.presenters.AnimePresenter
import ani.saikou.tv.presenters.GenresPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TVAnimeFragment: BrowseSupportFragment()  {

    val model: AnilistAnimeViewModel by activityViewModels()
    val genresModel: GenresViewModel by viewModels()

    lateinit var genresAdapter: ArrayObjectAdapter
    lateinit var trendingAdapter: ArrayObjectAdapter
    lateinit var updatedAdapter: ArrayObjectAdapter
    lateinit var popularAdapter: ArrayObjectAdapter
    lateinit var rowAdapter: ArrayObjectAdapter
    var loading = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUIElements()

        if(model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults("ANIME", isAdult = false, onList = false, results = arrayListOf(), hasNextPage = true, sort = "Popular")
        }

        //TODO Pagination of all lists
        /*binding.animePageRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                if (!v.canScrollVertically(1)) {
                    if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading) {
                        scope.launch(Dispatchers.IO) {
                            loading=true
                            model.loadNextPage(model.searchResults)
                        }
                    }
                }
                if(layout.findFirstVisibleItemPosition()>1 && !visible){
                    binding.animePageScrollTop.visibility = View.VISIBLE
                    visible = true
                    animate()
                }

                if(!v.canScrollVertically(-1)){
                    visible = false
                    animate()
                    scope.launch{
                        delay(300)
                        binding.animePageScrollTop.visibility = View.GONE
                    }
                }

                super.onScrolled(v, dx, dy)
            }
        })*/



        val presenter = CustomListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
        presenter.shadowEnabled = false
        rowAdapter = ArrayObjectAdapter(presenter)
        adapter = rowAdapter

        genresAdapter = ArrayObjectAdapter(GenresPresenter("ANIME",true))
        trendingAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        popularAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        updatedAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))

        //This determines order in screen
        rowAdapter.add(ListRow(HeaderItem(1, "Genres"), genresAdapter))
        rowAdapter.add(ListRow(HeaderItem(0, "Trending"), trendingAdapter))
        rowAdapter.add(ListRow(HeaderItem(0, "Popular"), popularAdapter))
        rowAdapter.add(ListRow(HeaderItem(0, "Updated"), updatedAdapter))

        observeData()
    }

    private fun observeData() {
        model.getTrending().observe(viewLifecycleOwner) {
            if (it != null) {
                trendingAdapter.addAll(0, it)
            }
        }

        model.getUpdated().observe(viewLifecycleOwner) {
            if (it != null) {
                updatedAdapter.addAll(0, it)
            }
        }

        model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {
                popularAdapter.addAll(0, it.results)
            }
        }

        val scope = viewLifecycleOwner.lifecycleScope
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        model.loaded = true
                        loading = false
                        model.loadTrending()
                        model.loadUpdated()
                        model.loadPopular("ANIME", sort = "Popular")
                        genresModel.loadGenres(Anilist.genres?: loadData("genres_list") ?: arrayListOf()) {
                            MainScope().launch {
                                genresAdapter.add(it)
                            }
                        }
                    }
                    live.postValue(false)
                }
            }
        }
    }

    private fun setupUIElements() {
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // Set fastLane (or headers) background color
        //brandColor = ContextCompat.getColor(requireActivity(), R.color.violet_700)
        badgeDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_logo)

        // Set search icon color.
        //searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.bg_black)
        /*setHeaderPresenterSelector(object : PresenterSelector() {
            override fun getPresenter(o: Any): Presenter {
                return AnimePresenter(0, requireActivity())
            }
        })*/

        setOnSearchClickedListener {
            val intent = Intent(requireActivity().applicationContext, TVSearchActivity::class.java)
            startActivity(intent)
        }

        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->

            if (item is Media) {
                val intent =
                    Intent(requireActivity().applicationContext, TVAnimeDetailActivity::class.java)
                intent.putExtra("media", item as Media)
                startActivity(intent)
            } else if (item is Pair<*,*>) {
                ContextCompat.startActivity(requireContext(), Intent(requireContext(), TVSearchActivity::class.java).putExtra("type","ANIME").putExtra("genre",item.first as String).putExtra("sortBy","Trending").also {
                    //TODO deal with this when we have settings on TV
                    /* if(item.lowercase()=="hentai") {
                        it.putExtra("hentai", true)
                    }*/
                },null)
            }
        }

        setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            //Pagination here??
            if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    loading=true
                    model.loadNextPage(model.searchResults)
                }
            }
        }
    }


    private inner class GridItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.cardview_dark_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            (viewHolder.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {}
    }

    companion object {
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
    }
}