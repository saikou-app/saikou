package ani.saikou.anime.source.extractors

import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.getSize
import ani.saikou.httpClient
import ani.saikou.others.logError

class StreamTape : Extractor() {
    private val linkRegex =
        Regex("""'robotlink'\)\.innerHTML = '(.+?)'\+ \('(.+?)'\)""")

    override suspend fun getStreamLinks(name: String, url: String): Episode.StreamLinks? {
        val list = mutableListOf<Episode.Quality>()
        try {
            println("called streamtape")
            linkRegex.find(httpClient.get(url).text)!!.let {
                val extractedUrl = "https:${it.groups[1]!!.value + it.groups[2]!!.value.substring(3)}"
                return Episode.StreamLinks(name, listOf(Episode.Quality(extractedUrl, "Default Quality", getSize(extractedUrl))))
            }
        } catch (e:Exception){
            logError(e)
        }
        return null
    }
}