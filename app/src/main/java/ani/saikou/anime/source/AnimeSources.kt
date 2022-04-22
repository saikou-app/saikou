package ani.saikou.anime.source

import ani.saikou.anime.source.parsers.*

object AnimeSources : WatchSources() {
    override val names = arrayListOf(
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

    val animeParsers:MutableMap<Int,AnimeParser> = mutableMapOf()
    override operator fun get(i:Int) : AnimeParser?{
        val a = when (i) {
            0 -> animeParsers.getOrPut(i) { Gogo() }
            1 -> animeParsers.getOrPut(i) { Gogo(true) }
            2 -> animeParsers.getOrPut(i) { Animekisa() }
            3 -> animeParsers.getOrPut(i) { Animekisa(true) }
            4 -> animeParsers.getOrPut(i) { NineAnime() }
            5 -> animeParsers.getOrPut(i) { NineAnime(true) }
            6 -> animeParsers.getOrPut(i) { Tenshi() }
            7 -> animeParsers.getOrPut(i) { Zoro() }
            8 -> animeParsers.getOrPut(i) { Twist() }
            else -> null
        }
        return a
    }
    override fun flushLive(){
        animeParsers.forEach{
            it.value.text = ""
        }
    }
}