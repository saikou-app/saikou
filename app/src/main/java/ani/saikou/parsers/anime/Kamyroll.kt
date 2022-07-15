package ani.saikou.parsers.anime

import ani.saikou.*
import ani.saikou.media.Media
import ani.saikou.parsers.*
import ani.saikou.settings.PlayerSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Kamyroll : AnimeParser() {

    override val name: String = "Kamyroll"
    override val saveName: String = "kamy_roll"
    override val hostUrl: String = apiUrl
    override val isDubAvailableSeparately: Boolean = false

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        return if (extra?.get("type") == "series") {
            val idHeader = "id" to animeLink
            val filter = extra["filter"]
            val eps = client.get(
                "$hostUrl/content/v1/seasons",
                getHeaders(),
                params = if (filter == null) mapOf(channelHeader, localeHeader, idHeader)
                else mapOf(channelHeader, localeHeader, idHeader, "filter" to filter),
                timeout = 100
            ).parsed<EpisodesResponse>()

            data class Temp(
                val type: String,
                val thumb: String?,
                val title: String?,
                val description: String?,
                val series: MutableMap<String, String> = mutableMapOf()
            )

            val epMap = mutableMapOf<Float, Temp>()
            val dataList = (eps.items ?: return listOf()).mapNotNull { item ->
                val tit = item.title ?: return@mapNotNull null
                (item.episodes ?: return@mapNotNull null).map {
                    Pair(
                        it.sequenceNumber,
                        Temp(
                            it.type,
                            it.images?.thumbnail?.getOrNull(6)?.source,
                            it.title,
                            it.description,
                            mutableMapOf(tit to it.id)
                        )
                    )
                }
            }.flatten()
            dataList.forEach {
                epMap[it.first] = epMap[it.first] ?: it.second
                epMap[it.first]?.series?.putAll(it.second.series)
            }
            epMap.map {
                if (it.value.thumb != null)
                    Episode(
                        it.key.toString().replace(".0",""),
                        it.value.type,
                        it.value.title,
                        it.value.thumb!!,
                        it.value.description,
                        false,
                        it.value.series.toMap()
                    )
                else
                    Episode(it.key.toString(), it.value.type, extra = it.value.series.toMap())
            }
        } else {
            val eps = client.get(
                "$hostUrl/content/v1/movies",
                getHeaders(),
                params = mapOf(
                    channelHeader,
                    localeHeader,
                    "id" to animeLink
                ),
                timeout = 100
            ).parsed<MovieResponse>()
            val ep = eps.items?.sortedByDescending { it.duration }?.get(0)?.let {
                val thumb = it.images?.thumbnail?.getOrNull(5)?.source
                if (thumb != null) Episode("1", it.id, thumbnail = thumb)
                else Episode("1", it.id)
            } ?: return listOf()
            listOf(ep)
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        return if (extra is Map<*, *>) {
            extra.map {
                VideoServer(it.key.toString(), it.value.toString())
            }
        } else {
            listOf(VideoServer(channel, episodeLink))
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = KamyrollExtractor(server)

    class KamyrollExtractor(override val server: VideoServer) : VideoExtractor() {
        override suspend fun extract(): VideoContainer {
            val eps = client.get(
                "$apiUrl/videos/v1/streams",
                getHeaders(),
                params = mapOf(
                    channelHeader,
                    "id" to server.embed.url,
                    localeHeader,
                    "type" to "adaptive_hls",
                    "format" to "srt",
                    "service" to service,
                ),
                timeout = 60
            ).parsed<StreamsResponse>()

            var foundSub = false
            val link = FileUrl(
                eps.streams?.find {
                    it.hardsubLocale == locale
                }?.url ?: eps.streams?.find {
                    it.hardsubLocale == ""
                }?.also { foundSub = true }?.url ?: return VideoContainer(listOf()),
                mapOf("accept" to "*/*", "accept-encoding" to "gzip")
            )
            val vid = listOf(Video(null, true, link))
            val subtitle = if (foundSub) eps.subtitles?.find { it.locale == locale || it.locale == "en-GB" }
                .let { listOf(Subtitle("English", it?.url ?: return@let null, "ass")) } else null
            println("vid: $vid \nsub: $subtitle")
            return VideoContainer(vid, subtitle ?: listOf())
        }

        @Serializable
        private data class StreamsResponse(
            @SerialName("subtitles") val subtitles: List<Subtitle>? = null,
            @SerialName("streams") val streams: List<Stream>? = null
        ) {
            @Serializable
            data class Stream(
                @SerialName("hardsub_locale") val hardsubLocale: String? = null,
                @SerialName("url") val url: String? = null
            )

            @Serializable
            data class Subtitle(
                @SerialName("locale") val locale: String? = null,
                @SerialName("url") val url: String? = null,
                @SerialName("format") val format: String? = null
            )
        }
    }

    override suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        if (channel == "crunchyroll") {
            var response = loadSavedShowResponse(mediaObj.id)
            if (response != null) {
                saveShowResponse(mediaObj.id, response, true)
            } else {
                response = if (mediaObj.crunchySlug != null || mediaObj.vrvId != null) ShowResponse(
                    "Automatically",
                    mediaObj.vrvId ?: mediaObj.crunchySlug!!,
                    "",
                    extra = mapOf(
                        "type" to if (mediaObj.format != "MOVIE") "series" else "",
                        "filter" to (mediaObj.alName())
                    )
                ) else null
                if (response == null) {
                    setUserText("Searching : ${mediaObj.mainName()}")
                    response = search("$" + mediaObj.mainName()).let { if (it.isNotEmpty()) it[0] else null }
                }
                if (response == null) {
                    setUserText("Searching : ${mediaObj.nameRomaji}")
                    response = search("$" + mediaObj.nameRomaji).let { if (it.isNotEmpty()) it[0] else null }
                }
                saveShowResponse(mediaObj.id, response)
            }
            return response
        } else return super.autoSearch(mediaObj)
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val res = client.get(
            "$hostUrl/content/v1/search",
            getHeaders(),
            params = mapOf(
                channelHeader,
                localeHeader,
                "limit" to "25",
                "query" to query
            )
        ).parsed<SearchResponse>()
        return (res.items ?: listOf()).map { item ->
            val filter = if (query.startsWith("$")) query.substringAfter("$") else null
            item.items.map {
                val type = "type" to it.type
                ShowResponse(
                    name = it.title,
                    link = it.id,
                    coverUrl = it.images?.posterTall?.getOrNull(5)?.source ?: "",
                    extra = if (filter == null) mapOf(type) else mapOf(type, "filter" to filter)
                )
            }
        }.flatten().sortedBy { levenshtein(it.name, query) }
    }

    companion object {
        private val player = "player_settings"
        val settings = loadData<PlayerSettings>(player, toast = false) ?: PlayerSettings().apply { saveData(player, this) }
        private val locales = when(settings.locale) {
            0 -> "en-US"
            1 -> "en-GB"
            2 -> "pt-BR"
            3 -> "es-LA"
            4 -> "es-ES"
            5 -> "pt-PT"
            6 -> "fr-FR"
            7 -> "de-DE"
            8 -> "ar-ME"
            9 -> "it-IT"
            10 -> "ru-RU"
            else -> "en-US"
        }
        private const val apiUrl = "https://kamyroll.herokuapp.com"
        private const val channel = "crunchyroll"
        private val locale = locales
        private const val service = "google"

        private var headers: Map<String, String>? = null
        private val channelHeader = "channel_id" to channel
        private val localeHeader = "locale" to locale

        suspend fun getHeaders(): Map<String, String> {
            headers = headers ?: let {
                val res = client.post(
                    "$apiUrl/auth/v1/token",
                    mapOf(
                        "authorization" to "Basic vrvluizpdr2eby+RjSKM17dOLacExxq1HAERdxQDO6+2pHvFHTKKnByPD7b6kZVe1dJXifb6SG5NWMz49ABgJA=="
                    ),
                    data = mapOf(
                        "refresh_token" to "IV+FtTI+SYR0d5CQy2KOc6Q06S6aEVPIjZdWA6mmO7nDWrMr04cGjSkk4o6urP/6yDmE4yzccSX/rP/OIgDgK4ildzNf2G/pPS9Ze1XbEyJAEUyN+oKT7Gs1PhVTFdz/vYXvxp/oZmLWQGoGgSQLwgoRqnJddWjqk0ageUbgT1FwLazdL3iYYKdNN98BqGFbs/baeqqa8aFre5SzF/4G62y201uLnsElgd07OAh1bnJOy8PTNHpGqEBxxbo1VENqtYilG9ZKY18nEz8vLPQBbin/IIEjKITjSa+LvSDQt/0AaxCkhClNDUX2uUZ8q7fKuSDisJtEyIFDXtuZGFhaaA==",
                        "grant_type" to "refresh_token",
                        "scope" to "offline_access",
                    )
                ).parsed<AccessToken>()
                mapOf("authorization" to "${res.tokenType} ${res.accessToken}")
            }
            return headers!!
        }

        @Serializable
        private data class AccessToken(
            @SerialName("access_token")
            val accessToken: String,
            @SerialName("token_type")
            val tokenType: String,
        )
    }

    @Serializable
    private data class MovieResponse(
        @SerialName("items") val items: List<KamyEpisode>? = null,
    )

    @Serializable
    private data class EpisodesResponse(
        @SerialName("total") val total: Long? = null,
        @SerialName("items") val items: List<Item>? = null
    ) {
        @Serializable
        data class Item(
            @SerialName("title") val title: String? = null,

            @SerialName("season_number")
            val seasonNumber: Long? = null,

            @SerialName("episode_count")
            val episodeCount: Long? = null,

            @SerialName("episodes")
            val episodes: List<KamyEpisode>? = null
        )
    }

    @Serializable
    data class KamyEpisode(
        @SerialName("id") val id: String,
        @SerialName("type") val type: String,

        @SerialName("season_number")
        val seasonNumber: Float? = null,

        @SerialName("episode") val episode: String? = null,

        @SerialName("sequence_number")
        val sequenceNumber: Float,

        @SerialName("title")
        val title: String? = null,

        @SerialName("description")
        val description: String? = null,

        @SerialName("is_subbed")
        val isSubbed: Boolean? = null,

        @SerialName("is_dubbed")
        val isDubbed: Boolean? = null,

        @SerialName("images") val images: Images? = null,

        @SerialName("duration_ms")
        val duration: Long? = null,
    ) {
        @Serializable
        data class Images(
            @SerialName("thumbnail") val thumbnail: List<Thumbnail>? = null
        )

        @Serializable
        data class Thumbnail(
            @SerialName("width") val width: Long? = null,
            @SerialName("height") val height: Long? = null,
            @SerialName("source") val source: String? = null
        )
    }

    @Serializable
    private data class SearchResponse(
        @SerialName("total") val total: Long? = null,
        @SerialName("items") val items: List<ResponseItem>? = null
    ) {
        @Serializable
        data class ResponseItem(@SerialName("items") val items: List<ItemItem>)

        @Serializable
        data class ItemItem(
            @SerialName("id") val id: String,
            @SerialName("media_type") val type: String,
            @SerialName("title") val title: String,
            @SerialName("images") val images: Images? = null,
        )

        @Serializable
        data class Images(
            @SerialName("poster_tall") val posterTall: List<PosterTall>
        )

        @Serializable
        data class PosterTall(
            @SerialName("source") val source: String,
        )
    }
}