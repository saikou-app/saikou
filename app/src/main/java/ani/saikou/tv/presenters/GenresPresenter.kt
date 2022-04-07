package ani.saikou.tv.presenters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.leanback.widget.Presenter
import ani.saikou.databinding.ItemGenreBinding
import ani.saikou.databinding.TvGenreCardBinding
import ani.saikou.loadImage
import ani.saikou.px
import ani.saikou.tv.TVSearchActivity

class GenresPresenter(
    private val type: String,
    private val big:Boolean = false): Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val binding = TvGenreCardBinding.inflate(LayoutInflater.from(parent?.context), parent, false)
        if (big) binding.genreCard.updateLayoutParams { height=72f.px }
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        val genre = item as Pair<String, String>
        val holder = viewHolder as GenreViewHolder
        val binding = holder.binding
        binding.genreTitle.text = genre.first
        binding.genreImage.loadImage(item.second)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class GenreViewHolder(val binding: TvGenreCardBinding) : Presenter.ViewHolder(binding.root) {}
}