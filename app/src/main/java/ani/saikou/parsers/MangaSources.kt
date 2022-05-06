package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.manga.*

object MangaSources : MangaReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        ::MangaKakalot,
        ::MangaBuddy,
        ::MangaSee,
        ::MangaPill,
        ::MangaDex,
        ::MangaReaderTo,
        ::AllAnime
    )
    override val names: List<String> = listOf(
        "MangaKakalot",
        "MangaBuddy",
        "MangaSee",
        "MangaPill",
        "MangaDex",
        "MangaReaderTo",
        "AllAnime"
    )
}

object HMangaSources : MangaReadSources() {

    override val list: List<Lazier<BaseParser>> = MangaSources.list

    override val names: List<String> = MangaSources.names

}