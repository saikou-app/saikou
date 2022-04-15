package ani.saikou.anime

import ani.saikou.anime.source.HAnimeSources
import ani.saikou.anime.source.WatchSources

class HWatchFragment : AnimeWatchFragment() {
    override val watchSources: WatchSources = HAnimeSources
}
