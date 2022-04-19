package ani.saikou.core.source.anime.parsers

import ani.saikou.core.model.anime.Episode
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source
import ani.saikou.core.service.LOG
import ani.saikou.core.service.STORE
import ani.saikou.core.source.anime.AnimeParser
import ani.saikou.core.source.anime.Extractor
import ani.saikou.core.source.anime.extractors.FPlayer
import ani.saikou.core.source.anime.extractors.GogoCDN
import ani.saikou.core.source.anime.extractors.StreamSB
import ani.saikou.core.utils.MALSyncBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class Gogo(private val dub: Boolean = false, override val name: String = "gogoanime.cm") :
    AnimeParser() {

    private val base = "go-go${if (dub) "dub" else ""}"

    private val host = listOf(
        "http://gogoanime.fi"
    )

    private fun httpsIfy(text: String): String {
        return if (text.take(2) == "//") "https:$text"
        else text
    }

    private fun directLinkify(
        name: String,
        url: String,
        getSize: Boolean = true
    ): Episode.StreamLinks? {
        val domain = Regex("""(?<=^http[s]?://).+?(?=/)""").find(url)!!.value
        val extractor: Extractor? = when {
            "gogo" in domain -> GogoCDN(host[0])
            "goload" in domain -> GogoCDN(host[0])
            "sb" in domain -> StreamSB()
            "fplayer" in domain -> FPlayer(getSize)
            "fembed" in domain -> FPlayer(getSize)
            else -> null
        }
        val a = extractor?.getStreamLinks(name, url)
        if (a != null && a.quality.isNotEmpty()) return a
        return null
    }

    override fun getStream(episode: Episode, server: String): Episode {
        episode.streamLinks = runBlocking {
            val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()
            try {
                withContext(Dispatchers.Default) {
                    Jsoup.connect(episode.link!!).ignoreHttpErrors(true).get()
                        .select("div.anime_muti_link > ul > li").forEach {
                        val name = it.select("a").text().replace("Choose this server", "")
                        if (name == server)
                            launch {
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
                }
            } catch (e: Exception) {
                LOG.notify(e.toString())
            }
            return@runBlocking (linkForVideos)
        }
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        try {
            episode.streamLinks = runBlocking {
                val linkForVideos = mutableMapOf<String, Episode.StreamLinks?>()
                withContext(Dispatchers.Default) {
                    Jsoup.connect(episode.link!!).ignoreHttpErrors(true).get()
                        .select("div.anime_muti_link > ul > li").forEach {
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
                }
                return@runBlocking (linkForVideos)
            }
        } catch (e: Exception) {
            LOG.notify(e.toString())
        }
        return episode
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        try {
            var slug: Source? = STORE.loadData("${base}_${media.id}")
            if (slug == null) {
                slug = MALSyncBackup[media.id, "Gogoanime", dub]
                if (slug != null)
                    saveSource(slug, media.id, false)
                else {
                    var it = (media.nameMAL ?: media.nameRomaji) + if (dub) " (Dub)" else ""
                    setTextListener("Searching for $it")
                    LOG.log("Gogo : Searching for $it")
                    var search = search(it)
                    if (search.isNotEmpty()) {
                        slug = search[0]
                        saveSource(slug, media.id, false)
                    } else {
                        it = media.nameRomaji + if (dub) " (Dub)" else ""
                        search = search(it)
                        setTextListener("Searching for $it")
                        LOG.log("Gogo : Searching for $it")
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
            LOG.notify(e.toString())
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        // make search and get all links
        LOG.log("Searching for : $string")
        val responseArray = arrayListOf<Source>()
        try {
            Jsoup.connect("${host[0]}/search.html?keyword=$string").get().body()
                .select(".last_episodes > ul > li div.img > a").forEach {
                    val link = it.attr("href").toString().replace("/category/", "")
                    val title = it.attr("title")
                    val cover = it.select("img").attr("src")
                    responseArray.add(Source(link, title, cover))
                }
        } catch (e: Exception) {
            LOG.notify(e.toString())
        }
        return responseArray
    }

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val pageBody = Jsoup.connect("${host[0]}/category/$slug").get().body()
            val lastEpisode =
                pageBody.select("ul#episode_page > li:last-child > a").attr("ep_end").toString()
            val animeId = pageBody.select("input#movie_id").attr("value").toString()

            val a =
                Jsoup.connect("https://ajax.gogo-load.com/ajax/load-list-episode?ep_start=0&ep_end=$lastEpisode&id=$animeId")
                    .get().body().select("ul > li > a").reversed()
            a.forEach {
                val num = it.select(".name").text().replace("EP", "").trim()
                responseArray[num] = Episode(number = num, link = host[0] + it.attr("href").trim())
            }
            LOG.log("Response Episodes : $responseArray")
        } catch (e: Exception) {
            LOG.notify(e.toString())
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        STORE.saveData("${base}_$id", source)
    }
}
