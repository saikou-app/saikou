package ani.saikou.anime.source

import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.media.Source
import ani.saikou.media.SourceAdapter
import ani.saikou.media.SourceSearchDialogFragment
import kotlinx.coroutines.CoroutineScope

class AnimeSourceAdapter(
    sources: ArrayList<Source>,
    val model: MediaDetailsViewModel,
    val i: Int,
    val id: Int,
    fragment: SourceSearchDialogFragment,
    scope: CoroutineScope
) : SourceAdapter(sources, fragment, scope) {
    override fun onItemClick(source: Source) {
        model.overrideEpisodes(i, source, id)
    }
}