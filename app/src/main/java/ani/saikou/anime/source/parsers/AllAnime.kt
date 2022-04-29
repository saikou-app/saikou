package ani.saikou.anime.source.parsers

import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.anime.source.Extractor
import ani.saikou.anime.source.extractors.FPlayer
import ani.saikou.anime.source.extractors.GogoCDN
import ani.saikou.anime.source.extractors.StreamSB
import ani.saikou.media.Media
import ani.saikou.media.Source
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.lang.Exception
import java.text.DecimalFormat

class AllAnime(private val dub: Boolean = false, override val name: String = "allanime.site") : AnimeParser() {
    private val host = "https://$name/"
    private val apiHost = "https://blog.allanimenews.com/"
    private val mapper = jacksonObjectMapper()
    private val idRegex = Regex("${host}anime/(\\w+)")
    private val languageRegex = Regex("vo_a_hls_(\\w+-\\w+)")

    private suspend fun directLinkify(
        name: String,
        url: String,
    ): Episode.StreamLinks? {
        val domain = Regex("""(?<=^http[s]?://).+?(?=/)""").find(url)?.value ?: return null
        val extractor: Extractor? = when {
            "gogo" in domain    -> GogoCDN(apiHost)
            "goload" in domain  -> GogoCDN(apiHost)
            "sb" in domain      -> StreamSB()
            "fplayer" in domain -> FPlayer(true)
            "fembed" in domain  -> FPlayer(true)
            else                -> null
        }
        val a = extractor?.getStreamLinks(name, url)
        if (a != null && a.quality.isNotEmpty()) return a
        return null
    }

    override suspend fun getStream(episode: Episode, server: String): Episode {
        val desiredServer = getStreams(episode).streamLinks[server]
        if (desiredServer != null) {
            episode.streamLinks[server] = desiredServer
        }
        return episode
    }

    override suspend fun getStreams(episode: Episode): Episode {
        val showId = idRegex.find(episode.link!!)?.groupValues?.get(1)
        if (showId != null) {
            try {
                episode.streamLinks = let {
                    val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()
                    val epInfo = getEpisodeInfos(showId)
                    val currentEp = epInfo?.find { it.episodeIdNum == episode.number.toFloat() }
                    val reportedResolution =
                        currentEp.let { if (dub) it?.vidInforsdub?.vidResolution else it?.vidInforssub?.vidResolution }
                    val variables =
                        """{"showId":"$showId","translationType":"${if (dub) "dub" else "sub"}","episodeString":"${episode.number}"}"""
                    graphqlQuery(
                        variables,
                        "29f49ce1a69320b2ab11a475fd114e5c07b03a7dc683f77dd502ca42b26df232"
                    )?.data?.episode?.sourceUrls?.forEach { source ->
                        val nonExtractorLinks = mutableListOf<String>()
                        // It can be that two different actual sources share the same sourceName
                        var serverName = source.sourceName
                        var sourceNum = 2
                        // Sometimes provides relative links just because ¯\_(ツ)_/¯
                        if (source.sourceUrl.toHttpUrlOrNull() == null) {
                            val jsonUrl = """${apiHost}${source.sourceUrl.replace("clock", "clock.json").substring(1)}"""
                            val rawResponse = httpClient.get(jsonUrl)
                            if (rawResponse.code > 400) return@forEach
                            val response = rawResponse.body?.string()
                            if (response != null) {
                                mapper.readValue<ApiSourceResponse>(response).links.forEach {
                                    // Avoid languages other than english when multiple urls are provided
                                    val matchesLanguagePattern = languageRegex.find(it.resolutionStr)
                                    val language = matchesLanguagePattern?.groupValues?.get(1)
                                    if (matchesLanguagePattern == null || language?.contains("en") == true) {
                                        while (linkForVideos[serverName] != null) {
                                            serverName = "${source.sourceName} ($sourceNum)"
                                            sourceNum++
                                        }
                                    }

                                    // Check for extractor links
                                    (it.src ?: it.link)?.let { url ->
                                        val directLinks = directLinkify(serverName, url)
                                        if (directLinks != null) {
                                            linkForVideos[serverName] = directLinks
                                        } else {
                                            nonExtractorLinks.add(url)
                                        }
                                    }
                                }
                            }
                        } else {
                            while (linkForVideos[serverName] != null) {
                                serverName = "${source.sourceName} ($sourceNum)"
                                sourceNum++
                            }

                            val directLinks = directLinkify(serverName, source.sourceUrl)
                            if (directLinks != null) {
                                linkForVideos[serverName] = directLinks
                            } else {
                                nonExtractorLinks.add(source.sourceUrl)
                            }
                        }

                        toStreamLinks(serverName, nonExtractorLinks, reportedResolution)?.let {
                            linkForVideos[serverName] = it
                        }
                    }
                    return@let (linkForVideos)
                }
            } catch (e: Exception) {
                toastString("$e")
                e.printStackTrace()
            }
        }
        return episode
    }

