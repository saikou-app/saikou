package ani.saikou.app.android.fragment

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.marginBottom
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.app.android.activity.AnimePageAdapter
import ani.saikou.app.android.adapter.media.MediaAdapter
import ani.saikou.app.android.adapter.media.ProgressAdapter
import ani.saikou.app.anilist.Anilist
import ani.saikou.app.anilist.AnilistAnimeViewModel
import ani.saikou.app.anilist.SearchResults
import ani.saikou.app.databinding.FragmentAnimeBinding
import ani.saikou.app.util.*
import ani.saikou.core.model.settings.UserInterfaceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min


class AnimeFragment : Fragment() {
    private var _binding: FragmentAnimeBinding? = null
    private val binding get() = _binding!!

    private var uiSettings: UserInterfaceSettings =
        loadData("ui_settings") ?: UserInterfaceSettings()

    val model: AnilistAnimeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scope = viewLifecycleOwner.lifecycleScope

        var height = statusBarHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    height = max(
                        statusBarHeight,
                        min(
                            displayCutout.boundingRects[0].width(),
                            displayCutout.boundingRects[0].height()
                        )
                    )
                }
            }
        }
        binding.animeRefresh.setSlingshotDistance(height + 128)
        binding.animeRefresh.setProgressViewEndTarget(false, height + 128)
        binding.animeRefresh.setOnRefreshListener {
            Refresh.activity[this.hashCode()]!!.postValue(true)
        }

        binding.animePageRecyclerView.updatePaddingRelative(bottom = navBarHeight + 160f.px)

        val animePageAdapter = AnimePageAdapter()
        var loading = true
        if (model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults(
                "ANIME",
                isAdult = false,
                onList = false,
                results = arrayListOf(),
                hasNextPage = true,
                sort = "Popular"
            )
        }
        val popularAdaptor = MediaAdapter(1, model.searchResults.results, requireActivity())
        val progressAdaptor = ProgressAdapter(searched = model.searched)
        val adapter = ConcatAdapter(animePageAdapter, popularAdaptor, progressAdaptor)
        binding.animePageRecyclerView.adapter = adapter
        val layout = LinearLayoutManager(requireContext())
        binding.animePageRecyclerView.layoutManager = layout

        var visible = false
        fun animate() {
            val start = if (visible) 0f else 1f
            val end = if (!visible) 0f else 1f
            ObjectAnimator.ofFloat(binding.animePageScrollTop, "scaleX", start, end).apply {
                duration = 300
                interpolator = OvershootInterpolator(2f)
                start()
            }
            ObjectAnimator.ofFloat(binding.animePageScrollTop, "scaleY", start, end).apply {
                duration = 300
                interpolator = OvershootInterpolator(2f)
                start()
            }
        }

        binding.animePageScrollTop.setOnClickListener {
            binding.animePageRecyclerView.scrollToPosition(4)
            binding.animePageRecyclerView.smoothScrollToPosition(0)
        }


        binding.animePageRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                if (!v.canScrollVertically(1)) {
                    if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading) {
                        scope.launch(Dispatchers.IO) {
                            loading = true
                            model.loadNextPage(model.searchResults)
                        }
                    }
                }
                if (layout.findFirstVisibleItemPosition() > 1 && !visible) {
                    binding.animePageScrollTop.visibility = View.VISIBLE
                    visible = true
                    animate()
                }

                if (!v.canScrollVertically(-1)) {
                    visible = false
                    animate()
                    scope.launch {
                        delay(300)
                        binding.animePageScrollTop.visibility = View.GONE
                    }
                }

                super.onScrolled(v, dx, dy)
            }
        })
        animePageAdapter.ready.observe(viewLifecycleOwner) { i ->
            if (i == true) {
                model.getUpdated().observe(viewLifecycleOwner) {
                    if (it != null) {
                        animePageAdapter.updateRecent(MediaAdapter(0, it, requireActivity()))
                    }
                }
                if (animePageAdapter.trendingViewPager != null) {
                    animePageAdapter.updateHeight()
                    model.getTrending().observe(viewLifecycleOwner) {
                        if (it != null) {
                            animePageAdapter.updateTrending(
                                MediaAdapter(
                                    if (uiSettings.smallView) 3 else 2,
                                    it,
                                    requireActivity(),
                                    viewPager = animePageAdapter.trendingViewPager
                                )
                            )
                            animePageAdapter.updateAvatar()
                        }
                    }
                }
                binding.animePageScrollTop.translationY =
                    -(navBarHeight + bottomBar.height + bottomBar.marginBottom).toFloat()
            }
        }

        model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {
                model.searchResults.hasNextPage = it.hasNextPage
                model.searchResults.page = it.page
                val prev = model.searchResults.results.size
                model.searchResults.results.addAll(it.results)
                popularAdaptor.notifyItemRangeInserted(prev, it.results.size)
                if (it.hasNextPage)
                    progressAdaptor.bar?.visibility = View.VISIBLE
                else {
                    toastString("DAMN! YOU TRULY ARE JOBLESS\nYOU REACHED THE END")
                    progressAdaptor.bar?.visibility = View.GONE
                }
                loading = false
            }
        }

        suspend fun load() = withContext(Dispatchers.Main) {
            animePageAdapter.updateAvatar()
        }

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(false) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        if (Anilist.userid == null)
                            if (Anilist.query.getUserData()) load() else logger("Error loading data")
                        else load()
                        model.loaded = true
                        model.loadTrending()
                        model.loadUpdated()
                        model.loadPopular("ANIME", sort = "Popular")
                    }
                    live.postValue(false)
                    _binding?.animeRefresh?.isRefreshing = false
                }
            }
        }
    }

    override fun onResume() {
        if (!model.loaded) Refresh.activity[this.hashCode()]!!.postValue(true)
        super.onResume()
    }
}