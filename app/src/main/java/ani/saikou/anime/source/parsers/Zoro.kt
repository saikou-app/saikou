package ani.saikou.anime.source.parsers

import android.net.Uri
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.anime.source.Extractor
import ani.saikou.anime.source.extractors.RapidCloud
import ani.saikou.anime.source.extractors.StreamSB
import ani.saikou.anime.source.extractors.StreamTape
import ani.saikou.httpClient
import ani.saikou.loadData
import ani.saikou.logger
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.MalSyncBackup
import ani.saikou.others.asyncEach
import ani.saikou.others.logError
import ani.saikou.saveData
import org.jsoup.Jsoup
import java.net.URLEncoder

@Suppress("BlockingMethodInNonBlockingContext")
class Zoro(override val name: String = "Zoro", override val saveStreams: Boolean = false) : AnimeParser() {
    private val type = arrayOf("TV_SHORT", "MOVIE", "TV", "OVA", "ONA", "SPECIAL", "MUSIC")
    private val host = "https://zoro.to"

    private suspend fun directLinkify(name: String, url: String): Episode.StreamLinks? {
        val domain = Uri.parse(url).host ?: ""
        val extractor: Extractor? = when {
            "rapid" in domain -> RapidCloud()
            "sb" in domain    -> StreamSB()
            "streamta" in domain -> StreamTape()
            else              -> null
        }
        val a = extractor?.getStreamLinks(name, url)
        if (a != null && a.quality.isNotEmpty()) return a
        return null
    }

    data class SourceResponse (
        val link: String
    )

    override suspend fun getStream(episode: Episode, server: String): Episode {
        try {
            episode.streamLinks = let {
                val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()

                val res = httpClient.get("$host/ajax/v2/episode/servers?episodeId=${episode.link}").parsed<HtmlResponse>()
                val element = Jsoup.parse(res.html ?: return@let linkForVideos)
                element.select("div.server-item").asyncEach {
                    val serverName = "${it.attr("data-type").uppercase()} - ${it.text()}"
                    if (serverName  == server) {
                        val link = httpClient.get("$host/ajax/v2/episode/sources?id=${it.attr("data-id")}").parsed<SourceResponse>().link
                        val directLinks = directLinkify(serverName,link)
                        if (directLinks != null) {
                            linkForVideos[directLinks.server] = (directLinks)
                        }
                    }
                }
                linkForVideos
            }
        } catch (e: Exception) {
            logError(e)
        }
        return episode
    }

    override suspend fun getStreams(episode: Episode): Episode {
        try {
            episode.streamLinks = let {
                val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()

                val res = httpClient.get("$host/ajax/v2/episode/servers?episodeId=${episode.link}").parsed<HtmlResponse>()
                val element = Jsoup.parse(res.html ?: return@let linkForVideos)
                element.select("div.server-item").asyncEach {
                    val serverName = "${it.attr("data-type").uppercase()} - ${it.text()}"
                    val link = httpClient.get("$host/ajax/v2/episode/sources?id=${it.attr("data-id")}").parsed<SourceResponse>().link
                    val directLinks = directLinkify(serverName,link)
                    if (directLinks != null) {
                        linkForVideos[directLinks.server] = (directLinks)
                    }
                }
                linkForVideos
            }
        } catch (e: Exception) {
            logError(e)
        }
        return episode
    }

    override suspend fun getEpisodes(media: Media): MutableMap<String, Episode> {
        var slug: Source? = loadData("zoro_${media.id}")
        slug = slug ?: MalSyncBackup.get(media.id, "Zoro")?.also { saveSource(it, media.id, false) }
        if (slug == null) {
            val it = media.getMainName()
            setTextListener("Searching for $it")
            logger("Zoro : Searching for $it")
            val search = search("$!$it | &type=${type.indexOf(media.format)}")
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

    override suspend fun search(string: String): ArrayList<Source> {
        val responseArray = arrayListOf<Source>()
        try {
            var url = URLEncoder.encode(string, "utf-8")
            if (string.startsWith("$!")) {
                val a = string.replace("$!", "").split(" | ")
                url = URLEncoder.encode(a[0], "utf-8") + a[1]
            }
            httpClient.get("${host}/search?keyword=$url").document.select(".film_list-wrap > .flw-item > .film-poster").forEach {
                val link = it.select("a").attr("data-id")
                val title = it.select("a").attr("title")
                val cover = it.select("img").attr("data-src")
                responseArray.add(Source(link, title, cover))
            }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    data class HtmlResponse (
        val status: Boolean,
        val html: String?=null,
    )

    override suspend fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val res = httpClient.get("$host/ajax/v2/episode/list/$slug").parsed<HtmlResponse>()
            val element = Jsoup.parse(res.html ?: return responseArray)
            element.select(".detail-infor-content > div > a").forEach {
                val title = it.attr("title")
                val num = it.attr("data-number").replace("\n", "")
                val id = it.attr("data-id")
                val filler = it.attr("class").contains("ssl-item-filler")

                responseArray[num] = Episode(number = num, link = id, title = title, filler = filler, saveStreams = false)
            }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("zoro_$id", source)
    }
}