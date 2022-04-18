package ani.saikou.manga.source

import ani.saikou.manga.MangaChapter
import ani.saikou.media.Media
import ani.saikou.media.Source

abstract class MangaParser {
    abstract val name: String
    var text = ""
    var textListener: ((String) -> Unit)? = null
    abstract fun getLinkChapters(link: String): MutableMap<String, MangaChapter>
    abstract fun getChapter(chapter: MangaChapter): MangaChapter
    abstract fun getChapters(media: Media): MutableMap<String, MangaChapter>
    abstract fun search(string: String): ArrayList<Source>
    open fun saveSource(source: Source, id: Int, selected: Boolean = true) {
        setTextListener("${if (selected) "Selected" else "Found"} : ${source.name}")
    }

    fun setTextListener(string: String) {
        text = string
        textListener?.invoke(text)
    }
}