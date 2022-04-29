package ani.saikou.anime.source.extractors

import android.net.Uri
import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.httpClient

class VizCloud(private val referer: String) : Extractor() {

    override suspend fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val embedded = httpClient.get(url, referer = referer).document
        val skey = embedded.selectFirst("script:containsData(window.skey = )")!!.data().substringAfter("window.skey = \'")
            .substringBefore("\'")

        data class Sources(val file: String, val label: String?)
        data class Media(val sources: List<Sources>)
        data class Response(val success: Boolean, val media: Media)

        val response = httpClient.get(
            url.replace("/embed/", "/info/").replace("/e/", "/info/"),
            referer = referer,
            params = mapOf("skey" to skey)
        ).parsed<Response>()

        val mediaSources = response.media.sources[0].file
        return Episode.StreamLinks(
            name,
            listOf(Episode.Quality(mediaSources, "Multi Quality", null)),
            mutableMapOf("referer" to "https://${Uri.parse(url).host}/")
        )
    }
}