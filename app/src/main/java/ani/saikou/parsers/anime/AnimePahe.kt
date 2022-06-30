package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.*

class AnimePahe : AnimeParser() {

    override val hostUrl = "https://animepahe.com"
    override val name = "AnimePahe"
    override val saveName = "anime_pahe"
    override val isDubAvailableSeparately = false

    override suspend fun search(query: String): List<ShowResponse> {
        val resp = client.get("$hostUrl/api?m=search&q=${encode(query)}").parsed<SearchQuery>()
        return resp.data.map {
            val epLink = "$hostUrl/api?m=release&id=${it.session}&sort=episode_asc"
            ShowResponse(name = it.title, link = epLink, coverUrl = it.poster)
        }
    }

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val resp = client.get(animeLink).parsed<ReleaseRouteResponse>()
        val releaseId = animeLink.substringAfter("&id=").substringBefore("&sort")
        return getEpisodes(releaseId, resp.last_page)
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val resp = client.get(episodeLink).parsed<KwikUrls>()
        return resp.data.toList().map { it.entries.map {
                o -> VideoServer(name = "Kwik ${o.key}p", embedUrl = o.value.kwik.toString())
        } }[0]
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? = AnimePaheExtractor(server)

    class AnimePaheExtractor(override val server: VideoServer): VideoExtractor() {

        private val kwikRe = Regex("Plyr\\|(.+?)'")

        override suspend fun extract(): VideoContainer {
            val resp = client.get(server.embed.url, referer = "https://animepahe.com/").text
            val obfUrl = kwikRe.find(resp, 0)?.groupValues?.get(1)
            val i = obfUrl?.split('|')?.reversed()!!
            val m3u8Url = "${i[0]}://${i[1]}-${i[2]}.${i[3]}.${i[4]}.${i[5]}/${i[6]}/${i[7]}/${i[8]}/${i[9]}.${i[10]}"  // :pepega:
            return VideoContainer(
                videos = listOf(Video(quality = null, isM3U8 = true, url = FileUrl(m3u8Url, mapOf("Referer" to "https://kwik.cx/"))))
            )
        }
    }


    private suspend fun getEpisodes(releaseId: String, lastPage: Int): List<Episode> {
        val episodes = mutableListOf<Episode>()
        for (i in 1 until lastPage + 1) {
            val url = "$hostUrl/api?m=release&id=$releaseId&sort=episode_asc&page=$i"
            val resp = client.get(url).parsed<ReleaseRouteResponse>()
            resp.data!!.map { ep ->
                val kwikEpLink = "$hostUrl/api?m=links&id=${ep.anime_id}&session=${ep.session}&p=kwik"
                episodes.add(
                    Episode(number = ep.episode.toString(), link = kwikEpLink, title = ep.title,
                        extra = mapOf("session" to ep.session, "releaseId" to releaseId))
                )
            }
        }
        return episodes
    }

}


// --- dataclasses ---

private data class SearchQuery(val data: List<SearchQueryData>) {
    data class SearchQueryData(
        val slug: String,
        val title: String,
        val poster: String,
        val session: String,
    )
}

private data class ReleaseRouteResponse(
    val last_page: Int,
    val data: List<ReleaseResponse>?
) {
    data class ReleaseResponse(
        val episode: Int,
        val anime_id: Int,
        val title: String,
        val snapshot: String,
        val session: String,
    )
}

private data class KwikUrls(val data: List<Map<String, Url>>) {
    data class Url(
        val kwik: String?,
    )
}
