package ani.saikou.core.source.anime.parsers

import android.util.Base64
import ani.saikou.core.model.anime.Episode
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source
import ani.saikou.core.service.LOG
import ani.saikou.core.service.STORE
import ani.saikou.core.source.anime.AnimeParser
import ani.saikou.core.source.anime.extractors.FPlayer
import ani.saikou.core.util.extension.findBetween
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup

class HentaiFF(override val name: String = "HentaiFF") : AnimeParser() {

    private val host = "https://hentaiff.com"

    private fun getCdnViewLink(name: String, link: String): Episode.StreamLinks? {
        return try {
            val a = Jsoup.connect(link).get().select("source").attr("abs:src")
            Episode.StreamLinks(name, arrayListOf(Episode.Quality(a, "Multi Quality", null)), null)
        } catch (e: Exception) {
            LOG.notify(e)
            null
        }
    }

    override fun getStream(episode: Episode, server: String): Episode {
        try {
            Jsoup.connect(episode.link!!).get().select("select.mirror>option").forEach {
                val base64 = it.attr("value")
                val link =
                    String(Base64.decode(base64, Base64.DEFAULT)).findBetween("src=\"", "\" ")
                if (!link.isNullOrEmpty() && server == it.text()) {
                    val a = when {
                        link.contains("amhentai") -> FPlayer(true).getStreamLinks(it.text(), link)
                        link.contains("cdnview") -> getCdnViewLink(it.text(), link)
                        else -> null
                    }
                    if (a != null) episode.streamLinks[it.text()] = a
                }
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        try {
            runBlocking {
                Jsoup.connect(episode.link!!).get().select("select.mirror>option").forEach {
                    val base64 = it.attr("value")
                    val link =
                        String(Base64.decode(base64, Base64.DEFAULT)).substringAfter("src=\"")
                            .substringBefore('"')
                    if (link.isNotEmpty()) {
                        launch {
                            val a = when {
                                link.contains("amhentai") -> FPlayer(true).getStreamLinks(
                                    it.text(),
                                    link
                                )
                                link.contains("cdnview") -> getCdnViewLink(it.text(), link)
                                else -> null
                            }
                            if (a != null) episode.streamLinks[it.text()] = a
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return episode
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        var slug: Source? = STORE.loadData("hentaiff_${media.id}")
        if (slug == null) {
            val it = media.nameMAL ?: media.name
            setTextListener("Searching for $it")
            LOG.log("HentaiFF : Searching for $it")
            val search = search(it)
            if (search.isNotEmpty()) {
                if (search.isNotEmpty()) {
                    slug = search[0]
                    saveSource(slug, media.id, false)
                }
            }
        } else {
            setTextListener("Selected : ${slug.name}")
        }
        if (slug != null) return getSlugEpisodes(slug.link)
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        val responseArray = arrayListOf<Source>()
        try {
            Jsoup.connect("${host}/?s=$string").get().body()
                .select(".bs>.bsx>a").forEach {
                    val link = it.attr("href").toString()
                    val title = it.attr("title")
                    val cover = it.select("img").attr("src")
                    responseArray.add(Source(link, title, cover))
                }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return responseArray
    }

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val pageBody = Jsoup.connect(slug).get().body()
            val notRaw = arrayListOf<Episode>()
            val raw = arrayListOf<Episode>()
            pageBody.select("div.eplister>ul>li>a").reversed().forEach { i ->
                i.select(".epl-num").text().split(" ").apply {
                    val num = this[0]
                    val title = this[1]
                    (if (title == "RAW") raw else notRaw).add(
                        Episode(
                            number = num,
                            link = i.attr("href"),
                            title = title
                        )
                    )
                }
            }
            raw.map { responseArray[it.number] = it }
            notRaw.map { responseArray[it.number] = it }
            LOG.log("Response Episodes : $responseArray")
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        STORE.saveData("hentaiff_$id", source)
    }
}