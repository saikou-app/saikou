package ani.saikou.app.android.adapter.manga

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.app.android.fragment.manga.MangaReadFragment
import ani.saikou.app.databinding.ItemChapterListBinding
import ani.saikou.app.databinding.ItemEpisodeCompactBinding
import ani.saikou.app.util.setAnimation
import ani.saikou.app.util.updateAnilistProgress
import ani.saikou.core.model.manga.MangaChapter
import ani.saikou.core.model.media.Media

class MangaChapterAdapter(
    private var type: Int,
    private val media: Media,
    private val fragment: MangaReadFragment,
    var arr: ArrayList<MangaChapter> = arrayListOf(),
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> ChapterCompactViewHolder(
                ItemEpisodeCompactBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
            0 -> ChapterListViewHolder(
                ItemChapterListBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return type
    }

    override fun getItemCount(): Int = arr.size

    inner class ChapterCompactViewHolder(val binding: ItemEpisodeCompactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (0 <= bindingAdapterPosition && bindingAdapterPosition < arr.size)
                    fragment.onMangaChapterClick(arr[bindingAdapterPosition].number)
            }
        }
    }

    inner class ChapterListViewHolder(val binding: ItemChapterListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (0 <= bindingAdapterPosition && bindingAdapterPosition < arr.size)
                    fragment.onMangaChapterClick(arr[bindingAdapterPosition].number)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChapterCompactViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root, fragment.uiSettings)
                val ep = arr[position]
                binding.itemEpisodeNumber.text = ep.number
                if (media.userProgress != null) {
                    if (ep.number.toFloatOrNull() ?: 9999f <= media.userProgress!!.toFloat())
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                    else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeCont.setOnLongClickListener {
                            updateAnilistProgress(media, ep.number)
                            true
                        }
                    }
                }
            }
            is ChapterListViewHolder -> {
                val binding = holder.binding
                val ep = arr[position]
                setAnimation(fragment.requireContext(), holder.binding.root, fragment.uiSettings)
                binding.itemChapterNumber.text = ep.number
                if (!ep.title.isNullOrEmpty()) {
                    binding.itemChapterTitle.text = ep.title
                    binding.itemChapterTitle.setOnClickListener {
                        binding.itemChapterTitle.maxLines.apply {
                            binding.itemChapterTitle.maxLines = if (this == 1) 3 else 1
                        }
                    }
                    binding.itemChapterTitle.visibility = View.VISIBLE
                } else binding.itemChapterTitle.visibility = View.GONE

                if (media.userProgress != null) {
                    if (ep.number.toFloatOrNull() ?: 9999f <= media.userProgress!!.toFloat()) {
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                        binding.itemEpisodeViewed.visibility = View.VISIBLE
                    } else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeViewed.visibility = View.GONE
                        binding.root.setOnLongClickListener {
                            updateAnilistProgress(media, ep.number)
                            true
                        }
                    }
                } else {
                    binding.itemEpisodeViewedCover.visibility = View.GONE
                    binding.itemEpisodeViewed.visibility = View.GONE
                }
            }
        }
    }

    fun updateType(t: Int) {
        type = t
    }
}