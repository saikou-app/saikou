package ani.saikou.anime.source

import ani.saikou.anime.source.AnimeSources.animeParsers
import ani.saikou.anime.source.parsers.*

object HAnimeSources : WatchSources() {
    override val names = arrayListOf(
        "HENTAIFF",
        "HAHO",
        "GOGO",
        "GOGO-DUB",
        "ANIMEKISA",
        "ANIMEKISA-DUB",
        "9ANIME",
        "9ANIME-DUB",
        "TENSHI",
        "ZORO",
        "TWIST",
    )

    private val hParsers:MutableMap<Int,AnimeParser> = mutableMapOf()
    override operator fun get(i:Int) : AnimeParser?{
        val a = when (i) {
            0 -> hParsers.getOrPut(i) { HentaiFF() }
            1 -> hParsers.getOrPut(i) { Haho() }
            2 -> animeParsers.getOrPut(i) { Gogo() }
            3 -> animeParsers.getOrPut(i) { Gogo(true) }
            4 -> animeParsers.getOrPut(i) { Animekisa() }
            5 -> animeParsers.getOrPut(i) { Animekisa(true) }
            6 -> animeParsers.getOrPut(i) { NineAnime() }
            7 -> animeParsers.getOrPut(i) { NineAnime(true) }
            8 -> animeParsers.getOrPut(i) { Tenshi() }
            9 -> animeParsers.getOrPut(i) { Zoro() }
            10 -> animeParsers.getOrPut(i) { Twist() }
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