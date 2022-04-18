package ani.saikou.android.manga.source

import ani.saikou.core.model.media.Source
import ani.saikou.android.media.MediaDetailsViewModel
import ani.saikou.android.media.SourceAdapter
import ani.saikou.android.media.SourceSearchDialogFragment
import kotlinx.coroutines.CoroutineScope

class MangaSourceAdapter(
    sources: ArrayList<Source>,
    val model: MediaDetailsViewModel,
    val i: Int,
    val id: Int,
    fragment: SourceSearchDialogFragment,
    scope: CoroutineScope
) : SourceAdapter(sources, fragment, scope) {
    override fun onItemClick(source: Source) {
        model.overrideMangaChapters(i, source, id)
    }
}