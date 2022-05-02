package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.anime.*

object AnimeSources : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        ::NineAnime,
        ::Gogo,
        ::Zoro,
        ::Tenshi,
        ::AnimeKisa,
        ::AllAnime
    )
    override val names: List<String> = listOf(
        "9Anime",
        "Gogo",
        "Zoro",
        "Tenshi",
        "AnimeKisa",
        "AllAnime"
    )
}