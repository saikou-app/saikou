package ani.saikou.parsers.anime

import android.net.Uri
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.FPlayer
import ani.saikou.parsers.anime.extractors.GogoCDN
import ani.saikou.parsers.anime.extractors.StreamSB
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.Requests.Companion.mapper
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.text.DecimalFormat

class AllAnime : AnimeParser() {
    override val name = "AllAnime"
    override val saveName = "all_anime"
    override val hostUrl = "https://allanime.site"
    override val isDubAvailableSeparately = true

    private val apiHost = "https://blog.allanimenews.com/"
    private val idRegex = Regex("${hostUrl}/anime/(\\w+)")
    private val epNumRegex = Regex("/[sd]ub/(\\d+)")



    override suspend fun loadEpisodes(animeLink: String): List<Episode> {
        val responseArray = mutableListOf<Episode>()
        tryWithSuspend {
            val showId = idRegex.find(animeLink)?.groupValues?.get(1)
            if (showId != null) {
                val episodeInfos = getEpisodeInfos(showId)
                val format = DecimalFormat("0")
                episodeInfos?.sortedBy { it.episodeIdNum }?.forEach { epInfo ->
                    val link = """${hostUrl}/anime/$showId/episodes/${if (selectDub) "dub" else "sub"}/${epInfo.episodeIdNum}"""
                    val epNum = format.format(epInfo.episodeIdNum).toString()
                    responseArray.add(Episode(epNum, link = link, epInfo.notes, epInfo.thumbnails?.get(0)?.let { FileUrl(it) }))
                }

            }
        }
        return responseArray
    }

