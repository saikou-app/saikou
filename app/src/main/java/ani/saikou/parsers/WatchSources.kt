package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.anime.Episode
import ani.saikou.media.Media
import ani.saikou.tryWithSuspend

abstract class WatchSources : BaseSources() {

    override operator fun get(i: Int): AnimeParser {
        return list[i].get.value as AnimeParser
    }


    suspend fun loadEpisodesFromMedia(i: Int, media: Media): MutableMap<String, Episode> {
        return tryWithSuspend {
            val res = get(i).autoSearch(media) ?: return@tryWithSuspend mutableMapOf()
            loadEpisodes(i, res.link)
        } ?: mutableMapOf()
    }

    suspend fun loadEpisodes(i: Int, showLink: String): MutableMap<String, Episode> {
        val map = mutableMapOf<String, Episode>()
        val parser = get(i)
        tryWithSuspend {
            parser.loadEpisodes(showLink).forEach {
                map[it.number] = Episode(it.number, it.link, it.title, it.description, it.thumbnail, it.isFiller)
            }
        }
        return map
    }

}

abstract class BaseSources {
    abstract val list: List<Lazier<BaseParser>>

    abstract val names: List<String>

    fun flushText() {
        list.forEach {
            if (it.get.isInitialized())
                it.get.value.showUserText = ""
        }
    }

    open operator fun get(i: Int): BaseParser {
        return list[i].get.value
    }

    fun saveResponse(i: Int, mediaId: Int, response: ShowResponse) {
        get(i).saveShowResponse(mediaId, response)
    }
}



