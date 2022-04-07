package ani.saikou.anime.source

import ani.saikou.anime.source.AnimeSources.animeParsers
import ani.saikou.anime.source.parsers.*

object HAnimeSources : WatchSources() {
    override val names = arrayListOf(
        "HAHO",
        "GOGO",
        "GOGO-DUB",
        "9ANIME",
        "9ANIME-DUB",
        "ZORO",
        "TWIST",
        "TENSHI",
            "MONOSCHINOS",
    )

    private val hParsers:MutableMap<Int,AnimeParser> = mutableMapOf()
    override operator fun get(i:Int) : AnimeParser?{
        val a = when (i) {
            0 -> hParsers.getOrPut(i) { Haho() }
            1 -> animeParsers.getOrPut(i) { Gogo() }
            2 -> animeParsers.getOrPut(i) { Gogo(true) }
            3 -> animeParsers.getOrPut(i) { NineAnime() }
            4 -> animeParsers.getOrPut(i) { NineAnime(true) }
            5 -> animeParsers.getOrPut(i) { Zoro() }
            6 -> animeParsers.getOrPut(i) { Twist() }
            7 -> animeParsers.getOrPut(i) { Tenshi() }
            8 -> animeParsers.getOrPut(i) { Monoschinos() }
            else -> null
        }
        return a
    }

    override fun flushLive() {
        hParsers.forEach{
            it.value.text = ""
        }
        animeParsers.forEach{
            it.value.text = ""
        }
    }
}