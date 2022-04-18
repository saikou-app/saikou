package ani.saikou.android.anime.source.parsers

import android.net.Uri
import ani.saikou.android.anime.source.AnimeParser
import ani.saikou.android.anime.source.Extractor
import ani.saikou.android.anime.source.extractors.RapidCloud
import ani.saikou.android.anime.source.extractors.StreamSB
import ani.saikou.core.model.anime.Episode
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source
import ani.saikou.core.utils.extension.findBetween
import ani.saikou.android.loadData
import ani.saikou.android.logger
import ani.saikou.android.saveData
import ani.saikou.android.toastString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class Zoro(override val name: String = "Zoro", override val saveStreams: Boolean = false) :
    AnimeParser() {
    private val type = arrayOf("TV_SHORT", "MOVIE", "TV", "OVA", "ONA", "SPECIAL", "MUSIC")
    private val host = "https://zoro.to"

    private fun directLinkify(name: String, url: String): Episode.StreamLinks? {
        val domain = Uri.parse(url).host ?: ""
        val extractor: Extractor? = when {
            "rapid" in domain -> RapidCloud()
            "sb" in domain -> StreamSB()
            else -> null
        }
        val a = extractor?.getStreamLinks(name, url)
        if (a != null && a.quality.isNotEmpty()) return a
        return null
    }

    override fun getStream(episode: Episode, server: String): Episode {
        try {
            episode.streamLinks = runBlocking {
                val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()
                withContext(Dispatchers.Default) {
                    val res =
                        Jsoup.connect("$host/ajax/v2/episode/servers?episodeId=${episode.link}")
                            .ignoreContentType(true).execute().body().replace("\\n", "\n")
                            .replace("\\\"", "\"")
                    val element = Jsoup.parse(
                        res.findBetween("""{"status":true,"html":"""", """"}""")
                            ?: return@withContext episode
                    )
                    element.select("div.server-item").forEach {
                        if ("${it.attr("data-type").uppercase()} - ${it.text()}" == server) {
                            val resp =
                                Jsoup.connect("$host/ajax/v2/episode/sources?id=${it.attr("data-id")}")
                                    .ignoreContentType(true).execute().body().replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                            launch {
                                val link = resp.findBetween(""""link":"""", """","server"""")
                                    ?: return@launch
                                val directLinks = directLinkify(
                                    "${
                                        it.attr("data-type").uppercase()
                                    } - ${it.text()}", link
                                )
                                if (directLinks != null) {
                                    linkForVideos[directLinks.server] = (directLinks)
                                }
                            }
                        }
                    }
                }
                return@runBlocking (linkForVideos)
            }
        } catch (e: Exception) {
            toastString("$e")
        }
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        try {
            episode.streamLinks = runBlocking {
                val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()
                withContext(Dispatchers.Default) {
                    val res =
                        Jsoup.connect("$host/ajax/v2/episode/servers?episodeId=${episode.link}")
                            .ignoreContentType(true).execute().body().replace("\\n", "\n")
                            .replace("\\\"", "\"")
                    val element = Jsoup.parse(
                        res.findBetween("""{"status":true,"html":"""", """"}""")
                            ?: return@withContext episode
                    )
                    element.select("div.server-item").forEach {
                        val resp =
                            Jsoup.connect("$host/ajax/v2/episode/sources?id=${it.attr("data-id")}")
                                .ignoreContentType(true).execute().body().replace("\\n", "\n")
                                .replace("\\\"", "\"")
                        launch {
                            val link =
                                resp.findBetween(""""link":"""", """","server"""") ?: return@launch
                            val directLinks = directLinkify(
                                "${it.attr("data-type").uppercase()} - ${it.text()}",
                                link
                            )
                            if (directLinks != null) {
                                linkForVideos[directLinks.server] = (directLinks)
                            }
                        }
                    }
                }
                return@runBlocking (linkForVideos)
            }
        } catch (e: Exception) {
            toastString("$e")
        }
        return episode
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        var slug: Source? = loadData("zoro_${media.id}")
        if (slug == null) {
            val it = media.nameRomaji
            setTextListener("Searching for $it")
            logger("Zoro : Searching for $it")
            val search = search("$!${media.nameRomaji} | &type=${type.indexOf(media.format)}")
            if (search.isNotEmpty()) {
                slug = search[0]
                saveSource(slug, media.id, false)
            }
        } else {
            setTextListener("Selected : ${slug.name}")
        }
        if (slug != null) return getSlugEpisodes(slug.link)
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
            Jsoup.connect("${host}/search?keyword=$url").get()
                .select(".film_list-wrap > .flw-item > .film-poster").forEach {
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

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val res =
                Jsoup.connect("$host/ajax/v2/episode/list/$slug").ignoreContentType(true).execute()
                    .body().replace("\\n", "\n").replace("\\\"", "\"")
            val element = Jsoup.parse(
                res.findBetween("""{"status":true,"html":"""", """","totalItems"""")
                    ?: return responseArray
            )
            element.select(".detail-infor-content > div > a").forEach {
                val title = it.attr("title")
                val num = it.attr("data-number").replace("\n", "")
                val id = it.attr("data-id")
                val filler = it.attr("class").contains("ssl-item-filler")

                responseArray[num] = Episode(
                    number = num,
                    link = id,
                    title = title,
                    filler = filler,
                    saveStreams = false
                )
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