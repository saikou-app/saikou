package ani.saikou.manga.source

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

        return null
    }

    override fun flushLive() {
        mangaParsers.forEach {
            it.value.text = ""
        }
    }
}