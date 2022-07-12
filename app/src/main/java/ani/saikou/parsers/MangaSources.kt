package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.lazyList
import ani.saikou.parsers.manga.*

object MangaSources : MangaReadSources() {
    override val list: List<Lazier<BaseParser>> = lazyList(
        "MangaKakalot" to ::MangaKakalot,
        "MangaBuddy" to ::MangaBuddy,
        "MangaPill" to ::MangaPill,
        "MangaDex" to ::MangaDex,
        "MangaReaderTo" to ::MangaReaderTo,
        "AllAnime" to ::AllAnime,
        "ComickFun" to ::ComickFun,
        "MangaHub" to ::MangaHub,
        "MangaKatana" to ::MangaKatana,
        "MangaSee" to ::MangaSee,
        "Manhwa18" to ::Manhwa18
    )
}

object HMangaSources : MangaReadSources() {
    val aList: List<Lazier<BaseParser>> = lazyList(
        "NineHentai" to ::NineHentai,
        "NHentai" to ::NHentai,
    )
    override val list = listOf(aList,MangaSources.list).flatten()
}
