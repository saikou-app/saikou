package ani.saikou.app.android.adapter.anime

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.app.R
import ani.saikou.app.android.fragment.media.SourceSearchDialogFragment
import ani.saikou.app.android.fragment.anime.AnimeWatchFragment
import ani.saikou.app.databinding.ItemAnimeWatchBinding
import ani.saikou.app.databinding.ItemChipBinding
import ani.saikou.app.util.countDown
import ani.saikou.app.util.loadData
import ani.saikou.app.util.loadImage
import ani.saikou.app.util.px
import ani.saikou.core.model.media.Media
import ani.saikou.core.source.anime.WatchSources
import com.google.android.material.chip.Chip
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class AnimeWatchAdapter(
    private val media: Media,
    private val fragment: AnimeWatchFragment,
    private val watchSources: WatchSources
) : RecyclerView.Adapter<AnimeWatchAdapter.ViewHolder>() {

    private var _binding: ItemAnimeWatchBinding? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind = ItemAnimeWatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(bind)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val anime = media.anime!!
        val binding = holder.binding
        _binding = binding

        //Youtube
        if (anime.youtube != null && fragment.uiSettings.showYtButton) {
            binding.animeSourceYT.visibility = View.VISIBLE
            binding.animeSourceYT.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(anime.youtube))
                fragment.requireContext().startActivity(intent)
            }
        }

        //Source Selection
        binding.animeSource.setText(watchSources.names[media.selected!!.source])
        watchSources[media.selected!!.source]!!.apply {
            binding.animeSourceTitle.text = text
            textListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
        }
        binding.animeSource.setAdapter(
            ArrayAdapter(
                fragment.requireContext(),
                R.layout.item_dropdown,
                watchSources.names
            )
        )
        binding.animeSourceTitle.isSelected = true
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            fragment.onSourceChange(i).apply {
                binding.animeSourceTitle.text = text
                textListener = { MainScope().launch { binding.animeSourceTitle.text = it } }
            }
            fragment.loadEpisodes(i)
        }

        //Wrong Title
        binding.animeSourceSearch.setOnClickListener {
            SourceSearchDialogFragment().show(
                fragment.requireActivity().supportFragmentManager,
                null
            )
        }

        //Icons
        var reversed = media.selected!!.recyclerReversed
        var style = media.selected!!.recyclerStyle ?: fragment.uiSettings.animeDefaultView
        binding.animeSourceTop.rotation = if (reversed) -90f else 90f
        binding.animeSourceTop.setOnClickListener {
            reversed = !reversed
            binding.animeSourceTop.rotation = if (reversed) -90f else 90f
            fragment.onIconPressed(style, reversed)
        }
        var selected = when (style) {
            0 -> binding.animeSourceList
            1 -> binding.animeSourceGrid
            2 -> binding.animeSourceCompact
            else -> binding.animeSourceList
        }
        selected.alpha = 1f
        fun selected(it: ImageView) {
            selected.alpha = 0.33f
            selected = it
            selected.alpha = 1f
        }
        binding.animeSourceList.setOnClickListener {
            selected(it as ImageView)
            style = 0
            fragment.onIconPressed(style, reversed)
        }
        binding.animeSourceGrid.setOnClickListener {
            selected(it as ImageView)
            style = 1
            fragment.onIconPressed(style, reversed)
        }
        binding.animeSourceCompact.setOnClickListener {
            selected(it as ImageView)
            style = 2
            fragment.onIconPressed(style, reversed)
        }

        //Episode Handling
        handleEpisodes()
    }

    //Chips
    @SuppressLint("SetTextI18n")
    fun updateChips(limit: Int, names: Array<String>, arr: Array<Int>, selected: Int = 0) {
        val binding = _binding
        if (binding != null) {
            val screenWidth = fragment.screenWidth.px
            var select: Chip? = null
            for (position in arr.indices) {
                val last = if (position + 1 == arr.size) names.size else (limit * (position + 1))
                val chip = ItemChipBinding.inflate(
                    LayoutInflater.from(fragment.context),
                    binding.animeSourceChipGroup,
                    false
                ).root
                chip.isCheckable = true
                fun selected() {
                    chip.isChecked = true
                    binding.animeWatchChipScroll.smoothScrollTo(
                        (chip.left - screenWidth / 2) + (chip.width / 2),
                        0
                    )
                }
                chip.text = "${names[limit * (position)]} - ${names[last - 1]}"

                chip.setOnClickListener {
                    selected()
                    fragment.onChipClicked(position, limit * (position), last - 1)
                }
                binding.animeSourceChipGroup.addView(chip)
                if (selected == position) {
                    selected()
                    select = chip
                }
            }
            if (select != null)
                binding.animeWatchChipScroll.apply {
                    post {
                        scrollTo(
                            (select.left - screenWidth / 2) + (select.width / 2),
                            0
                        )
                    }
                }
        }
    }

    fun clearChips() {
        _binding?.animeSourceChipGroup?.removeAllViews()
    }

    @SuppressLint("SetTextI18n")
    fun handleEpisodes() {
        val binding = _binding
        if (binding != null) {
            if (media.anime?.episodes != null) {
                val anime = media.anime!!
                val episodes = anime.episodes!!.keys.toTypedArray()
                var continueEp =
                    loadData<String>("${media.id}_current_ep") ?: media.userProgress?.plus(1)
                        .toString()
                if (episodes.contains(continueEp)) {
                    binding.animeSourceContinue.visibility = View.VISIBLE
                    handleProgress(
                        binding.itemEpisodeProgressCont,
                        binding.itemEpisodeProgress,
                        binding.itemEpisodeProgressEmpty,
                        media.id,
                        continueEp
                    )
                    if ((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight > fragment.playerSettings.watchPercentage) {
                        val e = episodes.indexOf(continueEp)
                        if (e != -1 && e + 1 < episodes.size) {
                            continueEp = episodes[e + 1]
                            handleProgress(
                                binding.itemEpisodeProgressCont,
                                binding.itemEpisodeProgress,
                                binding.itemEpisodeProgressEmpty,
                                media.id,
                                continueEp
                            )
                        }
                    }
                    val ep = anime.episodes!![continueEp]!!
                    binding.itemEpisodeImage.loadImage(ep.thumb ?: media.banner ?: media.cover)
                    if (ep.filler) binding.itemEpisodeFillerView.visibility = View.VISIBLE
                    binding.animeSourceContinueText.text =
                        "Continue : Episode ${ep.number}${if (ep.filler) " - Filler" else ""}${if (ep.title != null) "\n${ep.title}" else ""}"
                    binding.animeSourceContinue.setOnClickListener {
                        fragment.onEpisodeClick(continueEp)
                    }
                    if (fragment.continueEp) {
                        if ((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight < fragment.playerSettings.watchPercentage) {
                            binding.animeSourceContinue.performClick()
                            fragment.continueEp = false
                        }

                    }
                }
                binding.animeSourceProgressBar.visibility = View.GONE
                if (anime.episodes!!.isNotEmpty()) {
                    binding.animeSourceNotFound.visibility = View.GONE
                } else {
                    binding.animeSourceNotFound.visibility = View.VISIBLE
                }
            } else {
                binding.animeSourceContinue.visibility = View.GONE
                binding.animeSourceNotFound.visibility = View.GONE
                clearChips()
                binding.animeSourceProgressBar.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(val binding: ItemAnimeWatchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            // Timer
            countDown(media, binding.animeSourceContainer)
        }
    }
}