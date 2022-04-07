package ani.saikou.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.anime.source.AnimeSources
import ani.saikou.anime.source.HAnimeSources
import ani.saikou.anime.source.WatchSources
import ani.saikou.databinding.ItemEpisodeListBinding
import ani.saikou.databinding.TvItemSourceBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel

class TVSourceSelectorFragment: VerticalGridSupportFragment() {

    open val watchSources: WatchSources = AnimeSources
    val model : MediaDetailsViewModel by activityViewModels()
    private var media: Media? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Select source"

        val presenter = VerticalGridPresenter()
        presenter.numberOfColumns = 1
        gridPresenter = presenter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it

                model.watchAnimeWatchSources = if (it.isAdult) HAnimeSources else AnimeSources

                val objectAdapter = ArrayObjectAdapter(SourceAdapter())
                objectAdapter.addAll(0, watchSources.names)
                adapter = objectAdapter

            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    fun onSourceChange(selectedSourceName: String) {

        val selectedSourceIndex = watchSources.names.indexOfFirst { it == selectedSourceName }
        media?.anime?.episodes = null
        val selected = model.loadSelected(media!!)
        selected.source = selectedSourceIndex
        selected.stream = null
        model.saveSelected(media!!.id, selected, requireActivity())
        media!!.selected = selected
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {
        fun newInstance(): TVSourceSelectorFragment = TVSourceSelectorFragment()
    }

    private inner class SourceAdapter : Presenter() {

        inner class SourceViewHolder(val binding: TvItemSourceBinding) : Presenter.ViewHolder(binding.root)

            override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
            return SourceViewHolder(TvItemSourceBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
        }

        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            val holder = viewHolder as SourceViewHolder

            holder.binding.sourceName.text = item as String
            holder.binding.root.setOnClickListener {
                onSourceChange(item)
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
    }
}