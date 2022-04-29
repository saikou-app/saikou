package ani.saikou.anime.source

import ani.saikou.anime.Episode
import ani.saikou.media.Media
import ani.saikou.media.Source

abstract class AnimeParser {
    abstract val name: String
    open val saveStreams = true
    var text = ""
    var textListener: ((String) -> Unit)? = null
    abstract suspend fun getStream(episode: Episode, server: String): Episode
    abstract suspend fun getStreams(episode: Episode): Episode
    abstract suspend fun getEpisodes(media: Media): MutableMap<String, Episode>
    abstract suspend fun search(string: String): ArrayList<Source>
    abstract suspend fun getSlugEpisodes(slug: String): MutableMap<String, Episode>
    open fun saveSource(source: Source, id: Int, selected: Boolean = true) {
        setTextListener("${if (selected) "Selected" else "Found"} : ${source.name}")
    }

    fun setTextListener(string: String) {
        text = string
        textListener?.invoke(text)
    }
}