package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.manga.*

object MangaSources : MangaReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "MangaKakalot" to ::MangaKakalot,
        "MangaBuddy" to ::MangaBuddy,
        "MangaSee" to ::MangaSee,
        "MangaPill" to ::MangaPill,
        "MangaDex" to ::MangaDex,
        "MangaReaderTo" to ::MangaReaderTo,
        "AllAnime" to ::AllAnime,
        "Comickfun" to ::Comickfun,
    )
}

object HMangaSources : MangaReadSources() {
    val aList: List<Lazier<BaseParser>> = lazyList(
        "NHentai" to ::NHentai,
        "NineHentai" to ::NineHentai,
    )
    override val list = listOf(aList,MangaSources.list).flatten()
}
