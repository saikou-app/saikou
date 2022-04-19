package ani.saikou.core.source.manga.rework

import ani.saikou.core.model.manga.MangaChapter
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source
import ani.saikou.core.source.IParser

/**
 * @author xtrm
 */
interface IMangaParser: IParser {
    fun getLinkChapters(link: String): Map<String, MangaChapter>
    fun getChapter(chapter: MangaChapter): MangaChapter
    fun getChapters(media: Media): Map<String, MangaChapter>

    /**
     * Searches for content in the given source.
     *
     * @param string The source to search in.
     */
    fun search(string: String): List<Source>

}