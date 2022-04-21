package ani.saikou.anime.source.parsers

import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.newsrc.AnimeProvider
import ani.saikou.media.Media
import ani.saikou.media.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

object Zoro : AnimeProvider() {
    override val name: String = "Zoro"
    override val isAdult: Boolean = false

    private val type = arrayOf("TV_SHORT", "MOVIE", "TV", "OVA", "ONA", "SPECIAL", "MUSIC")
    private const val host = "https://zoro.to"

    override fun fetchVideoServer(episode: Episode, server: String): Map<String, Episode.VideoServer> =
        runBlocking {
            val linkForVideos = mutableMapOf<String, Episode.VideoServer>()
            withContext(Dispatchers.Default) {
                val res =
                    Jsoup.connect("$host/ajax/v2/episode/servers?episodeId=${episode.link}").ignoreContentType(true).execute()
                        .body().replace("\\n", "\n").replace("\\\"", "\"")
                val element =
                    Jsoup.parse(res.findBetween("""{"status":true,"html":"""", """"}""") ?: return@withContext episode)
                element.select("div.server-item").forEach {
                    if ("${it.attr("data-type").uppercase()} - ${it.text()}" == server) {
                        val resp =
                            Jsoup.connect("$host/ajax/v2/episode/sources?id=${it.attr("data-id")}").ignoreContentType(true)
                                .execute().body().replace("\\n", "\n").replace("\\\"", "\"")
                        launch {
                            val link = resp.findBetween(""""link":"""", """","server"""") ?: return@launch
                            val videoServer = resolveVideoServer("${it.attr("data-type").uppercase()} - ${it.text()}", link)
                            if (videoServer != null) {
                                linkForVideos[videoServer.serverName] = videoServer
                            }
                        }
                    }
                }
            }
            return@runBlocking (linkForVideos)
        }

    override fun fetchVideoServers(episode: Episode): Map<String, Episode.VideoServer> =
        runBlocking {
            val linkForVideos = mutableMapOf<String, Episode.VideoServer>()
            withContext(Dispatchers.Default) {
                val res =
                    Jsoup.connect("$host/ajax/v2/episode/servers?episodeId=${episode.link}").ignoreContentType(true).execute()
                        .body().replace("\\n", "\n").replace("\\\"", "\"")
                val element =
                    Jsoup.parse(res.findBetween("""{"status":true,"html":"""", """"}""") ?: return@withContext episode)
                element.select("div.server-item").forEach {
                    val resp = Jsoup.connect("$host/ajax/v2/episode/sources?id=${it.attr("data-id")}").ignoreContentType(true)
                        .execute().body().replace("\\n", "\n").replace("\\\"", "\"")
                    launch {
                        val link = resp.findBetween(""""link":"""", """","server"""") ?: return@launch
                        val videoServer = resolveVideoServer("${it.attr("data-type").uppercase()} - ${it.text()}", link)
                        if (videoServer != null) {
                            linkForVideos[videoServer.serverName] = (videoServer)
                        }
                    }
                }
            }
            return@runBlocking (linkForVideos)
        }

    override fun getEpisodes(media: Media): Map<String, Episode> {
        var slug: Source? = loadData("zoro_${media.id}")
        if (slug == null) {
            val it = media.nameRomaji
            updateStatus("Searching for $it")
            logger("Zoro : Searching for $it")
            val search = search("$!${media.nameRomaji} | &type=${type.indexOf(media.format)}")
            if (search.isNotEmpty()) {
                slug = search[0]
                saveSource(slug, media.id, false)
            }
        } else {
            updateStatus("Selected : ${slug.name}")
        }
        if (slug != null) {
            return getEpisodes(slug.link)
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        val responseArray = arrayListOf<Source>()
        try {
            var url = URLEncoder.encode(string, "utf-8")
            if (string.startsWith("$!")) {
                val a = string.replace("$!", "").split(" | ")
                url = URLEncoder.encode(a[0], "utf-8") + a[1]
            }
            Jsoup.connect("${host}/search?keyword=$url").get().select(".film_list-wrap > .flw-item > .film-poster").forEach {
                val link = it.select("a").attr("data-id")
                val title = it.select("a").attr("title")
                val cover = it.select("img").attr("data-src")
                responseArray.add(Source(link, title, cover))
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun getEpisodes(animeId: String): Map<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val res =
                Jsoup.connect("$host/ajax/v2/episode/list/$animeId").ignoreContentType(true).execute().body().replace("\\n", "\n")
                    .replace("\\\"", "\"")
            val element =
                Jsoup.parse(res.findBetween("""{"status":true,"html":"""", """","totalItems"""") ?: return responseArray)
            element.select(".detail-infor-content > div > a").forEach {
                val title = it.attr("title")
                val num = it.attr("data-number").replace("\n", "")
                val id = it.attr("data-id")
                val filler = it.attr("class").contains("ssl-item-filler")

                responseArray[num] = Episode(number = num, link = id, title = title, filler = filler, saveStreams = false)
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("zoro_$id", source)
    }
}