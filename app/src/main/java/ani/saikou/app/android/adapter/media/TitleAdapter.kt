package ani.saikou.app.android.adapter.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.app.databinding.ItemTitleBinding

class TitleAdapter(private val text: String) :
    RecyclerView.Adapter<TitleAdapter.TitleViewHolder>() {
    inner class TitleViewHolder(val binding: ItemTitleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        val binding = ItemTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TitleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TitleViewHolder, position: Int) {
        holder.binding.itemTitle.text = text
    }

    override fun getItemCount(): Int = 1
}