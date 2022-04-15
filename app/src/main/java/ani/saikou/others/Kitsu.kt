package ani.saikou.others

import ani.saikou.anime.Episode
import ani.saikou.logger
import ani.saikou.media.Media
import ani.saikou.toastString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.jsoup.Connection.Method
import org.jsoup.Jsoup

object Kitsu {
    private fun getKitsuData(query: String): String {
        return Jsoup.connect("https://kitsu.io/api/graphql")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Connection", "keep-alive")
            .header("DNT", "1")
            .header("Origin", "https://kitsu.io")
            .ignoreContentType(true)
            .ignoreHttpErrors(true)
            .requestBody(query)
            .method(Method.POST).execute().body()
    }

    fun getKitsuEpisodesDetails(media: Media): MutableMap<String, Episode>? {
        val print = false
        logger("Kitsu : title=${media.getMangaName()}", print)
        try {
            val query = """{"query":"query{searchAnimeByTitle(first:5,title:\"${media.getMangaName()}\"){nodes{id season startDate titles{localized}episodes(first:2000){nodes{number titles{canonical}description thumbnail{original{url}}}}}}}"}"""
            val result = getKitsuData(query)
            logger("Kitsu : result=$result", print)
            var arr: MutableMap<String, Episode>?
            val json = Json.decodeFromString<JsonObject>(result)
            if (json.containsKey("data")) {
                val node: JsonElement? = json.jsonObject["data"]!!.jsonObject["searchAnimeByTitle"]!!.jsonObject["nodes"]
                if (node != null) {
                    if (!node.jsonArray.isEmpty()) {
                        logger("Kitsu : Not Empty", print)
                        node.jsonArray.forEach { j ->
                            logger(j.jsonObject["season"].toString().trim('"'), print)
                            if (j.jsonObject["season"].toString().trim('"') == media.anime!!.season && j.jsonObject["startDate"].toString().trim('"').split('-')[0] == media.anime.seasonYear.toString()) {
                                val episodes: JsonElement? = j.jsonObject["episodes"]!!.jsonObject["nodes"]
                                logger("Kitsu : episodes=$episodes", print)
                                arr = mutableMapOf()
                                episodes?.jsonArray?.forEach {
                                    logger("Kitsu : forEach=$it", print)
                                    if (it != JsonNull) {
                                        val i = it.jsonObject["number"]?.toString()?.replace("\"", "")!!
                                        var name: String? = null
                                        if (it.jsonObject["titles"]!!.jsonObject["canonical"] != JsonNull) {
                                            name = it.jsonObject["titles"]!!.jsonObject["canonical"]?.toString()?.replace("\"", "")
                                            if (name == "null") {
                                                name = null
                                            }
                                        }
                                        arr!![i] = Episode(
                                            number = it.jsonObject["number"]?.toString()?.replace("\"", "")!!,
                                            title = name,
                                            desc = if (it.jsonObject["description"]!!.jsonObject["en"] != JsonNull) it.jsonObject["description"]!!.jsonObject["en"]?.toString()?.replace("\"", "")?.replace("\\n", "\n") else null,
                                            thumb = if (it.jsonObject["thumbnail"] != JsonNull) it.jsonObject["thumbnail"]!!.jsonObject["original"]!!.jsonObject["url"]?.toString()?.replace("\"", "") else null,
                                        )
                                        logger("Kitsu : arr[$i] = ${arr!![i]}", print)
                                    }
                                }
                                return arr
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return null
    }
}
