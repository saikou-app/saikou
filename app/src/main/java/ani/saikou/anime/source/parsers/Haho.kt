package ani.saikou.anime.source.parsers

import ani.saikou.anime.Episode
import ani.saikou.findBetween
import ani.saikou.getSize
import ani.saikou.httpClient
import ani.saikou.others.asyncEach
import ani.saikou.others.logError
import org.jsoup.nodes.Element

class Haho(name: String = "haho.moe") : Tenshi(name) {
    override suspend fun getStream(episode: Episode, server: String): Episode {
        try {
            episode.streamLinks = mutableMapOf()
            httpClient.get(episode.link!!, mapOf(cookieHeader))
                .document.select("ul.dropdown-menu > li > a.dropdown-item").asyncEach {
                    val a = it.text().replace(" ", "").replace("/-", "")
                    if (server == a)
                        load(episode, it)
                }
        } catch (e: Exception) {
            logError(e)
        }
        return episode
    }

    override suspend fun getStreams(episode: Episode): Episode {
        try {
            episode.streamLinks = mutableMapOf()
            httpClient.get(episode.link!!, mapOf(cookieHeader))
                .document.select("ul.dropdown-menu > li > a.dropdown-item").asyncEach {
                    load(episode, it)
                }
        } catch (e: Exception) {
            logError(e)
        }
        return episode
    }

    override suspend fun load(episode: Episode, it: Element) {
        val server = it.text().replace(" ", "").replace("/-", "")
        val url = "https://$name/embed?v=" + ("${it.attr("href")}|").findBetween("?v=", "|")
        val headers = mutableMapOf(cookieHeader, "referer" to episode.link!!)
        val qualities = arrayListOf<Episode.Quality>()

        httpClient.get(url, headers).document.select("video#player>source").asyncEach {
            val uri = it.attr("src")
            if (uri != "")
                qualities.add(
                    Episode.Quality(
                        url = uri,
                        quality = it.attr("title"),
                        size = getSize(uri, headers)
                    )
                )
        }
        episode.streamLinks[server] = Episode.StreamLinks(server, qualities, headers)
    }
}