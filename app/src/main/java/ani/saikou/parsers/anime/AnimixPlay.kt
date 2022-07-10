package ani.saikou.parsers.anime

import ani.saikou.*
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.GogoCDN
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import org.jsoup.Jsoup

class AnimixPlay : AnimeParser()  {

    override val name = "AnimixPlay"
    override val saveName = "animix_play"
    override val hostUrl = "https://animixplay.to"
    override val isDubAvailableSeparately = true

    private val searchUrl = "https://cachecow.eu/api/search"

    override suspend fun search(query: String): List<ShowResponse> {
        val form = FormBody.Builder()
            .add("qfast", query) // did not use encode(query) because it messes up response
            .add("root", "animixplay.to")
            .build()
        val resp = client.post(searchUrl, requestBody = form).parsed<SearchResponse>()
        val doc = Jsoup.parse(resp.result)
        val atag = doc.select("a")
        val imgs = atag.select("img")
        val data = atag.zip(imgs)
        return data.map {
            val title = it.first.select("p").text()
            val link = it.first.attr("href").substringAfter("\\&quot;").substringBefore("\\&quot;")
            val cover = it.second.attr("src").substringAfter("\\&quot;").substringBefore("\\&quot;")
            val finalLink = "$hostUrl/$link${if (selectDub) "-dub" else ""}"
            ShowResponse(name = title, link = finalLink, coverUrl = cover)
        }
    }

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val doc = client.get(animeLink).document
        val eplist = doc.select("div#epslistplace").text()
        if (!eplist.startsWith("{") && "-dub" in animeLink) {
            return emptyList()
        }
        val json = Mapper.parse<JsonObject>(eplist)
        val total = json["eptotal"].toString().replace("\"", "").toInt()
        return (1..total).mapIndexedNotNull { i, it ->
            val link = json[i.toString()]?.jsonPrimitive?.content
            if (link != null) Episode(number = it.toString(), link = "https:$link")
            else null
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        return listOf(VideoServer(name = "Goload", FileUrl(episodeLink)))
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor?  = GogoCDN(server)


    @Serializable
    private data class SearchResponse(val result: String)

}
