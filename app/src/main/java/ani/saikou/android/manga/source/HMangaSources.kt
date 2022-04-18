package ani.saikou.android.manga.source

import ani.saikou.android.manga.source.parsers.*

object HMangaSources : MangaReadSources() {
    private val mangaParsers: MutableMap<Int, MangaParser> = mutableMapOf()

    override val names = arrayListOf(
        "MANGABUDDY",
        "MANGASEE",
        "MANGAPILL",
        "MANGADEX",
        "MANGAREADER",
        "MANGAKAKALOT",
    )

    override operator fun get(i: Int): MangaParser? {
        val a = when (i) {
            0 -> mangaParsers.getOrPut(i) { MangaBuddy() }
            1 -> mangaParsers.getOrPut(i) { MangaSee() }
            2 -> mangaParsers.getOrPut(i) { MangaPill() }
            3 -> mangaParsers.getOrPut(i) { MangaDex() }
            4 -> mangaParsers.getOrPut(i) { MangaReaderTo() }
            5 -> mangaParsers.getOrPut(i) { MangaKakaLot() }
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