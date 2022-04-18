package ani.saikou.android.anime

import ani.saikou.android.anime.source.HAnimeSources
import ani.saikou.android.anime.source.WatchSources

class HWatchFragment : AnimeWatchFragment() {
    override val watchSources: WatchSources = HAnimeSources
}