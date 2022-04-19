package ani.saikou.core.source.anime.parsers

import ani.saikou.core.model.anime.Episode
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source
import ani.saikou.core.service.LOG
import ani.saikou.core.service.STORE
import ani.saikou.core.source.anime.AnimeParser
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
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

open class Tenshi(override val name: String = "tenshi.moe") : AnimeParser() {
    var cookie = "__ddg1_=;__ddg2_=;loop-view=thumb"
    private val httpClient = OkHttpClient()

    override fun getStream(episode: Episode, server: String): Episode {
        try {
            runBlocking {
                val asy = arrayListOf<Deferred<*>>()
                episode.streamLinks = mutableMapOf()
                val htmlResponse = httpClient.newCall(
                    Request.Builder().url(episode.link!!)
                        .header("Cookie", "__ddg1_=;__ddg2_=;loop-view=thumb").build()
                ).execute().body!!.string()
                Jsoup.parse(htmlResponse).select("ul.dropdown-menu > li > a.dropdown-item")
                    .forEach {
                        asy.add(async {
                            val a = it.text().replace(" ", "").replace("/-", "")
                            if (server == a)
                                load(episode, it)
                        })
                    }
                asy.awaitAll()
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        try {
            runBlocking {
                val asy = arrayListOf<Deferred<*>>()
                episode.streamLinks = mutableMapOf()
                val htmlResponse = httpClient.newCall(
                    Request.Builder().url(episode.link!!)
                        .header("Cookie", "__ddg1_=;__ddg2_=;loop-view=thumb").build()
                ).execute().body!!.string()
                Jsoup.parse(htmlResponse).select("ul.dropdown-menu > li > a.dropdown-item")
                    .forEach {
                        asy.add(async {
                            load(episode, it)
                        })
                    }
                asy.awaitAll()
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return episode
    }

    open fun load(episode: Episode, it: Element) {
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

        val qualities = arrayListOf<Episode.Quality>()
        runBlocking {
            json["sources"]?.jsonArray?.forEach { i ->
                a.add(async {
                    val uri = i.jsonObject["src"]?.toString()?.trim('"')
                    if (uri != null)
                        qualities.add(
                            Episode.Quality(
                                url = uri,
                                quality = i.jsonObject["size"].toString() + "p",
                                size = null
                            )
                        )
                })
            }
            a.awaitAll()
        }
        episode.streamLinks[server] = Episode.StreamLinks(server, qualities, headers)
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        try {
            var slug: Source? = STORE.loadData("tenshi_${media.id}")
            if (slug == null) {
                fun s(it: String): Boolean {
                    setTextListener("Searching for $it")
                    LOG.log("Tenshi : Searching for $it")
                    val search = search(it)
                    if (search.isNotEmpty()) {
                        slug = search[0]
                        saveSource(slug!!, media.id, false)
                        return true
                    }
                    return false
                }
                if (!s(media.nameMAL ?: media.name))
                    s(media.nameRomaji)
            } else {
                setTextListener("Selected : ${slug!!.name}")
            }
            if (slug != null) return getSlugEpisodes(slug!!.link)
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        LOG.log("Searching for : $string")
        val responseArray = arrayListOf<Source>()
        try {
            val htmlResponse = httpClient.newCall(
                Request.Builder().url("https://$name/anime?q=$string&s=vtt-d")
                    .header("Cookie", cookie).build()
            ).execute().body!!.string()
            Jsoup.parse(htmlResponse).select("ul.loop.anime-loop.thumb > li > a").forEach {
                responseArray.add(
                    Source(
                        link = it.attr("abs:href"),
                        name = it.attr("title"),
                        cover = it.select(".image")[0].attr("src"),
                        headers = mutableMapOf("Cookie" to cookie)
                    )
                )
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return responseArray
    }

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val htmlResponse = httpClient.newCall(
                Request.Builder().url(slug)
                    .header("Cookie", cookie).build()
            ).execute().body!!.string()
            (1..Jsoup.parse(htmlResponse)
                .select(".entry-episodes > h2 > span.badge.badge-secondary.align-top").text()
                .toInt()).forEach {
                responseArray[it.toString()] = Episode(it.toString(), link = "${slug}/$it")
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        STORE.saveData("tenshi_$id", source)
    }
}