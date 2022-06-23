package ani.saikou.parsers

import ani.saikou.Lazier
import ani.saikou.anime.Episode
import ani.saikou.manga.MangaChapter
import ani.saikou.media.Media
import ani.saikou.tryWithSuspend

abstract class WatchSources : BaseSources() {

    override operator fun get(i: Int): AnimeParser {
        return list[i].get.value as AnimeParser
    }

    suspend fun loadEpisodesFromMedia(i: Int, media: Media): MutableMap<String, Episode> {
        return tryWithSuspend {
            val res = get(i).autoSearch(media) ?: return@tryWithSuspend mutableMapOf()
            loadEpisodes(i, res.link, res.extra)
        } ?: mutableMapOf()
    }

    suspend fun loadEpisodes(i: Int, showLink: String, extra: Map<String, String>?): MutableMap<String, Episode> {
        val map = mutableMapOf<String, Episode>()
        val parser = get(i)
        tryWithSuspend {
            parser.loadEpisodes(showLink,extra).forEach {
                map[it.number] = Episode(it.number, it.link, it.title, it.description, it.thumbnail, it.isFiller, extra = it.extra)
            }
        }
        return map
    }

}

abstract class MangaReadSources : BaseSources() {

    override operator fun get(i: Int): MangaParser {
        return list[i].get.value as MangaParser
    }

    suspend fun loadChaptersFromMedia(i: Int, media: Media): MutableMap<String, MangaChapter> {
        return tryWithSuspend {
            val res = get(i).autoSearch(media) ?: return@tryWithSuspend mutableMapOf()
            loadChapters(i, res)
        } ?: mutableMapOf()
    }

    suspend fun loadChapters(i: Int, show: ShowResponse): MutableMap<String, MangaChapter> {
        val map = mutableMapOf<String, MangaChapter>()
        val parser = get(i)
        tryWithSuspend {
            parser.loadChapters(show.link, show.extra).forEach {
                map[it.number] = MangaChapter(it)
            }
        }
        return map
    }
}

abstract class BaseSources {
    abstract val list: List<Lazier<BaseParser>>

    val names: List<String> get() = list.map { it.name }

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
        get(i).saveShowResponse(mediaId, response, true)
    }
}



