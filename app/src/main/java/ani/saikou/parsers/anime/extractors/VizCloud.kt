package ani.saikou.parsers.anime.extractors

import android.net.Uri
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer

class VizCloud(override val server: VideoServer) : VideoExtractor() {

    private data class Sources(val file: String, val label: String?)
    private data class Media(val sources: List<Sources>)
    private data class Response(val success: Boolean, val media: Media)

    override suspend fun extract(): VideoContainer {

        val url = server.embed.url
        val headers = server.embed.headers

        val embedded = client.get(url, headers).document
        val skey = embedded.selectFirst("script:containsData(window.skey = )")!!.data().substringAfter("window.skey = \'")
            .substringBefore("\'")

        val response = client.get(
            url.replace("/embed/", "/info/").replace("/e/", "/info/"),
            headers,
            params = mapOf("skey" to skey)
        ).parsed<Response>()

        return VideoContainer(response.media.sources.map {
            val file = FileUrl(it.file, mapOf("referer" to "https://${Uri.parse(url).host}/"))
            Video(null, true, file, null, it.label)
        })
    }
}