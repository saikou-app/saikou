package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.anime.*

object AnimeSources : WatchSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "9Anime" to ::NineAnime,
        "Gogo" to ::Gogo,
        "AllAnime" to ::AllAnime,
        "Zoro" to ::Zoro,
        "Tenshi" to ::Tenshi,
        "Kamyroll" to ::Kamyroll,
        "9Anime Backup" to ::AnimeKisa,
        "AnimePahe" to ::AnimePahe,
        "AnimixPlay" to ::AnimixPlay,
    )
}

object HAnimeSources : WatchSources() {
    val aList: List<Lazier<BaseParser>>  = lazyList(
        "HentaiMama" to ::HentaiMama,
        "Haho" to ::Haho,
        "HentaiStream" to ::HentaiStream,
        "HentaiFF" to ::HentaiFF,
    )

    override val list = listOf(aList,AnimeSources.list).flatten()
}
