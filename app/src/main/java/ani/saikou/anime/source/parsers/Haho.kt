package ani.saikou.anime.source.parsers

import ani.saikou.anime.Episode
import ani.saikou.findBetween
import ani.saikou.getSize
import ani.saikou.toastString
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class Haho(name: String = "haho.moe") : Tenshi(name) {
    override fun getStream(episode: Episode, server: String): Episode {
        try {
            runBlocking {
                val asy = arrayListOf<Deferred<*>>()
                episode.streamLinks = mutableMapOf()
                Jsoup.connect(episode.link!!).header("Cookie", cookie).get()
                    .select("ul.dropdown-menu > li > a.dropdown-item").forEach {
                    asy.add(async {
                        val a = it.text().replace(" ", "").replace("/-", "")
                        if (server == a)
                            load(episode, it)
                    })
                }
                asy.awaitAll()
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        try {
            runBlocking {
                val asy = arrayListOf<Deferred<*>>()
                episode.streamLinks = mutableMapOf()
                Jsoup.connect(episode.link!!).header("Cookie", cookie).get()
                    .select("ul.dropdown-menu > li > a.dropdown-item").forEach {
                    asy.add(async {
                        load(episode, it)
                    })
                }
                asy.awaitAll()
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return episode
    }

    override fun load(episode: Episode, it: Element) {
        val server = it.text().replace(" ", "").replace("/-", "")
        val url = "https://$name/embed?v=" + ("${it.attr("href")}|").findBetween("?v=", "|")
        val a = arrayListOf<Deferred<*>>()
        val headers = mutableMapOf("Cookie" to cookie, "referer" to url)
        val qualities = arrayListOf<Episode.Quality>()
        runBlocking {
            Jsoup.connect(url)
                .headers(mutableMapOf("Cookie" to cookie, "referer" to episode.link!!)).get()
                .select("video#player>source").forEach {
                a.add(async {
                    val uri = it.attr("src")
                    if (uri != "")
                        qualities.add(
                            Episode.Quality(
                                url = uri,
                                quality = it.attr("title"),
                                size = getSize(uri, headers)
                            )
                        )
                })
            }
            a.awaitAll()
        }
        episode.streamLinks[server] = Episode.StreamLinks(server, qualities, headers)
    }
}