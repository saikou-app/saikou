package ani.saikou.anime.source.extractors

import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.findBetween
import ani.saikou.httpClient
import ani.saikou.others.logError
import ani.saikou.toastString
import com.fasterxml.jackson.annotation.JsonProperty

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

    override suspend fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        try {
            val hex = bytesToHex((url.findBetween("/e/", ".html") ?: url.split("/e/")[1]).encodeToByteArray())
            val jsonLink = "${getHost()}/7361696b6f757c7c${hex}7c7c7361696b6f757c7c73747265616d7362/7361696b6f757c7c363136653639366436343663363136653639366436343663376337633631366536393664363436633631366536393664363436633763376336313665363936643634366336313665363936643634366337633763373337343732363536313664373336327c7c7361696b6f757c7c73747265616d7362"
            val json = httpClient.get(jsonLink, mapOf("watchsb" to "streamsb")).parsed<Response>()
            if(json.statusCode==200) {
                val m3u8 = json.streamData!!.file
                return Episode.StreamLinks(
                    name,
                    listOf(Episode.Quality(m3u8, "Multi Quality", null))
                )
            }
        } catch (e: Exception) {
            logError(e)
        }
        return Episode.StreamLinks(name, listOf(), null)
    }
    private data class Response (
        @JsonProperty("stream_data")
        val streamData: StreamData? = null,
        @JsonProperty("status_code")
        val statusCode: Int? = null
    )

    private data class StreamData (
        val file: String
    )

    companion object {
        private var host :String?=null
        private suspend fun getHost(): String? {
            host = host
                ?: httpClient.get("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/sb.txt").text
            return host
        }
    }
}
