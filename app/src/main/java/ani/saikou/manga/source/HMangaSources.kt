package ani.saikou.manga.source

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
        return null
    }

    override fun flushLive() {
        mangaParsers.forEach {
            it.value.text = ""
        }
    }
}