    override suspend fun loadVideoServers(episodeLink: String): List<VideoServer> {
        val showId = idRegex.find(episodeLink)?.groupValues?.get(1)
        val videoServers = mutableListOf<VideoServer>()
        val episodeNum = epNumRegex.find(episodeLink)?.groupValues?.get(1)
        if (showId != null && episodeNum != null) {
            tryWithSuspend {
                val variables =
                    """{"showId":"$showId","translationType":"${if (selectDub) "dub" else "sub"}","episodeString":"$episodeNum"}"""
                graphqlQuery(
                    variables,
                    "29f49ce1a69320b2ab11a475fd114e5c07b03a7dc683f77dd502ca42b26df232"
                )?.data?.episode?.sourceUrls?.forEach { source ->
                    // It can be that two different actual sources share the same sourceName
                    var serverName = source.sourceName
                    var sourceNum = 2
                    // Sometimes provides relative links just because ¯\_(ツ)_/¯
                    while (videoServers.any { it.name == serverName }) {
                        serverName = "${source.sourceName} ($sourceNum)"
                        sourceNum++
                    }

                    if (source.sourceUrl.toHttpUrlOrNull() == null) {
                        val jsonUrl = """${apiHost}${source.sourceUrl.replace("clock", "clock.json").substring(1)}"""
                        videoServers.add(VideoServer(serverName, jsonUrl))
                    } else {
                        videoServers.add(VideoServer(serverName, source.sourceUrl))
                    }
                }
            }
        }
        return videoServers
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val serverUrl = Uri.parse(server.embed.url)
        val domain = serverUrl.host ?: return null
        val path = serverUrl.path ?: return null
        val extractor: VideoExtractor? = when {
            "gogo" in domain    -> GogoCDN(server)
            "goload" in domain  -> GogoCDN(server)
            "sb" in domain      -> StreamSB(server)
            "fplayer" in domain -> FPlayer(server)
            "fembed" in domain  -> FPlayer(server)
            "apivtwo" in path   -> AllAnimeExtractor(server)
            else                -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val responseArray = arrayListOf<ShowResponse>()
        tryWithSuspend {
            val variables =
                """{"search":{"allowAdult":${Anilist.adult},"query":"$query"},"translationType":"${if (selectDub) "dub" else "sub"}"}"""
            val edges =
                graphqlQuery(variables, "9343797cc3d9e3f444e2d3b7db9a84d759b816a4d84512ea72d079f85bb96e98")?.data?.shows?.edges
            if (!edges.isNullOrEmpty()) {
                for (show in edges) {
                    val link = """${hostUrl}/anime/${show.id}"""
                    val otherNames = mutableListOf<String>()
                    show.englishName?.let { otherNames.add(it) }
                    show.nativeName?.let { otherNames.add(it) }
                    responseArray.add(
                        ShowResponse(
                            show.name,
                            link,
                            show.thumbnail,
                            otherNames,
                            show.availableEpisodes.let { if (selectDub) it.dub else it.sub })
                    )
                }

            }
        }
        return responseArray
    }

    private suspend fun graphqlQuery(variables: String, persistHash: String): Query? {
        val extensions = """{"persistedQuery":{"version":1,"sha256Hash":"$persistHash"}}"""
        val graphqlUrl = ("$hostUrl/graphql").toHttpUrl().newBuilder().addQueryParameter("variables", variables)
            .addQueryParameter("extensions", extensions).build()
        return tryWithSuspend {
            client.get(graphqlUrl.toString()).parsed()
        }
    }

    private suspend fun getEpisodeInfos(showId: String): List<EpisodeInfo>? {
        val variables = """{"_id": "$showId"}"""
        val show = graphqlQuery(variables, "bea0b50519809a797e72b9bd5131d453de6bd1841ea7e720765c5af143a0d6f0")?.data?.show
        if (show != null) {
            val epCount = if (selectDub) show.lastEpisodeInfo.dub?.episodeString else show.lastEpisodeInfo.sub?.episodeString
            if (epCount != null) {
                val epVariables = """{"showId":"$showId","episodeNumStart":0,"episodeNumEnd":${epCount.toFloat()}}"""
                return graphqlQuery(
                    epVariables,
                    "73d998d209d6d8de325db91ed8f65716dce2a1c5f4df7d304d952fa3f223c9e8"
                )?.data?.episodeInfos
            }
        }
        return null
    }

    private class AllAnimeExtractor(override val server: VideoServer) : VideoExtractor() {
        private val languageRegex = Regex("vo_a_hls_(\\w+-\\w+)")

        override suspend fun extract(): VideoContainer {
            val url = server.embed.url
            val rawResponse = client.get(url)
            val linkList = mutableListOf<String>()
            if (rawResponse.code < 400) {
                val response = rawResponse.body?.string()
                if (response != null) {
                    mapper.readValue<ApiSourceResponse>(response).links.forEach {
                        // Avoid languages other than english when multiple urls are provided
                        val matchesLanguagePattern = languageRegex.find(it.resolutionStr)
                        val language = matchesLanguagePattern?.groupValues?.get(1)
                        if (matchesLanguagePattern == null || language?.contains("en") == true) {
                            (it.src ?: it.link)?.let { fileUrl ->
                                linkList.add(fileUrl)
                            }
                        }
                    }
                }
            }

            return VideoContainer(toVideoList(linkList))
        }

        private suspend fun toVideoList(
            links: List<String>
        ): List<Video> {
            val videos = mutableListOf<Video>()
            val headers = mutableMapOf<String, String>()
            links.forEach {
                val fileUrl = FileUrl(it, headers)
                val urlPath = Uri.parse(it).path
                if (urlPath != null) {
                    if (urlPath.endsWith(".m3u8")) {
                        videos.add(Video(null, true, fileUrl, getSize(fileUrl)))
                    }
                    if (urlPath.endsWith(".mp4")) {
                        if ("king.stronganime" in it) {
                            headers["Referer"] = "https://allanime.site"
                        }
                        videos.add(Video(null, false, fileUrl, getSize(fileUrl)))
                    }
                }
            }
            return videos
        }
    }


}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Query(
    var data: Data?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Data(
    val shows: ShowsConnection?,
    val show: Show?,
    val episodeInfos: List<EpisodeInfo>?,
    val episode: AllAnimeEpisode?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShowsConnection(
    val edges: List<Show>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Show(
    @JsonProperty("_id")
    val id: String,
    val name: String,
    val englishName: String?,
    val nativeName: String?,
    val thumbnail: String,
    val availableEpisodes: AvailableEpisodes,
    // Actually just raw unspecified json
    val lastEpisodeInfo: LastEpisodeInfos
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AvailableEpisodes(
    val sub: Int,
    val dub: Int,
    // val raw: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LastEpisodeInfos(
    val sub: LastEpisodeInfo?,
    val dub: LastEpisodeInfo?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LastEpisodeInfo(
    val episodeString: String?,
    val notes: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EpisodeInfo(
    // Episode "numbers" can have decimal values, hence float
    val episodeIdNum: Float,
    val notes: String?,
    val thumbnails: List<String>?,
    val vidInforssub: VidInfo?,
    val vidInforsdub: VidInfo?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VidInfo(
    // val vidPath
    val vidResolution: Int?,
    val vidSize: Double?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AllAnimeEpisode(
    var sourceUrls: List<SourceUrl>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SourceUrl(
    val sourceUrl: String,
    val sourceName: String,
    val priority: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiSourceResponse(
    val links: List<ApiLink>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiLink(
    val link: String?,
    val src: String?,
    val hls: Boolean?,
    val mp4: Boolean?,
    val resolutionStr: String,
)