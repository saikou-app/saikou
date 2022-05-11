package ani.saikou.anime

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.math.MathUtils
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.*
import ani.saikou.databinding.FragmentAnimeWatchBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.parsers.AnimeParser
import ani.saikou.parsers.AnimeSources
import ani.saikou.parsers.HAnimeSources
import ani.saikou.settings.PlayerSettings
import ani.saikou.settings.UserInterfaceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class AnimeWatchFragment : Fragment() {
    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!
    private val model: MediaDetailsViewModel by activityViewModels()

    private lateinit var media: Media

    private var start = 0
    private var end: Int? = null
    private var style: Int? = null
    private var reverse = false

    private lateinit var headerAdapter: AnimeWatchAdapter
    private lateinit var episodeAdapter: EpisodeAdapter

    var screenWidth = 0f
    private var progress = View.VISIBLE

    var continueEp: Boolean = false
    var loaded = false

    lateinit var playerSettings: PlayerSettings
    lateinit var uiSettings: UserInterfaceSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAnimeWatchBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.animeSourceRecycler.updatePadding(bottom = binding.animeSourceRecycler.paddingBottom + navBarHeight)
        screenWidth = resources.displayMetrics.widthPixels.dp

        var maxGridSize = (screenWidth / 100f).roundToInt()
        maxGridSize = max(4, maxGridSize - (maxGridSize % 2))

        playerSettings =
            loadData("player_settings", toast = false) ?: PlayerSettings().apply { saveData("player_settings", this) }
        uiSettings = loadData("ui_settings", toast = false) ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }

        val gridLayoutManager = GridLayoutManager(requireContext(), maxGridSize)

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val style = episodeAdapter.getItemViewType(position)

                return when (position) {
                    0    -> maxGridSize
                    else -> when (style) {
                        0    -> maxGridSize
                        1    -> 2
                        2    -> 1
                        else -> maxGridSize
                    }
                }
            }
        }

        binding.animeSourceRecycler.layoutManager = gridLayoutManager

        model.scrolledToTop.observe(viewLifecycleOwner){
            if(it) binding.animeSourceRecycler.scrollToPosition(0)
        }

        continueEp = model.continueMedia ?: false
        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it
                media.selected = model.loadSelected(media)

                style = media.selected!!.recyclerStyle
                reverse = media.selected!!.recyclerReversed

                progress = View.GONE
                binding.mediaInfoProgressBar.visibility = progress

                if (!loaded) {
                    model.watchSources = if (media.isAdult) HAnimeSources else AnimeSources

                    headerAdapter = AnimeWatchAdapter(it, this, model.watchSources)
                    episodeAdapter = EpisodeAdapter(style ?: uiSettings.animeDefaultView, media, this)

                    binding.animeSourceRecycler.adapter = ConcatAdapter(headerAdapter, episodeAdapter)

                    lifecycleScope.launch(Dispatchers.IO) {
                        awaitAll(
                            async { model.loadKitsuEpisodes(media) },
                            async { model.loadFillerEpisodes(media) }
                        )
                        model.loadEpisodes(media, media.selected!!.source)
                    }
                    loaded = true
                } else {
                    reload()
                }
            }
        }
        model.getEpisodes().observe(viewLifecycleOwner) { loadedEpisodes ->
            if (loadedEpisodes != null) {
                val episodes = loadedEpisodes[media.selected!!.source]
                if (episodes != null) {
                    episodes.forEach { (i, episode) ->
                        if (media.anime?.fillerEpisodes != null) {
                            if (media.anime!!.fillerEpisodes!!.containsKey(i)) {
                                episode.title = media.anime!!.fillerEpisodes!![i]?.title
                                episode.filler =
                                    media.anime!!.fillerEpisodes!![i]?.filler ?: false
                            }
                        }
                        if (media.anime?.kitsuEpisodes != null) {
                            if (media.anime!!.kitsuEpisodes!!.containsKey(i)) {
                                episode.desc = media.anime!!.kitsuEpisodes!![i]?.desc
                                episode.title = media.anime!!.kitsuEpisodes!![i]?.title
                                episode.thumb =
                                    media.anime!!.kitsuEpisodes!![i]?.thumb ?: FileUrl(media.cover?:"")
                            }
                        }
                    }
                    media.anime?.episodes = episodes

                    //CHIP GROUP
                    val total = episodes.size
                    val divisions = total.toDouble() / 10
                    start = 0
                    end = null
                    val limit = when {
                        (divisions < 25) -> 25
                        (divisions < 50) -> 50
                        else             -> 100
                    }
                    headerAdapter.clearChips()
                    if (total > limit) {
                        val arr = media.anime!!.episodes!!.keys.toTypedArray()
                        val stored = ceil((total).toDouble() / limit).toInt()
                        val position = MathUtils.clamp(media.selected!!.chip, 0, stored - 1)
                        val last = if (position + 1 == stored) total else (limit * (position + 1))
                        start = limit * (position)
                        end = last - 1
                        headerAdapter.updateChips(
                            limit,
                            arr,
                            (1..stored).toList().toTypedArray(),
                            position
                        )
                    }
                    reload()
                }
            }
        }

        model.getKitsuEpisodes().observe(viewLifecycleOwner) { i ->
            if (i != null)
                media.anime?.kitsuEpisodes = i
        }

        model.getFillerEpisodes().observe(viewLifecycleOwner) { i ->
            if (i != null)
                media.anime?.fillerEpisodes = i
        }
    }

    fun onSourceChange(i: Int): AnimeParser {
        media.anime?.episodes = null
        reload()
        val selected = model.loadSelected(media)
        model.watchSources[selected.source].showUserTextListener = null
        selected.source = i
        selected.server = null
        model.saveSelected(media.id, selected, requireActivity())
        media.selected = selected
        return model.watchSources[i]
    }

    fun onDubClicked(checked:Boolean){
        val selected = model.loadSelected(media)
        model.watchSources[selected.source].selectDub = checked
        selected.preferDub = checked
        model.saveSelected(media.id, selected, requireActivity())
        media.selected = selected
        lifecycleScope.launch(Dispatchers.IO) { model.forceLoadEpisode(media, selected.source) }
    }

    fun loadEpisodes(i: Int) {
        lifecycleScope.launch(Dispatchers.IO) { model.loadEpisodes(media, i) }
    }

    fun onIconPressed(viewType: Int, rev: Boolean) {
        style = viewType
        reverse = rev
        media.selected!!.recyclerStyle = style
        media.selected!!.recyclerReversed = reverse
        model.saveSelected(media.id, media.selected!!, requireActivity())
        reload()
    }

    fun onChipClicked(i: Int, s: Int, e: Int) {
        media.selected!!.chip = i
        start = s
        end = e
        model.saveSelected(media.id, media.selected!!, requireActivity())
        reload()
    }

    fun onEpisodeClick(i: String) {
        model.continueMedia = false
        model.onEpisodeClick(media, i, requireActivity().supportFragmentManager)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun reload() {
        val selected = model.loadSelected(media)
        model.saveSelected(media.id, selected, requireActivity())
        headerAdapter.handleEpisodes()
        episodeAdapter.notifyItemRangeRemoved(0, episodeAdapter.arr.size)
        var arr: ArrayList<Episode> = arrayListOf()
        if (media.anime!!.episodes != null) {
            val end = if (end != null && end!! < media.anime!!.episodes!!.size) end else null
            arr.addAll(
                media.anime!!.episodes!!.values.toList()
                    .slice(start..(end ?: (media.anime!!.episodes!!.size - 1)))
            )
            if (reverse)
                arr = (arr.reversed() as? ArrayList<Episode>) ?: arr
        }
        episodeAdapter.arr = arr
        episodeAdapter.updateType(style ?: uiSettings.animeDefaultView)
        episodeAdapter.notifyItemRangeInserted(0, arr.size)
    }

    override fun onDestroy() {
        model.watchSources.flushText()
        super.onDestroy()
    }

    var state: Parcelable? = null
    override fun onResume() {
        super.onResume()
        binding.mediaInfoProgressBar.visibility = progress
        binding.animeSourceRecycler.layoutManager?.onRestoreInstanceState(state)
    }

    override fun onPause() {
        super.onPause()
        state = binding.animeSourceRecycler.layoutManager?.onSaveInstanceState()
    }

}