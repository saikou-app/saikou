package ani.saikou.anime.source.parsers

import android.annotation.SuppressLint
import ani.saikou.anime.Episode
import ani.saikou.anime.newsrc.AnimeProvider
import ani.saikou.loadData
import ani.saikou.logger
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.MalSyncBackup
import ani.saikou.saveData
import ani.saikou.toastString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

@SuppressLint("SetTextI18n")
class Gogo(private val dub: Boolean = false) : AnimeProvider() {
    override val name: String = "GoGoAnime"
    override val isAdult: Boolean = false

    companion object {
        const val host = "gogoanime.fi"
    }

    override fun fetchVideoServers(episode: Episode): Map<String, Episode.VideoServer> =
        runBlocking {
            val videoServers = mutableMapOf<String, Episode.VideoServer>()
            try {
                withContext(Dispatchers.Default) {
                    Jsoup.connect(episode.link!!)
                        .ignoreHttpErrors(true).get()
                        .select("div.anime_muti_link > ul > li")
                        .forEach { element ->
                            val serverName = element.select("a").text().replace("Choose this server", "")
                            val url = element.select("a").attr("data-video")

                            launch {
                                resolveVideoServer(
                                    serverName,
                                    url,
                                    false
                                )?.let { server ->
                                    videoServers[serverName] = server
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                toastString(e.toString())
            }
            return@runBlocking videoServers
        }

    override fun fetchVideoServer(episode: Episode, server: String): Map<String, Episode.VideoServer> =
        runBlocking {
            val videoServers = mutableMapOf<String, Episode.VideoServer>()
            try {
                withContext(Dispatchers.Default) {
                    Jsoup.connect(episode.link!!)
                        .ignoreHttpErrors(true).get()
                        .select("div.anime_muti_link > ul > li")
                        .forEach { element ->
                            val serverName = element.select("a").text().replace("Choose this server", "")
                            val url = element.select("a").attr("data-video")

                            if (serverName == server) {
                                launch {
                                    resolveVideoServer(
                                        serverName,
                                        url
                                    )?.let { server ->
                                        videoServers[serverName] = server
                                    }
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                toastString("$e")
            }
            return@runBlocking videoServers
        }


    override fun getEpisodes(media: Media): Map<String, Episode> {
        try {
            var slug: Source? = loadData("go-go${if (dub) "dub" else ""}_${media.id}")
            if (slug == null) {
                slug = MalSyncBackup[media.id, "Gogoanime", dub]
                if (slug != null)
                    saveSource(slug, media.id, false)
                else {
                    var it = (media.nameMAL ?: media.nameRomaji) + if (dub) " (Dub)" else ""
                    updateStatus("Searching for $it")
                    logger("Gogo : Searching for $it")
                    var search = search(it)
                    if (search.isNotEmpty()) {
                        slug = search[0]
                        saveSource(slug, media.id, false)
                    } else {
                        it = media.nameRomaji + if (dub) " (Dub)" else ""
                        search = search(it)
                        updateStatus("Searching for $it")
                        logger("Gogo : Searching for $it")
                        if (search.isNotEmpty()) {
                            slug = search[0]
                            saveSource(slug, media.id, false)
                        }
                    }
                }
            } else {
                updateStatus("Selected : ${slug.name}")
            }
            if (slug != null) {
                return getEpisodes(slug.link)
            }
        } catch (e: Exception) {
            toastString("$e")
        }
        return mutableMapOf()
    }

    override fun search(string: String): List<Source> {
        // make search and get all links
        logger("Searching for : $string")
        val responseArray = arrayListOf<Source>()
        try {
            Jsoup.connect("https://$host/search.html?keyword=$string").get().body()
                .select(".last_episodes > ul > li div.img > a").forEach {
                    val link = it.attr("href").replace("/category/", "")
                    val title = it.attr("title")
                    val cover = it.select("img").attr("src")
                    responseArray.add(Source(link, title, cover))
                }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun getEpisodes(animeId: String): Map<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val pageBody = Jsoup.connect("https://$host/category/$animeId").get().body()
            val lastEpisode = pageBody.select("ul#episode_page > li:last-child > a").attr("ep_end")
            val episodeId = pageBody.select("input#movie_id").attr("value")

            val a =
                Jsoup.connect("https://ajax.gogo-load.com/ajax/load-list-episode?ep_start=0&ep_end=$lastEpisode&id=$episodeId")
                    .get().body().select("ul > li > a").reversed()
            a.forEach {
                val num = it.select(".name").text().replace("EP", "").trim()
                responseArray[num] = Episode(number = num, link = "https://$host" + it.attr("href").trim())
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
