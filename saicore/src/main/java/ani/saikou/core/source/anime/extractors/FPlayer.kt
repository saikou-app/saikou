package ani.saikou.core.source.anime.extractors

import ani.saikou.core.model.anime.Episode
import ani.saikou.core.service.LOG
import ani.saikou.core.source.anime.Extractor
import ani.saikou.core.utils.getSize
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup

class FPlayer(private val parseSize: Boolean) : Extractor() {
    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val apiLink = url.replace("/v/", "/api/source/")
        val tempQuality = mutableListOf<Episode.Quality>()
        try {
            val jsonResponse = Json.decodeFromString<JsonObject>(
                Jsoup.connect(apiLink).ignoreContentType(true)
                    .header("referer", url)
                    .post().body().text()
            )

            if (jsonResponse["success"].toString() == "true") {
                val a = arrayListOf<Deferred<*>>()
                runBlocking {
                    jsonResponse.jsonObject["data"]!!.jsonArray.forEach {
                        a.add(async {
                            tempQuality.add(
                                Episode.Quality(
                                    it.jsonObject["file"].toString().trim('"'),
                                    it.jsonObject["label"].toString().trim('"'),
                                    if (parseSize) {
                                        getSize(
                                            it.jsonObject["file"].toString().trim('"')
                                        )
                                    } else null
                                )
                            )
                        })
                    }
                }
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return Episode.StreamLinks(
            name,
            tempQuality,
            null
        )
    }

}