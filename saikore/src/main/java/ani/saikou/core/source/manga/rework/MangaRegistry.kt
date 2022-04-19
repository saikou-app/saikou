package ani.saikou.core.source.manga.rework

/**
 * Registry of [IMangaParser]s.
 *
 * @author xtrm
 */
object MangaRegistry {
    val parsers: MutableList<IMangaParser> =
        mutableListOf()

    init {
        // add default parsers
        parsers.apply {

        }
    }
}