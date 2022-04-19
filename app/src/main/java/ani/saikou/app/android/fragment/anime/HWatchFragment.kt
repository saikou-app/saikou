package ani.saikou.app.android.fragment.anime

import ani.saikou.core.source.anime.HAnimeSources
import ani.saikou.core.source.anime.WatchSources

class HWatchFragment : AnimeWatchFragment() {
    override val watchSources: WatchSources = HAnimeSources
}