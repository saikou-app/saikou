package ani.saikou.anime.source.parsers

import ani.saikou.anime.Episode
import ani.saikou.findBetween
import ani.saikou.getSize
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object Haho : Tenshi() {
    override val name = "Haho"
    private const val host = "haho.moe"
    override val isAdult: Boolean = true

    override fun findVideoServer(episode: Episode, it: Element): Pair<String, Episode.VideoServer> {
        val server = it.text().replace(" ", "").replace("/-", "")
        val url = "https://$host/embed?v=" + ("${it.attr("href")}|").findBetween("?v=", "|")
        val a = arrayListOf<Deferred<*>>()
        val headers = mutableMapOf("Cookie" to cookie, "referer" to url)
        val qualities = arrayListOf<Episode.VideoQuality>()
        runBlocking {
            Jsoup.connect(url).headers(mutableMapOf("Cookie" to cookie, "referer" to episode.link!!)).get()
                .select("video#player>source").forEach {
                    a.add(async {
                        val uri = it.attr("src")
                        if (uri != "")
                            qualities.add(
                                Episode.VideoQuality(
                                    videoUrl = uri,
                                    quality = it.attr("title"),
                                    size = getSize(uri, headers)
                                )
                            )
                    })
                }
            a.awaitAll()
        }

        return Pair(server, Episode.VideoServer(server, qualities, headers))
    }
}