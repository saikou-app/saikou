package ani.saikou.anime

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemEpisodeCompactBinding
import ani.saikou.databinding.ItemEpisodeGridBinding
import ani.saikou.databinding.ItemEpisodeListBinding
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.setAnimation
import ani.saikou.updateAnilistProgress
import com.bumptech.glide.Glide

fun handleProgress(cont: LinearLayout, bar: View, empty: View, mediaId: Int, ep: String) {
    val curr = loadData<Long>("${mediaId}_$ep")
    val max = loadData<Long>("${mediaId}_${ep}_max")
    if (curr != null && max != null) {
        cont.visibility = View.VISIBLE
        val div = curr.toFloat() / max
        val barParams = bar.layoutParams as LinearLayout.LayoutParams
        barParams.weight = div
        bar.layoutParams = barParams
        val params = empty.layoutParams as LinearLayout.LayoutParams
        params.weight = 1 - div
        empty.layoutParams = params
    } else {
        cont.visibility = View.GONE
    }
}

class EpisodeAdapter(
    private var type: Int,
    private val media: Media,
    private val fragment: AnimeWatchFragment,
    var arr: List<Episode> = arrayListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return (
            when (viewType) {
                0 -> EpisodeListViewHolder(ItemEpisodeListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                1 -> EpisodeGridViewHolder(ItemEpisodeGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                2 -> EpisodeCompactViewHolder(ItemEpisodeCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
                else -> throw IllegalArgumentException()
            }
            )
    }

    override fun getItemViewType(position: Int): Int {
        return type
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val ep = arr[position]
        val title = "${if (!ep.title.isNullOrEmpty() && ep.title != "null") "" else "Episode "}${if (!ep.title.isNullOrEmpty() && ep.title != "null") ep.title else ep.number}"

        when (holder) {
            is EpisodeListViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root, fragment.uiSettings)

                Glide.with(binding.itemEpisodeImage).load(ep.thumb ?: media.cover).override(400, 0).into(binding.itemEpisodeImage)
                binding.itemEpisodeNumber.text = ep.number
                binding.itemEpisodeTitle.text = title

                if (ep.filler) {
                    binding.itemEpisodeFiller.visibility = View.VISIBLE
                    binding.itemEpisodeFillerView.visibility = View.VISIBLE
                } else {
                    binding.itemEpisodeFiller.visibility = View.GONE
                    binding.itemEpisodeFillerView.visibility = View.GONE
                }
                binding.itemEpisodeDesc.visibility = if (ep.desc != null && ep.desc?.trim(' ') != "") View.VISIBLE else View.GONE
                binding.itemEpisodeDesc.text = ep.desc ?: ""

                if (media.userProgress != null) {
                    if (ep.number.toFloatOrNull() ?: 9999f <= media.userProgress!!.toFloat()) {
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                        binding.itemEpisodeViewed.visibility = View.VISIBLE
                    } else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeViewed.visibility = View.GONE
                        binding.itemEpisodeCont.setOnLongClickListener {
                            updateAnilistProgress(media, ep.number)
                            true
                        }
                    }
                } else {
                    binding.itemEpisodeViewedCover.visibility = View.GONE
                    binding.itemEpisodeViewed.visibility = View.GONE
                }

                handleProgress(binding.itemEpisodeProgressCont, binding.itemEpisodeProgress, binding.itemEpisodeProgressEmpty, media.id, ep.number)
            }

            is EpisodeGridViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root, fragment.uiSettings)
                Glide.with(binding.itemEpisodeImage).load(ep.thumb ?: media.cover).override(400, 0).into(binding.itemEpisodeImage)
                binding.itemEpisodeNumber.text = ep.number
                binding.itemEpisodeTitle.text = title
                if (ep.filler) {
                    binding.itemEpisodeFiller.visibility = View.VISIBLE
                    binding.itemEpisodeFillerView.visibility = View.VISIBLE
                } else {
                    binding.itemEpisodeFiller.visibility = View.GONE
                    binding.itemEpisodeFillerView.visibility = View.GONE
                }
                if (media.userProgress != null) {
                    if (ep.number.toFloatOrNull() ?: 9999f <= media.userProgress!!.toFloat()) {
                        binding.itemEpisodeViewedCover.visibility = View.VISIBLE
                        binding.itemEpisodeViewed.visibility = View.VISIBLE
                    } else {
                        binding.itemEpisodeViewedCover.visibility = View.GONE
                        binding.itemEpisodeViewed.visibility = View.GONE
                        binding.itemEpisodeCont.setOnLongClickListener {
                            updateAnilistProgress(media, ep.number)
                            true
                        }
                    }
                } else {
                    binding.itemEpisodeViewedCover.visibility = View.GONE
                    binding.itemEpisodeViewed.visibility = View.GONE
                }
                handleProgress(
                    binding.itemEpisodeProgressCont,
                    binding.itemEpisodeProgress,
                    binding.itemEpisodeProgressEmpty,
                    media.id,
                    ep.number
                )
            }

            is EpisodeCompactViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(), holder.binding.root, fragment.uiSettings)
                binding.itemEpisodeNumber.text = ep.number
                binding.itemEpisodeFillerView.visibility = if (ep.filler) View.VISIBLE else View.GONE
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
                handleProgress(binding.itemEpisodeProgressCont, binding.itemEpisodeProgress, binding.itemEpisodeProgressEmpty, media.id, ep.number)
            }
        }
    }

    override fun getItemCount(): Int = arr.size

    inner class EpisodeCompactViewHolder(val binding: ItemEpisodeCompactBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition < arr.size && bindingAdapterPosition >= 0)
                    fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
        }
    }

    inner class EpisodeGridViewHolder(val binding: ItemEpisodeGridBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition < arr.size && bindingAdapterPosition >= 0)
                    fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
        }
    }

    inner class EpisodeListViewHolder(val binding: ItemEpisodeListBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                if (bindingAdapterPosition < arr.size && bindingAdapterPosition >= 0)
                    fragment.onEpisodeClick(arr[bindingAdapterPosition].number)
            }
            binding.itemEpisodeDesc.setOnClickListener {
                if (binding.itemEpisodeDesc.maxLines == 3)
                    binding.itemEpisodeDesc.maxLines = 100
                else
                    binding.itemEpisodeDesc.maxLines = 3
            }
        }
    }

    fun updateType(t: Int) {
        type = t
    }
}
