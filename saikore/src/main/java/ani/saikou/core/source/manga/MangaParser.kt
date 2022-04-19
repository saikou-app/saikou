package ani.saikou.core.source.manga

import ani.saikou.core.model.manga.MangaChapter
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source

abstract class MangaParser {
    abstract val name: String
    var text = ""
    var textListener: ((String) -> Unit) = {}

    abstract fun getLinkChapters(link: String): MutableMap<String, MangaChapter>

    abstract fun getChapter(chapter: MangaChapter): MangaChapter

    abstract fun getChapters(media: Media): MutableMap<String, MangaChapter>

    abstract fun search(string: String): List<Source>

    open fun saveSource(source: Source, id: Int, selected: Boolean = true) {
        setTextListener("${if (selected) "Selected" else "Found"} : ${source.name}")
    }

    fun setTextListener(string: String) {
        text = string
        textListener.invoke(text)
    }
}