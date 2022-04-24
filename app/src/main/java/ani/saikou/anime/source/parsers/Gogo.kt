package ani.saikou.anime.source.parsers

import android.annotation.SuppressLint
import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.anime.source.Extractor
import ani.saikou.anime.source.extractors.FPlayer
import ani.saikou.anime.source.extractors.GogoCDN
import ani.saikou.anime.source.extractors.StreamSB
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.MalSyncBackup
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@SuppressLint("SetTextI18n")
class Gogo(private val dub: Boolean = false, override val name: String = "gogoanime.cm") : AnimeParser() {


    private val host = listOf(
        "http://gogoanime.fi"
    )

    private fun httpsIfy(text: String): String {
        return if (text.take(2) == "//") "https:$text"
        else text
    }

    private fun directLinkify(name: String, url: String, getSize: Boolean = true): Episode.StreamLinks? {
        val domain = Regex("""(?<=^http[s]?://).+?(?=/)""").find(url)!!.value
        val extractor: Extractor? = when {
            "gogo" in domain    -> GogoCDN(host[0])
            "goload" in domain  -> GogoCDN(host[0])
            "sb" in domain      -> StreamSB()
            "fplayer" in domain -> FPlayer(getSize)
            "fembed" in domain  -> FPlayer(getSize)
            else                -> null
        }
        val a = extractor?.getStreamLinks(name, url)
        if (a != null && a.quality.isNotEmpty()) return a
        return null
    }

    override fun getStream(episode: Episode, server: String): Episode {
        episode.streamLinks = runBlocking {
            val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()
            try {
                httpClient.get(episode.link!!).document.select("div.anime_muti_link > ul > li").forEach {
                    val name = it.select("a").text().replace("Choose this server", "")
                    if (name == server) launch {
                        val directLinks = directLinkify(
                            name,
                            httpsIfy(it.select("a").attr("data-video")),
                            false
                        )
                        if (directLinks != null) {
                            linkForVideos[name] = directLinks
                        }
                    }
                }
            } catch (e: Exception) {
                toastString(e.toString())
            }
            return@runBlocking (linkForVideos)
        }
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        try {
            episode.streamLinks = runBlocking {
                val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()
                httpClient.get(episode.link!!).document.select("div.anime_muti_link > ul > li").forEach {
                    launch {
                        val directLinks = directLinkify(
                            it.select("a").text().replace("Choose this server", ""),
                            httpsIfy(it.select("a").attr("data-video"))
                        )
                        if (directLinks != null) {
                            linkForVideos[directLinks.server] = directLinks
                        }
                    }
                }
                return@runBlocking (linkForVideos)
            }
        } catch (e: Exception) {
            toastString("$e")
        }
        return episode
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        try {
            var slug: Source? = loadData("go-go${if (dub) "dub" else ""}_${media.id}")
            if (slug == null) {
                slug = MalSyncBackup[media.id, "Gogoanime", dub]
                if (slug != null)
                    saveSource(slug, media.id, false)
                else {
                    var it = (media.nameMAL ?: media.nameRomaji) + if (dub) " (Dub)" else ""
                    setTextListener("Searching for $it")
                    logger("Gogo : Searching for $it")
                    var search = search(it)
                    if (search.isNotEmpty()) {
                        slug = search[0]
                        saveSource(slug, media.id, false)
                    } else {
                        it = media.nameRomaji + if (dub) " (Dub)" else ""
                        search = search(it)
                        setTextListener("Searching for $it")
                        logger("Gogo : Searching for $it")
                        if (search.isNotEmpty()) {
                            slug = search[0]
                            saveSource(slug, media.id, false)
                        }
                    }
                }
            } else {
                setTextListener("Selected : ${slug.name}")
            }
            if (slug != null) return getSlugEpisodes(slug.link)
        } catch (e: Exception) {
            toastString("$e")
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        // make search and get all links
        logger("Searching for : $string")
        val responseArray = arrayListOf<Source>()
        try {
            httpClient.get("${host[0]}/search.html?keyword=$string").document
                .select(".last_episodes > ul > li div.img > a").forEach {
                    val link = it.attr("href").toString().replace("/category/", "")
                    val title = it.attr("title")
                    val cover = it.select("img").attr("src")
                    responseArray.add(Source(link, title, cover))
                }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val pageBody = httpClient.get("${host[0]}/category/$slug").document
            val lastEpisode = pageBody.select("ul#episode_page > li:last-child > a").attr("ep_end").toString()
            val animeId = pageBody.select("input#movie_id").attr("value").toString()

            val a = httpClient.get("https://ajax.gogo-load.com/ajax/load-list-episode?ep_start=0&ep_end=$lastEpisode&id=$animeId").document.select("ul > li > a").reversed()
            a.forEach {
                val num = it.select(".name").text().replace("EP", "").trim()
                responseArray[num] = Episode(number = num, link = host[0] + it.attr("href").trim())
            }
            logger("Response Episodes : $responseArray")
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("go-go${if (dub) "dub" else ""}_$id", source)
    }
}
