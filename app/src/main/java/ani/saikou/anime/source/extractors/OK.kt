package ani.saikou.anime.source.extractors

import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.getSize
import ani.saikou.logger
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

class OK(): Extractor() {
    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
            val document = Jsoup.connect(url).get()
            val tempQuality = mutableListOf<Episode.Quality>()
            val videosString = document.select("div[data-options]").attr("data-options")
                .substringAfter("\\\"videos\\\":[{\\\"name\\\":\\\"")
                .substringBefore("]")
            videosString.split("{\\\"name\\\":\\\"").reversed().forEach {
                val videoUrl = it.substringAfter("url\\\":\\\"")
                    .substringBefore("\\\"")
                    .replace("\\\\u0026", "&")
                val videoQuality = "Okru: " + it.substringBefore("\\\"")
                logger("$url")
                if (videoUrl.startsWith("https://")) {
                    tempQuality.add(Episode.Quality(videoUrl, videoQuality, null))
                }
            }
        return Episode.StreamLinks(
            name,
            tempQuality,
            null
        )

                }

}