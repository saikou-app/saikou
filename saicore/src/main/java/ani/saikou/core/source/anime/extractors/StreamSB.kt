package ani.saikou.core.source.anime.extractors

import ani.saikou.core.model.anime.Episode
import ani.saikou.core.service.LOG
import ani.saikou.core.source.anime.Extractor
import ani.saikou.core.utils.extension.findBetween
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup

class StreamSB : Extractor() {
    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF

            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        try {
            val headers = mutableMapOf(
                "Referer" to "$url/",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36"
            )
            val source =
                Jsoup.connect("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/sb.txt")
                    .get().body().text()
            val jsonLink = "$source/7361696b6f757c7c${
                bytesToHex(
                    (url.findBetween("/e/", ".html") ?: url.split("/e/")[1]).encodeToByteArray()
                )
            }7c7c7361696b6f757c7c73747265616d7362/7361696b6f757c7c363136653639366436343663363136653639366436343663376337633631366536393664363436633631366536393664363436633763376336313665363936643634366336313665363936643634366337633763373337343732363536313664373336327c7c7361696b6f757c7c73747265616d7362"
            val json = Json.decodeFromString<JsonObject>(
                Jsoup.connect(jsonLink).headers(headers).header("watchsb", "streamsb")
                    .ignoreContentType(true).execute().body()
            )
            val m3u8 = json["stream_data"]!!.jsonObject["file"].toString().trim('"')
            return Episode.StreamLinks(
                name,
                listOf(Episode.Quality(m3u8, "Multi Quality", null)),
                headers
            )
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return Episode.StreamLinks(name, listOf(), null)
    }
}
