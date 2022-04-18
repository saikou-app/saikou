package ani.saikou.android.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.math.MathUtils.clamp
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.android.*
import ani.saikou.android.databinding.FragmentAnimeWatchBinding
import ani.saikou.android.manga.source.HMangaSources
import ani.saikou.android.manga.source.MangaParser
import ani.saikou.android.manga.source.MangaReadSources
import ani.saikou.android.manga.source.MangaSources
import ani.saikou.android.media.MediaDetailsViewModel
import ani.saikou.core.model.manga.MangaChapter
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.settings.UserInterfaceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

open class MangaReadFragment : Fragment() {
    open val mangaReadSources: MangaReadSources = MangaSources
    private var _binding: FragmentAnimeWatchBinding? = null
    private val binding get() = _binding!!
    private val model: MediaDetailsViewModel by activityViewModels()

    private lateinit var media: Media

    private var start = 0
    private var end: Int? = null
    private var style: Int? = null
    private var reverse = false

    private lateinit var headerAdapter: MangaReadAdapter
    private lateinit var chapterAdapter: MangaChapterAdapter

    var screenWidth = 0f
    private var progress = View.VISIBLE

    var continueEp: Boolean = false
    var loaded = false

    val uiSettings = loadData("ui_settings", toast = false)
        ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }

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

        val gridLayoutManager = GridLayoutManager(requireContext(), maxGridSize)

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val style = chapterAdapter.getItemViewType(position)

                return when (position) {
                    0 -> maxGridSize
                    else -> when (style) {
                        0 -> maxGridSize
                        1 -> 1
                        else -> maxGridSize
                    }
                }
            }
        }

        binding.animeSourceRecycler.layoutManager = gridLayoutManager
        continueEp = model.continueMedia ?: false
        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it
                progress = View.GONE
                binding.mediaInfoProgressBar.visibility = progress

                if (media.format == "MANGA" || media.format == "ONE_SHOT") {
                    media.selected = model.loadSelected(media)
                    style = media.selected!!.recyclerStyle
                    reverse = media.selected!!.recyclerReversed

                    if (!loaded) {
                        model.readMangaReadSources =
                            if (media.isAdult) HMangaSources else MangaSources

                        headerAdapter = MangaReadAdapter(it, this, mangaReadSources)
                        chapterAdapter =
                            MangaChapterAdapter(style ?: uiSettings.mangaDefaultView, media, this)

                        binding.animeSourceRecycler.adapter =
                            ConcatAdapter(headerAdapter, chapterAdapter)

                        lifecycleScope.launch(Dispatchers.IO) {
                            model.loadMangaChapters(media, media.selected!!.source)
                        }
                        loaded = true
                    } else {
                        reload()
                    }
                } else {
                    binding.animeNotSupported.visibility = View.VISIBLE
                    binding.animeNotSupported.text =
                        getString(R.string.not_supported, media.format ?: "")
                }
            }
        }

        model.getMangaChapters().observe(viewLifecycleOwner) { loadedChapters ->
            if (loadedChapters != null) {
                val chapters = loadedChapters[media.selected!!.source]
                if (chapters != null) {
                    media.manga?.chapters = chapters

                    //CHIP GROUP
                    val total = chapters.size
                    val divisions = total.toDouble() / 10
                    start = 0
                    end = null
                    val limit = when {
                        (divisions < 25) -> 25
                        (divisions < 50) -> 50
                        else -> 100
                    }
                    headerAdapter.clearChips()
                    if (total > limit) {
                        val arr = chapters.keys.toTypedArray()
                        val stored = ceil((total).toDouble() / limit).toInt()
                        val position = clamp(media.selected!!.chip, 0, stored - 1)
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
    }

    fun onSourceChange(i: Int): MangaParser {
        media.manga?.chapters = null
        reload()
        val selected = model.loadSelected(media)
        selected.source = i
        selected.stream = null
        model.saveSelected(media.id, selected, requireActivity())
        media.selected = selected
        return mangaReadSources[i]!!
    }

    fun loadChapters(i: Int) {
        lifecycleScope.launch(Dispatchers.IO) { model.loadMangaChapters(media, i) }
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

    fun onMangaChapterClick(i: String) {
        model.continueMedia = false
        if (media.manga?.chapters?.get(i) != null) {
            media.manga?.selectedChapter = i
            val intent =
                Intent(activity, MangaReaderActivity::class.java).apply { putExtra("media", media) }
            startActivity(intent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun reload() {
        val selected = model.loadSelected(media)
        model.saveSelected(media.id, selected, requireActivity())
        headerAdapter.handleChapters()
        chapterAdapter.notifyItemRangeRemoved(0, chapterAdapter.arr.size)
        var arr: ArrayList<MangaChapter> = arrayListOf()
        if (media.manga!!.chapters != null) {
            val end = if (end != null && end!! < media.manga!!.chapters!!.size) end else null
            arr.addAll(
                media.manga!!.chapters!!.values.toList()
                    .slice(start..(end ?: (media.manga!!.chapters!!.size - 1)))
            )
            if (reverse)
                arr = (arr.reversed() as? ArrayList<MangaChapter>) ?: arr
        }
        chapterAdapter.arr = arr
        chapterAdapter.updateType(style ?: uiSettings.mangaDefaultView)
        chapterAdapter.notifyItemRangeInserted(0, arr.size)
    }

    override fun onDestroy() {
        mangaReadSources.flushLive()
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