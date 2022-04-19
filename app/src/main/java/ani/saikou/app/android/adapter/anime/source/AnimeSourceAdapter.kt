package ani.saikou.app.android.adapter.anime.source

import ani.saikou.app.android.adapter.media.SourceAdapter
import ani.saikou.app.android.fragment.media.SourceSearchDialogFragment
import ani.saikou.app.android.model.media.MediaDetailsViewModel
import ani.saikou.core.model.media.Source
import kotlinx.coroutines.CoroutineScope

class AnimeSourceAdapter(
    sources: List<Source>,
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