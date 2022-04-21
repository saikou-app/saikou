package ani.saikou.anime.source.parsers

import ani.saikou.anime.Episode
import ani.saikou.anime.newsrc.AnimeProvider
import ani.saikou.loadData
import ani.saikou.logger
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.saveData
import ani.saikou.toastString
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI

open class Tenshi : AnimeProvider() {
    override val name: String = "tenshi.moe"
    override val isAdult: Boolean = true

    protected val cookie = "__ddg1_=;__ddg2_=;loop-view=thumb"
    private val httpClient = OkHttpClient()

    override fun fetchVideoServer(episode: Episode, server: String): Map<String, Episode.VideoServer> = runBlocking {
        val videoServers = mutableMapOf<String, Episode.VideoServer>()

        val htmlResponse = httpClient.newCall(
            Request.Builder().url(episode.link!!)
                .header("Cookie", cookie).build()
        ).execute().body!!.string()
        Jsoup.parse(htmlResponse).select("ul.dropdown-menu > li > a.dropdown-item").forEach { element ->
            launch {
                val a = element.text().replace(" ", "").replace("/-", "")
                if (server == a) {
                    val (name, videoServer) = findVideoServer(episode, element)
                    videoServers[name] = videoServer
                }
            }
        }

        videoServers
    }

    override fun fetchVideoServers(episode: Episode): Map<String, Episode.VideoServer> = runBlocking {
        val videoServers = mutableMapOf<String, Episode.VideoServer>()

        val htmlResponse = httpClient.newCall(
            Request.Builder().url(episode.link!!)
                .header("Cookie", cookie).build()
        ).execute().body!!.string()
        Jsoup.parse(htmlResponse).select("ul.dropdown-menu > li > a.dropdown-item").forEach { element ->
            launch {
                val (name, server) = findVideoServer(episode, element)
                videoServers[name] = server
            }
        }

        videoServers
    }

    open fun findVideoServer(episode: Episode, it: Element): Pair<String, Episode.VideoServer> {
        val server = it.text().replace(" ", "").replace("/-", "")
        val headers = mutableMapOf("cookie" to cookie, "referer" to episode.link!!)
        val url = "https://$name/embed?" + URI(it.attr("href")).query

        val unSanitized = httpClient.newCall(
            Request.Builder()
                .url(url)
                .headers(headers.toHeaders())
                .build()
        ).execute().body!!.string().substringAfter("player.source = ").substringBefore(';')

        val json = Json.decodeFromString<JsonObject>(
            Regex("""([a-z0-9A-Z_]+): """, RegexOption.DOT_MATCHES_ALL)
                .replace(unSanitized, "\"$1\" : ")
                .replace('\'', '"')
                .replace("\n", "").replace(" ", "").replace(",}", "}").replace(",]", "]")
        )

        val a = arrayListOf<Deferred<*>>()

        val qualities = arrayListOf<Episode.VideoQuality>()
        runBlocking {
            json["sources"]?.jsonArray?.forEach { i ->
                a.add(async {
                    val uri = i.jsonObject["src"]?.toString()?.trim('"')
                    if (uri != null)
                        qualities.add(
                            Episode.VideoQuality(
                                videoUrl = uri,
                                quality = i.jsonObject["size"].toString() + "p",
                                size = null
                            )
                        )
                })
            }
            a.awaitAll()
        }

        return Pair(server, Episode.VideoServer(server, qualities, headers))
    }

    override fun getEpisodes(media: Media): Map<String, Episode> {
        try {
            var source: Source? = loadData("tenshi_${media.id}")
            if (source == null) {
                fun s(it: String): Boolean {
                    updateStatus("Searching for $it")
                    logger("Tenshi : Searching for $it")
                    val search = search(it)
                    if (search.isNotEmpty()) {
                        source = search[0]
                        saveSource(source!!, media.id, false)
                        return true
                    }
                    return false
                }
                if (!s(media.nameMAL ?: media.name))
                    s(media.nameRomaji)
            } else {
                updateStatus("Selected : ${source!!.name}")
            }
            if (source != null) {
                return getEpisodes(source!!.id)
            }
        } catch (e: Exception) {
            toastString("$e")
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        logger("Searching for : $string")
        val responseArray = arrayListOf<Source>()
        try {
            val htmlResponse = httpClient.newCall(
                Request.Builder().url("https://$name/anime?q=$string&s=vtt-d")
                    .header("Cookie", cookie).build()
            ).execute().body!!.string()
            Jsoup.parse(htmlResponse).select("ul.loop.anime-loop.thumb > li > a").forEach {
                responseArray.add(
                    Source(
                        id = it.attr("abs:href"),
                        name = it.attr("title"),
                        cover = it.select(".image")[0].attr("src"),
                        headers = mutableMapOf("Cookie" to cookie)
                    )
                )
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun getEpisodes(animeId: String): Map<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val htmlResponse = httpClient.newCall(
                Request.Builder().url(animeId)
                    .header("Cookie", cookie).build()
            ).execute().body!!.string()

            val nbEp = Jsoup.parse(htmlResponse)
                .select(".entry-episodes > h2 > span.badge.badge-secondary.align-top")
                .text().toInt()

            for (i in 1..nbEp) {
                responseArray["$i"] = Episode("$i", link = "${animeId}/$i")
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("tenshi_$id", source)
    }
}