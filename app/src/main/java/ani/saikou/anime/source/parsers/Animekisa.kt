package ani.saikou.anime.source.parsers

import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.anime.source.extractors.VizCloud
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.logError
import java.net.URLEncoder

@Suppress("BlockingMethodInNonBlockingContext")
class Animekisa(private val dub: Boolean = false, override val name: String = "animekisa.in") :
    AnimeParser() {

    private val host = "https://animekisa.in/"

    override suspend fun getStream(episode: Episode, server: String): Episode {
        val streams = mutableMapOf<String, Episode.StreamLinks?>()
        try {
            httpClient.get(episode.link!!).document.select("#servers-list ul.nav li a").forEach { servers ->
                val embedLink = servers.attr("data-embed") // embed link of servers
                val name = servers.select("span").text()
                if (name == server) {
                    streams[name] = (VizCloud(host).getStreamLinks(name, embedLink))
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
        episode.streamLinks = streams
        return episode
    }

    override suspend fun getStreams(episode: Episode): Episode {
        val streams = mutableMapOf<String, Episode.StreamLinks?>()
        try {
            httpClient.get(episode.link!!).document.select("#servers-list ul.nav li a").forEach { servers ->
                val embedLink = servers.attr("data-embed") // embed link of servers
                val name = servers.select("span").text()
                streams[name] = (VizCloud(host).getStreamLinks(name, embedLink))
            }
        } catch (e: Exception) {
            logError(e)
        }
        episode.streamLinks = streams
        return episode
    }

    override suspend fun getEpisodes(media: Media): MutableMap<String, Episode> {
        var slug: Source? = loadData("animekisa_in${if (dub) "dub" else ""}_${media.id}")
        if (slug == null) {
            val it = media.nameMAL ?: media.name
            setTextListener("Searching for $it")
            logger("animekisa : Searching for $it")
            val search =
                search("$! | &language%5B%5D=${if (dub) "d" else "s"}ubbed&year%5B%5D=${media.anime?.seasonYear}&sort=default&season%5B%5D=${media.anime?.season?.lowercase()}&type%5B%5D=${media.typeMAL?.lowercase()}")
            if (search.isNotEmpty()) {
                search.sortByTitle(it)
                if (search.isNotEmpty()) {
                    slug = search[0]
                    saveSource(slug, media.id, false)
                }
            }
        } else {
            setTextListener("Selected : ${slug.name}")
        }
        if (slug != null) return getSlugEpisodes(slug.link)
        return mutableMapOf()
    }

    override suspend fun search(string: String): ArrayList<Source> {
        //THIS IS LIKE THE WORST SEARCH ENGINE OF A WEBSITE
        var url = URLEncoder.encode(string, "utf-8")
        if (string.startsWith("$!")) {
            val a = string.replace("$!", "").split(" | ")
            url = URLEncoder.encode(a[0], "utf-8") + a[1]
        }

        val responseArray = arrayListOf<Source>()
        try {
            httpClient.get("${host}filter?keyword=$url").document
                .select("#main-wrapper .film_list-wrap > .flw-item .film-poster").forEach {
                    val link = it.select("a").attr("href")
                    val title = it.select("img").attr("title")
                    val cover = it.select("img").attr("data-src")
                    responseArray.add(Source(link, title, cover))
                }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    override suspend fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val pageBody = httpClient.get(slug).document
            pageBody.select(".tab-pane > ul.nav").forEach {
                it.select("li>a").forEach { i ->
                    val num = i.text().trim()
                    responseArray[num] = Episode(number = num, link = i.attr("href").trim())
                }
            }
            logger("Response Episodes : $responseArray")
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("animekisa_in${if (dub) "dub" else ""}_$id", source)
    }
}