package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.anime.*

object HAnimeSources : WatchSources() {

    override val list: List<Lazier<BaseParser>> = lazyList(
        ::Haho,
        ::HentaiFF,
        ::NineAnime,
        ::Gogo,
        ::Zoro,
        ::Tenshi,
        ::AnimeKisa
    )

    override val names: List<String> = listOf(
        "Haho",
        "HentaiFF",
        "9Anime",
        "Gogo",
        "Zoro",
        "Tenshi",
        "AnimeKisa"
    )

}