    private suspend fun toStreamLinks(
        server: String, links: List<String>, reportedResolution: Int?
    ): Episode.StreamLinks? {
        val qualities = arrayListOf<Episode.Quality>()
        val headers = mutableMapOf<String, String>()
        links.forEach {
            if (it.contains(".m3u8")) {
                qualities.add(Episode.Quality(it, "Multi Quality", null))
                return Episode.StreamLinks(server, qualities)
            }
            if (it.contains(".mp4")) {
                if ("king.stronganime" in it) {
                    headers["Referer"] = host
                }
                qualities.add(Episode.Quality(it, "${reportedResolution ?: "??"}p", getSize(it, headers)))
            }
        }
        return if (qualities.isNotEmpty()) Episode.StreamLinks(server, qualities, headers) else null
    }

    override suspend fun getEpisodes(media: Media): MutableMap<String, Episode> {
        try {
            var slug: Source? = loadData("allanime${media.id}")
            if (slug == null) {
                suspend fun s(it: String): Boolean {
                    setTextListener("Searching for $it")
                    logger("AllAnime: Searching for $it")
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
            toastString("$e")
        }
        return mutableMapOf()
    }

    override suspend fun search(string: String): ArrayList<Source> {
        logger("AllAnime: Searching for: $string")
        val responseArray = arrayListOf<Source>()
        try {
            val variables =
                """{"search":{"allowAdult":${Anilist.adult},"query":"$string"},"translationType":"${if (dub) "dub" else "sub"}"}"""
            val edges =
                graphqlQuery(variables, "9343797cc3d9e3f444e2d3b7db9a84d759b816a4d84512ea72d079f85bb96e98")?.data?.shows?.edges
            if (!edges.isNullOrEmpty()) {
                for (show in edges) {
                    val link = host + "anime/" + show.id
                    responseArray.add(Source(link, show.name, show.thumbnail))
                }

            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override suspend fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            // Would rather query the API than parse the HTML of the site
            val showId = idRegex.find(slug)?.groupValues?.get(1)
            if (showId != null) {
                val episodeInfos = getEpisodeInfos(showId)
                val format = DecimalFormat("0")
                episodeInfos?.sortedBy { it.episodeIdNum }?.forEach {
                    val link = """${host}anime/$showId/episodes/${if (dub) "dub" else "sub"}/${it.episodeIdNum}"""
                    val epNum = format.format(it.episodeIdNum).toString()
                    responseArray[epNum] = Episode(epNum, it.notes, link = link, thumb = it.thumbnails?.get(0))
                }

            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    private suspend fun graphqlQuery(variables: String, persistHash: String): Query? {
        val extensions = """{"persistedQuery":{"version":1,"sha256Hash":"$persistHash"}}"""
        val graphqlUrl = (host + "graphql").toHttpUrl().newBuilder().addQueryParameter("variables", variables)
            .addQueryParameter("extensions", extensions).build()
        return try {
            httpClient.get(graphqlUrl.toString()).parsed()
        } catch (e: Exception) {
            toastString(e.toString())
            null
        }
    }

    private suspend fun getEpisodeInfos(showId: String): List<EpisodeInfo>? {
        val variables = """{"_id": "$showId"}"""
        val show = graphqlQuery(variables, "bea0b50519809a797e72b9bd5131d453de6bd1841ea7e720765c5af143a0d6f0")?.data?.show
        if (show != null) {
            val epCount = if (dub) show.lastEpisodeInfo.dub?.episodeString else show.lastEpisodeInfo.sub?.episodeString
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
    // val pageInfo
    // val __typename
    val edges: List<Show>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Show(
    @JsonProperty("_id")
    val id: String,
    val name: String,
    // val englishName: String,
    // val nativeName: String,
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
    //val notes: String,
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