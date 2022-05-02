package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.getSize
import ani.saikou.parsers.*
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.Requests
import java.net.URI

open class Tenshi : AnimeParser() {

    override val name: String = "Tenshi"
    override val saveName: String = "tenshi_moe"
    override val hostUrl: String = "https://tenshi.moe"
    override val isDubAvailableSeparately: Boolean = false

    private var cookieHeader = "Cookie" to "__ddg1_=;__ddg2_=;loop-view=thumb"

    override suspend fun loadEpisodes(animeLink: String): List<Episode> {
        val map = mutableMapOf<String, Episode>()
        val htmlResponse = client.get(animeLink, mapOf(cookieHeader)).document
        (1..htmlResponse.select(".entry-episodes > h2 > span.badge.badge-secondary.align-top").text().toInt()).forEach {
            val num = it.toString()
            map[num] = (Episode(num, "$animeLink/$num"))
        }
        htmlResponse.select(".episode-loop>li").forEach {
            val num = it.select(".episode-slug")[0].text().replace("Episode ", "")
            val link = "$animeLink/$num"
            val title = it.select(".episode-title")[0].text()
            val thumb = it.select("img")[0].attr("src")
            val desc = it.select("a[data-content]").attr("data-content")
            map[num] = Episode(num, link, title, FileUrl(thumb, mapOf(cookieHeader)), desc)
        }
        return map.values.toList()
    }

    override suspend fun loadVideoServers(episodeLink: String): List<VideoServer> {
        val htmlResponse = client.get(episodeLink, mapOf(cookieHeader)).document
        return htmlResponse.select("ul.dropdown-menu > li > a.dropdown-item").map {
            var server = it.text().replace(" ", "").replace("/-", "")
            val dub = it.select("[title=Audio: English]").first() != null
            server = if (dub) "Dub - $server" else server
            val headers = mapOf(cookieHeader, "referer" to episodeLink)
            val url = "$hostUrl/embed?" + URI(it.attr("href")).query
            VideoServer(server, FileUrl(url, headers))
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = TenshiVideoExtractor(server)

    override suspend fun search(query: String): List<ShowResponse> {
        val htmlResponse = client.get("$hostUrl/anime?q=$query&s=vtt-d", mapOf(cookieHeader))
        return htmlResponse.document.select("ul.loop.anime-loop.thumb > li > a").map {
            ShowResponse(
                it.attr("title"),
                it.attr("href"),
                FileUrl(it.select(".image")[0].attr("src"), mapOf(cookieHeader))
            )
        }
    }

    private class TenshiVideoExtractor(override val server: VideoServer) : VideoExtractor() {

        override suspend fun extract(): VideoContainer {
            val url = server.embed.url
            val headers = server.embed.headers

            val unSanitized = client.get(url, headers).text.substringAfter("player.source = ").substringBefore(';')

            val json = Requests.mapper.readValue<Player>(
                Regex("""([a-z0-9A-Z_]+): """)
                    .replace(unSanitized, "\"$1\" : ")
                    .replace('\'', '"').replace("\n", "")
                    .replace(" ", "").replace(",}", "}")
                    .replace(",]", "]")
            )

            return VideoContainer(json.sources.mapNotNull {
                if (it.src != null) {
                    val fileUrl = FileUrl(it.src, headers)
                    Video(it.size, false, fileUrl, getSize(fileUrl))
                } else null
            })
        }

        private data class Player(val sources: List<PlayerSource>) {
            data class PlayerSource(
                val size: Int? = null,
                val src: String? = null
            )
        }
    }
}