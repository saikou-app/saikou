package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import ani.saikou.anime.Episode
import ani.saikou.anime.handleProgress
import ani.saikou.databinding.TvEpisodeCardBinding
import ani.saikou.media.Media
import ani.saikou.setAnimation
import ani.saikou.tv.TVAnimeDetailFragment
import ani.saikou.updateAnilistProgress
import com.bumptech.glide.Glide

class EpisodePresenter(
    private var type:Int,
    private var media: Media,
    private val fragment: TVAnimeDetailFragment
): Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): Presenter.ViewHolder {
        return EpisodeListViewHolder(TvEpisodeCardBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        when (val holder = viewHolder) {
            is EpisodeListViewHolder -> {
                val binding = holder.binding
                setAnimation(fragment.requireContext(),holder.binding.root,fragment.uiSettings)
                val ep = (item as Episode)

                binding.root.setOnClickListener {
                        fragment.onEpisodeClick(ep.number)
                }

                binding.itemEpisodeDesc.setOnClickListener {
                    if(binding.itemEpisodeDesc.maxLines == 3)
                        binding.itemEpisodeDesc.maxLines = 100
                    else
                        binding.itemEpisodeDesc.maxLines = 3
                }

                Glide.with(binding.itemEpisodeImage).load(ep.thumb?:media.cover).override(400,0).into(binding.itemEpisodeImage)
                binding.itemEpisodeNumber.text = ep.number
                if(ep.filler){
                    binding.itemEpisodeFiller.visibility = View.VISIBLE
                    binding.itemEpisodeFillerView.visibility = View.VISIBLE
                }else{
                    binding.itemEpisodeFiller.visibility = View.GONE
                    binding.itemEpisodeFillerView.visibility = View.GONE
                }
                binding.itemEpisodeDesc.visibility = if (ep.desc!=null && ep.desc?.trim(' ')!="") View.VISIBLE else View.GONE
                binding.itemEpisodeDesc.text = ep.desc?:""
                binding.itemEpisodeTitle.text = ep.title?:media.userPreferredName
                if (media.userProgress!=null) {
                    if (ep.number.toFloatOrNull()?:9999f<=media.userProgress!!.toFloat()) {
                        binding.itemEpisodeViewedCover.visibility= View.VISIBLE
                        binding.itemEpisodeViewed.visibility = View.VISIBLE
                    } else{
                        binding.itemEpisodeViewedCover.visibility= View.GONE
                        binding.itemEpisodeViewed.visibility = View.GONE
                        binding.itemEpisodeCont.setOnLongClickListener{
                            updateAnilistProgress(media.id, ep.number)
                            true
                        }
                    }
                }else{
                    binding.itemEpisodeViewedCover.visibility= View.GONE
                    binding.itemEpisodeViewed.visibility = View.GONE
                }

                handleProgress(binding.itemEpisodeProgressCont,binding.itemEpisodeProgress,binding.itemEpisodeProgressEmpty,media.id,ep.number)
            }
        }
    }

    inner class EpisodeListViewHolder(val binding: TvEpisodeCardBinding) : Presenter.ViewHolder(binding.root) {}
}