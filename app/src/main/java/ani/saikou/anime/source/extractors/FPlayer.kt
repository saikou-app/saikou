package ani.saikou.anime.source.extractors

import ani.saikou.anime.Episode
import ani.saikou.anime.newsrc.IAnimeExtractor
import ani.saikou.anime.source.Extractor
import ani.saikou.getSize
import ani.saikou.toastString
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup

object FPlayer : Extractor(), IAnimeExtractor {
    override val name: String = "FPlayer"

    override fun resolveServer(serverName: String, url: String, fetchSize: Boolean): Episode.VideoServer =
        getStreamLinks(serverName, url, fetchSize)

    override fun getStreamLinks(name: String, url: String, fetchSize: Boolean): Episode.VideoServer {
        val apiLink = url.replace("/v/", "/api/source/")
        val tempQuality = mutableListOf<Episode.VideoQuality>()
        try {
            val jsonResponse = Json.decodeFromString<JsonObject>(
                Jsoup.connect(apiLink)
                    .ignoreContentType(true)
                    .header("referer", url)
                    .post().body().text()
            )

            if (jsonResponse["success"].toString() == "true") {
                val a = mutableListOf<Deferred<*>>()
                runBlocking {
                    jsonResponse.jsonObject["data"]!!.jsonArray.forEach {
                        a.add(async {
                            tempQuality.add(
                                Episode.VideoQuality(
                                    it.jsonObject["file"].toString().trim('"'),
                                    it.jsonObject["label"].toString().trim('"'),
                                    if (fetchSize) {
                                        getSize(it.jsonObject["file"].toString().trim('"'))
                                    } else null
                                )
                            )
                        })
                    }
                }
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return Episode.VideoServer(
            name,
            tempQuality
        )
    }

    override fun canResolve(url: String): Boolean {
        val domain = findDomain(url)
        return "fembed" in domain || "fplayer" in domain
    }
}