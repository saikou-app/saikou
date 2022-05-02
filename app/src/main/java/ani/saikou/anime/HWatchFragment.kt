package ani.saikou.anime

import ani.saikou.parsers.HAnimeSources
import ani.saikou.parsers.WatchSources

class HWatchFragment : AnimeWatchFragment() {
    override val watchSources: WatchSources = HAnimeSources
}