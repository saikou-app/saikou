package ani.saikou.media

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemCharacterBinding
import ani.saikou.loadImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class SourceAdapter(
    private val sources: ArrayList<Source>,
    private val dialogFragment: SourceSearchDialogFragment,
    private val scope: CoroutineScope
) : RecyclerView.Adapter<SourceAdapter.SourceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val binding = ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SourceViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        val binding = holder.binding
        val character = sources[position]
        binding.itemCompactImage.loadImage(character.cover, 200, character.headers)
        binding.itemCompactTitle.isSelected = true
        binding.itemCompactTitle.text = character.name
    }

    override fun getItemCount(): Int = sources.size

    abstract fun onItemClick(source: Source)

    inner class SourceViewHolder(val binding: ItemCharacterBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                dialogFragment.dismiss()
                scope.launch(Dispatchers.IO) { onItemClick(sources[bindingAdapterPosition]) }
            }
            var a = true
            itemView.setOnLongClickListener {
                a = !a
                binding.itemCompactTitle.isSingleLine = a
                true
            }
        }
    }
}
