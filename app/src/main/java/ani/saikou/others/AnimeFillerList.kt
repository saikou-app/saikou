package ani.saikou.others

import ani.saikou.anime.Episode
import ani.saikou.httpClient
import ani.saikou.logger
import com.fasterxml.jackson.annotation.JsonProperty

object AnimeFillerList {
    suspend fun getFillers(malId: Int): MutableMap<String, Episode>? {
        try {
            val map = mutableMapOf<String, Episode>()
            val json = httpClient.get("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/fillers/$malId.json")
            if (json.text != "404: Not Found") json.parsed<AnimeFillerListValue>().episodes?.forEach {
                val num = it.number.toString()
                map[num] = Episode(
                    num,
                    it.title,
                    filler = it.fillerBool == true
                )
            }
            return map
        } catch (e: Exception) {
            logger(e)
        }
        return null
    }
    data class AnimeFillerListValue (
        @JsonProperty("MAL-id")
        val malID: Int? = null,

        @JsonProperty("Anilist-id")
        val anilistID: Int? = null,

        val episodes: List<AFLEpisode>? = null
    )

    data class AFLEpisode (
        val number: Int? = null,
        val title: String? = null,
        val desc: String? = null,
        val filler: String? = null,
        @JsonProperty("filler-bool") val fillerBool : Boolean?=null,
        val airDate: String? = null
    )
}


