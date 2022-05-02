package ani.saikou.manga.source

import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.media.SourceAdapter
import ani.saikou.media.SourceSearchDialogFragment
import ani.saikou.parsers.ShowResponse
import kotlinx.coroutines.CoroutineScope

class MangaSourceAdapter(
    sources: List<ShowResponse>,
    val model: MediaDetailsViewModel,
    val i: Int,
    val id: Int,
    fragment: SourceSearchDialogFragment,
    scope: CoroutineScope
) : SourceAdapter(sources, fragment, scope) {
    override suspend fun onItemClick(source: ShowResponse) {
        model.overrideMangaChapters(i, source, id)
    }
}