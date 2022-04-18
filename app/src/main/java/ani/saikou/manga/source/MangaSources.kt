package ani.saikou.manga.source

import ani.saikou.manga.source.parsers.*

object MangaSources : MangaReadSources() {
    private val mangaParsers: MutableMap<Int, MangaParser> = mutableMapOf()

    override val names = arrayListOf(
        "MANGAKAKALOT",
        "MANGABUDDY",
        "MANGASEE",
        "MANGAPILL",
        "MANGADEX",
        "MANGAREADER",
    )

    override operator fun get(i: Int): MangaParser? {
        val a = when (i) {
            0 -> mangaParsers.getOrPut(i) { MangaKakaLot() }
            1 -> mangaParsers.getOrPut(i) { MangaBuddy() }
            2 -> mangaParsers.getOrPut(i) { MangaSee() }
            3 -> mangaParsers.getOrPut(i) { MangaPill() }
            4 -> mangaParsers.getOrPut(i) { MangaDex() }
            5 -> mangaParsers.getOrPut(i) { MangaReaderTo() }

            else -> null
        }
        return a
    }

    override fun flushLive() {
        mangaParsers.forEach {
            it.value.text = ""
        }
    }
}