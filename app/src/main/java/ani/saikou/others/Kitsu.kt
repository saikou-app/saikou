package ani.saikou.others

import ani.saikou.anime.Episode
import ani.saikou.httpClient
import ani.saikou.logger
import ani.saikou.media.Media
import com.fasterxml.jackson.annotation.JsonProperty

object Kitsu {
    private suspend fun getKitsuData(query: String): KitsuResponse {
        val headers = mapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "Connection" to "keep-alive",
            "DNT" to "1",
            "Origin" to "https://kitsu.io"
        )
        val json = httpClient.post("https://kitsu.io/api/graphql",headers, data = mapOf("query" to query))
        return json.parsed()
    }

    suspend fun getKitsuEpisodesDetails(media: Media): MutableMap<String, Episode>? {
        val print = false
        logger("Kitsu : title=${media.getMangaName()}", print)
        try {
            val query =
                """query{searchAnimeByTitle(first:5,title:"${media.getMangaName()}"){nodes{id season startDate titles{localized}episodes(first:2000){nodes{number titles{canonical}description thumbnail{original{url}}}}}}}"""
            val result = getKitsuData(query)
            logger("Kitsu : result=$result", print)
            var arr: MutableMap<String, Episode>?
            if(result.data!=null){
                result.data.searchAnimeByTitle?.nodes?.forEach{
                    logger(it.season, print)
                    if (
                        it.season == media.anime!!.season &&
                        (it.startDate?:return@forEach).split('-')[0] == media.anime.seasonYear.toString()
                    ) {
                        val episodes = it.episodes?.nodes?:return@forEach
                        logger("Kitsu : episodes=$episodes", print)
                        arr = mutableMapOf()
                        episodes.forEach { ep ->
                            logger("Kitsu : forEach=$it", print)
                            if (ep!=null) {
                                val num = ep.number.toString()
                                arr!![num] = Episode(
                                    number = num,
                                    title = ep.titles?.canonical,
                                    desc = ep.description?.en,
                                    thumb = ep.thumbnail?.original?.url,
                                )
                                logger("Kitsu : arr[$num] = ${arr!![num]}", print)
                            }
                        }
                        return arr
                    }
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
        return null
    }

    private data class KitsuResponse (
        val data: Data? = null
    ){
        data class Data (
            val searchAnimeByTitle: SearchAnimeByTitle? = null
        )

        data class SearchAnimeByTitle (
            val nodes: List<NodeElement>? = null
        )

        data class NodeElement (
            val id: String? = null,
            val titles: FluffyTitles? = null,
            val season: String? = null,
            val startDate: String? = null,
            val posterImage: PosterImage? = null,
            val episodes: Episodes? = null
        )

        data class Episodes (
            val nodes: List<EpisodesNode?>? = null
        )

        data class EpisodesNode (
            val number: Long? = null,
            val titles: PurpleTitles? = null,
            val description: Description? = null,
            val thumbnail: PosterImage? = null
        )

        data class Description (
            val en: String? = null
        )

        data class PosterImage (
            val original: Original? = null
        )

        data class Original (
            val url: String? = null
        )

        data class PurpleTitles (
            val canonical: String? = null
        )

        data class FluffyTitles (
            val localized: Localized? = null
        )

        data class Localized (
            val en: String? = null,

            @JsonProperty("en_jp")
            val enJp: String? = null,

            @JsonProperty("ja_jp")
            val jaJp: String? = null,

            @JsonProperty("en_us")
            val enUs: String? = null
        )
    }

}