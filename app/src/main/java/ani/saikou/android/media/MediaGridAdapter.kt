package ani.saikou.android.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.core.model.media.Media
import ani.saikou.android.databinding.ItemRecyclerviewBinding

class MediaGridAdapter(
    private val medias: ArrayList<Media>,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<MediaGridAdapter.MediaGridViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaGridViewHolder {
        val binding =
            ItemRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaGridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaGridViewHolder, position: Int) {
        val binding = holder.binding
        binding.root.adapter = MediaAdaptor(0, medias, activity, true)
        binding.root.layoutManager = GridLayoutManager(activity, medias.size)
    }

    override fun getItemCount(): Int = 1
    inner class MediaGridViewHolder(val binding: ItemRecyclerviewBinding) :
        RecyclerView.ViewHolder(binding.root)
}