package ani.saikou.anime.source.parsers

import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.asyncEach
import ani.saikou.others.logError
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.Requests
import org.jsoup.nodes.Element
import java.net.URI

open class Tenshi(override val name: String = "tenshi.moe") : AnimeParser() {
    var cookieHeader = "Cookie" to "__ddg1_=;__ddg2_=;loop-view=thumb"

    override suspend fun getStream(episode: Episode, server: String): Episode {
        try {
            episode.streamLinks = mutableMapOf()
            val htmlResponse = httpClient.get(episode.link!!, mapOf(cookieHeader)).document
            htmlResponse.select("ul.dropdown-menu > li > a.dropdown-item").asyncEach {
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
            val htmlResponse = httpClient.get(episode.link!!, mapOf(cookieHeader)).document
            htmlResponse.select("ul.dropdown-menu > li > a.dropdown-item").asyncEach {
                load(episode, it)
            }

        } catch (e: Exception) {
            logError(e)
        }
        return episode
    }

    private data class Player(
        val sources: List<PlayerSource>
    ) {
        data class PlayerSource(
            val size: Long? = null,
            val src: String? = null
        )
    }

    open suspend fun load(episode: Episode, it: Element) {
        var server = it.text().replace(" ","").replace("/-", "")
        val dub = it.select("[title=Audio: English]").first() != null
        server = if(dub) "Dub - $server" else server
        val headers = mutableMapOf(cookieHeader, "referer" to episode.link!!)
        val url = "https://$name/embed?" + URI(it.attr("href")).query

        val unSanitized = httpClient.get(url, headers).text.substringAfter("player.source = ").substringBefore(';')

        val json = Requests.mapper.readValue<Player>(
            Regex("""([a-z0-9A-Z_]+): """)
                .replace(unSanitized, "\"$1\" : ")
                .replace('\'', '"')
                .replace("\n", "").replace(" ", "").replace(",}", "}").replace(",]", "]")
        )

        val qualities = arrayListOf<Episode.Quality>()

        json.sources.asyncEach { i ->

            val uri = i.src
            if (uri != null)
                qualities.add(
                    Episode.Quality(
                        url = uri,
                        quality = "${i.size}p",
                        size = getSize(uri,headers)
                    )
                )

        }
        episode.streamLinks[server] = Episode.StreamLinks(server, qualities, headers)
    }

    override suspend fun getEpisodes(media: Media): MutableMap<String, Episode> {
        try {
            var slug: Source? = loadData("tenshi_${media.id}")
            if (slug == null) {
                suspend fun s(it: String): Boolean {
                    setTextListener("Searching for $it")
                    logger("Tenshi : Searching for $it")
                    val search = search(it)
                    if (search.isNotEmpty()) {
                        slug = search[0]
                        saveSource(slug!!, media.id, false)
                        return true
                    }
                    return false
                }
                if (!s(media.nameMAL ?: media.name))
                    s(media.nameRomaji)
            } else {
                setTextListener("Selected : ${slug!!.name}")
            }
            if (slug != null) return getSlugEpisodes(slug!!.link)
        } catch (e: Exception) {
            logError(e)
        }
        return mutableMapOf()
    }

    override suspend fun search(string: String): ArrayList<Source> {
        logger("Searching for : $string")
        val responseArray = arrayListOf<Source>()
        try {
            val htmlResponse = httpClient.get("https://$name/anime?q=$string&s=vtt-d", mapOf(cookieHeader))
            htmlResponse.document.select("ul.loop.anime-loop.thumb > li > a").forEach {
                responseArray.add(
                    Source(
                        link = it.attr("abs:href"),
                        name = it.attr("title"),
                        cover = it.select(".image")[0].attr("src"),
                        headers = mutableMapOf(cookieHeader)
                    )
                )
            }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    override suspend fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val htmlResponse = httpClient.get(slug, mapOf(cookieHeader)).document
            (1..htmlResponse.select(".entry-episodes > h2 > span.badge.badge-secondary.align-top").text().toInt()).forEach {
                responseArray[it.toString()] = Episode(it.toString(), link = "${slug}/$it")
            }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("tenshi_$id", source)
    }
}