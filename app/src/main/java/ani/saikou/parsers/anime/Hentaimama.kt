package ani.saikou.parsers.anime


import ani.saikou.client
import ani.saikou.parsers.*
import org.json.JSONArray

class Hentaimama : AnimeParser() {
    override val name = "Hentaimama"
    override val saveName = "hentai_mama"
    override val hostUrl = "https://hentaimama.io"
    override val isDubAvailableSeparately = false
    override val isNSFW = true

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val pageBody = client.get(animeLink).document
        return pageBody.select("div#episodes.sbox.fixidtab div.module.series div.content.series div.items article").map {
            val epNum = it.select("div.data h3").text().replace("Episode","")
            val url = it.select("div.poster div.season_m.animation-3 a").attr("href")
            val thumb1 = it.select("div.poster img").attr("data-src")
            Episode(epNum,url,thumbnail = thumb1)
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer>  {
        val servers = mutableListOf<VideoServer>()
        val animeId = client.get(episodeLink).document.select("#post_report > input:nth-child(5)").attr("value")
        val body = client.post("https://hentaimama.io/wp-admin/admin-ajax.php", data = mapOf(
            "action" to "get_player_contents",
            "a" to animeId
        ))
        val json = JSONArray(body.toString())
        for (i in 0 until json.length()){
            val url = json[i].toString().substringAfter("src=\"").substringBefore("\"")
            servers.add(VideoServer("Server $i",url))
        }

        return servers
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = HentaimamaExtractor(server)

    class HentaimamaExtractor(override val server: VideoServer) : VideoExtractor() {
        override suspend fun extract(): VideoContainer {
            var url = "https://hentaimama.io" + client.get(server.embed.url).document.select("script").toString()
                .substringAfter("open('").substringBefore("');")
            if(url.contains("<script type=\"text/javascript\">")){
                url = "https://hentaimama.io" + url.substringAfter("downloadURL: '").substringBefore("'")
            }
            return VideoContainer(listOf(Video(null,false,url)))

        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val list = mutableListOf<ShowResponse>()
        client.get("$hostUrl/?s=$query").document
            .select("div.result-item article").forEach {
                val link = it.select("div.details div.title a").attr("href")
                val title = it.select("div.details div.title a").text()
                val cover = it.select("div.image div a img").attr("src")
                list.add(ShowResponse(title, link,cover))
            }
        return list
    }
}