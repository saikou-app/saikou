package ani.saikou.app.android.adapter.media

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.app.android.activity.media.SearchActivity
import ani.saikou.app.anilist.Anilist
import ani.saikou.app.databinding.ItemGenreBinding
import ani.saikou.app.util.loadImage
import ani.saikou.app.util.px

class GenreAdapter(
    private val type: String,
    private val big: Boolean = false
) : RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {
    var genres = mutableMapOf<String, String>()
    var pos = arrayListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val binding = ItemGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (big) binding.genreCard.updateLayoutParams { height = 72f.px }
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val binding = holder.binding
        if (pos.size > position) {
            val genre = genres[pos[position]]
            binding.genreTitle.text = pos[position]
            binding.genreImage.loadImage(genre)
        }
    }

    override fun getItemCount(): Int = genres.size
    inner class GenreViewHolder(val binding: ItemGenreBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                ContextCompat.startActivity(
                    itemView.context,
                    Intent(itemView.context, SearchActivity::class.java).putExtra("type", type)
                        .putExtra("genre", pos[bindingAdapterPosition])
                        .putExtra("sortBy", "Trending").also {
                        if (pos[bindingAdapterPosition].lowercase() == "hentai") {
                            if (!Anilist.adult) Toast.makeText(
                                itemView.context,
                                "Turn on 18+ Content from your Anilist Settings",
                                Toast.LENGTH_SHORT
                            ).show()
                            it.putExtra("hentai", true)
                        }
                    },
                    null
                )
            }
        }
    }

    fun addGenre(genre: Pair<String, String>) {
        genres[genre.first] = genre.second
        pos.add(genre.first)
        notifyItemInserted(pos.size - 1)
    }
}