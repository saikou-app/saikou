package ani.saikou.parsers.anime

import android.net.Uri
import ani.saikou.FileUrl
import ani.saikou.asyncMap
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.RapidCloud
import ani.saikou.parsers.anime.extractors.StreamSB
import ani.saikou.parsers.anime.extractors.StreamTape
import org.jsoup.Jsoup
import java.net.URLEncoder

@Suppress("BlockingMethodInNonBlockingContext")
class Zoro : AnimeParser() {

    override val name: String = "Zoro"
    override val saveName: String = "zoro_to"
    override val hostUrl: String = "https://zoro.to"
    override val isDubAvailableSeparately: Boolean = false

    override suspend fun loadEpisodes(animeLink: String): List<Episode> {
        val res = client.get("$hostUrl/ajax/v2/episode/list/$animeLink").parsed<HtmlResponse>()
        val element = Jsoup.parse(res.html ?: return emptyList())
        return element.select(".detail-infor-content > div > a").map {
            val title = it.attr("title")
            val num = it.attr("data-number").replace("\n", "")
            val id = it.attr("data-id")
            val filler = it.attr("class").contains("ssl-item-filler")

            Episode(number = num, link = id, title = title, isFiller = filler)
        }
    }

    private val embedHeaders = mapOf("referer" to "$hostUrl/")

    override suspend fun loadVideoServers(episodeLink: String): List<VideoServer> {
        val res = client.get("$hostUrl/ajax/v2/episode/servers?episodeId=$episodeLink").parsed<HtmlResponse>()
        val element = Jsoup.parse(res.html ?: return emptyList())

        return element.select("div.server-item").asyncMap {
            val serverName = "${it.attr("data-type").uppercase()} - ${it.text()}"
            val link = client.get("$hostUrl/ajax/v2/episode/sources?id=${it.attr("data-id")}").parsed<SourceResponse>().link
            VideoServer(serverName, FileUrl(link,embedHeaders))
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val domain = Uri.parse(server.embed.url).host ?: return null
        val extractor: VideoExtractor? = when {
            "rapid" in domain    -> RapidCloud(server)
            "sb" in domain       -> StreamSB(server)
            "streamta" in domain -> StreamTape(server)
            else                 -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {

        var url = URLEncoder.encode(query, "utf-8")
        if (query.startsWith("$!")) {
            val a = query.replace("$!", "").split(" | ")
            url = URLEncoder.encode(a[0], "utf-8") + a[1]
        }

        val document = client.get("${hostUrl}/search?keyword=$url").document

        return document.select(".film_list-wrap > .flw-item > .film-poster").map {
            val link = it.select("a").attr("data-id")
            val title = it.select("a").attr("title")
            val cover = it.select("img").attr("data-src")
            ShowResponse(title, link, FileUrl(cover))
        }
    }

    data class SourceResponse(
        val link: String
    )

    private data class HtmlResponse(
        val status: Boolean,
        val html: String? = null,
    )

}