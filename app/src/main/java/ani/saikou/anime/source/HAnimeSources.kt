package ani.saikou.anime.source

import ani.saikou.anime.source.AnimeSources.animeParsers
import ani.saikou.anime.source.providers.*

object HAnimeSources : WatchSources() {
    override val names = arrayListOf(
        "HENTAIFF",
        "HAHO",
        "GOGO",
        "GOGO-DUB",
        "9ANIME",
        "9ANIME-DUB",
        "ZORO",
        "TWIST",
        "TENSHI",
    )

    private val hParsers: MutableMap<Int, AnimeParser> = mutableMapOf()

    override operator fun get(i: Int): AnimeParser? {
        val a = when (i) {
            0    -> hParsers.getOrPut(i) { HentaiFF() }
            1    -> hParsers.getOrPut(i) { Haho() }
            2    -> animeParsers.getOrPut(i) { Gogo() }
            3    -> animeParsers.getOrPut(i) { Gogo(true) }
            4    -> animeParsers.getOrPut(i) { NineAnime() }
            5    -> animeParsers.getOrPut(i) { NineAnime(true) }
            6    -> animeParsers.getOrPut(i) { Zoro() }
            7    -> animeParsers.getOrPut(i) { Twist() }
            8    -> animeParsers.getOrPut(i) { Tenshi() }
            else -> null
        }
        return a
    }

    override fun flushLive() {
        hParsers.forEach {
            it.value.text = ""
        }
        animeParsers.forEach {
            it.value.text = ""
        }
    }
}