package ani.saikou.parsers.manga

import ani.saikou.anilist.Anilist
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.tryWithSuspend
import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.DecimalFormat

class AllAnime : MangaParser() {
    override val name = "AllAnime"
    override val saveName = "all_anime_manga"
    override val hostUrl = "https://allanime.site"

    private val idRegex = Regex("${hostUrl}/manga/(\\w+)")
    private val epNumRegex = Regex("/[sd]ub/(\\d+)")

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val responseArray = mutableListOf<MangaChapter>()
        tryWithSuspend {
            val showId = idRegex.find(mangaLink)?.groupValues?.get(1)
            if (showId != null) {
                val episodeInfos = getEpisodeInfos(showId)
                val format = DecimalFormat("#####.#####")
                episodeInfos?.sortedBy { it.episodeIdNum }?.forEach { epInfo ->
                    val link = """${hostUrl}/manga/$showId/chapters/sub/${epInfo.episodeIdNum}"""
                    val epNum = format.format(epInfo.episodeIdNum).toString()
                    responseArray.add(MangaChapter(epNum, link, epInfo.notes))
                }

            }
        }
        return responseArray
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val images = mutableListOf<MangaImage>()
        val showId = idRegex.find(chapterLink)?.groupValues?.get(1)
        val episodeNum = epNumRegex.find(chapterLink)?.groupValues?.get(1)
        if (showId != null && episodeNum != null) {
            tryWithSuspend {
                val variables = """{"mangaId":"$showId","translationType":"sub","chapterString":"$episodeNum","limit":1000000}"""
                val chapterPages =
                    graphqlQuery(
                        variables,
                        "fd2226907c2435bdfc0d03a9c46ef354b75ba42ec0599acb6b3346ef9c1e162d"
                    )?.data?.chapterPages?.edges
                // For future reference: If pictureUrlHead is null then the link provided is a relative link of the "apivtwo" variety, but it doesn't seem to contain useful images
                val chapter = chapterPages?.filter { !it.pictureUrlHead.isNullOrEmpty() }?.get(0)
                chapter?.pictureUrls?.sortedBy { it.num }?.forEach { images.add(MangaImage("""${chapter.pictureUrlHead}${it.url}""")) }
            }
        }
        return images
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val responseArray = arrayListOf<ShowResponse>()
        tryWithSuspend {
            val variables =
                """{"search":{"isManga":true,"allowAdult":${Anilist.adult},"query":"$query"},"translationType":"sub"}"""
            val edges =
                graphqlQuery(variables, "d35cd021d782eb55310250ea818269622b4b94742c25f8af778562317966ac88")?.data?.mangas?.edges
            if (!edges.isNullOrEmpty()) {
                for (show in edges) {
                    val link = """${hostUrl}/manga/${show.id}"""
                    val otherNames = mutableListOf<String>()
                    show.englishName?.let { otherNames.add(it) }
                    show.nativeName?.let { otherNames.add(it) }
                    show.altNames?.forEach { otherNames.add(it) }
                    responseArray.add(
                        ShowResponse(show.name, link, show.thumbnail, otherNames, show.availableChapters.sub)
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
        val headers = mutableMapOf<String, String>()
        headers["Host"] = "allanime.site"
        return tryWithSuspend {
            client.get(graphqlUrl.toString(), headers).parsed()
        }
    }

    private suspend fun getEpisodeInfos(showId: String): List<EpisodeInfo>? {
        val variables = """{"_id": "$showId"}"""
        val manga = graphqlQuery(variables, "f60064134ecbaf89350a8aae1441dbffc86cf561a193a3bb8db4bb5a9989b9ad")?.data?.manga
        if (manga != null) {
            val epCount = manga.availableChapters.sub
            val epVariables = """{"showId":"manga@$showId","episodeNumStart":0,"episodeNumEnd":${epCount.toFloat()}}"""
            return graphqlQuery(
                epVariables,
                "73d998d209d6d8de325db91ed8f65716dce2a1c5f4df7d304d952fa3f223c9e8"
            )?.data?.episodeInfos
        }
        return null
    }

    private data class Query(var data: Data?) {
        data class Data(
            val manga: Manga?,
            val mangas: MangasConnection?,
            val episodeInfos: List<EpisodeInfo>?,
            val chapterPages: ChapterConnection?,
        )

        data class MangasConnection(
            val edges: List<Manga>
        )

        data class Manga(
            @JsonProperty("_id")
            val id: String,
            val name: String,
            val description: String?,
            val englishName: String?,
            val nativeName: String?,
            val thumbnail: String,
            val availableChapters: AvailableChapters,
            val altNames: List<String>?
        )

        data class AvailableChapters(
            val sub: Int,
            val raw: Int
        )

        data class ChapterConnection(
            val edges: List<Chapter>
        ) {
            data class Chapter(
                val pictureUrls: List<PictureUrl>,
                val pictureUrlHead: String?
            )

            data class PictureUrl(
                val num: Int,
                val url: String

                )
        }
    }

    private data class EpisodeInfo(
        // Episode "numbers" can have decimal values, hence float
        val episodeIdNum: Float,
        val notes: String?,
        val thumbnails: List<String>?,
    )

}
