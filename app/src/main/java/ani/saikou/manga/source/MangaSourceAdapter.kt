package ani.saikou.manga.source

import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.media.Source
import ani.saikou.media.SourceAdapter
import ani.saikou.media.SourceSearchDialogFragment
import kotlinx.coroutines.CoroutineScope

class MangaSourceAdapter(sources: ArrayList<Source>, val model: MediaDetailsViewModel, val i: Int, val id: Int, fragment: SourceSearchDialogFragment, scope: CoroutineScope) : SourceAdapter(sources, fragment, scope) {
    override fun onItemClick(source: Source) {
        model.overrideMangaChapters(i, source, id)
    }
}
