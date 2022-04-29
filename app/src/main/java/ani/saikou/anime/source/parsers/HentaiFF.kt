package ani.saikou.anime.source.parsers

import android.util.Base64
import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.anime.source.extractors.FPlayer
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.asyncEach
import ani.saikou.others.logError

class HentaiFF(override val name: String = "HentaiFF") : AnimeParser() {

    private val host = "https://hentaiff.com"

    private suspend fun getCdnViewLink(name: String, link: String): Episode.StreamLinks? {
        return try {
            val a = httpClient.get(link).document.select("source").attr("abs:src")
            Episode.StreamLinks(name, arrayListOf(Episode.Quality(a, "Multi Quality", null)), null)
        } catch (e: Exception) {
            logError(e)
            null
        }
    }

    override suspend fun getStream(episode: Episode, server: String): Episode {
        try {
            httpClient.get(episode.link!!).document.select("select.mirror>option").forEach {
                val base64 = it.attr("value")
                val link = String(Base64.decode(base64, Base64.DEFAULT)).findBetween("src=\"", "\" ")
                if (!link.isNullOrEmpty() && server == it.text()) {
                    val a = when {
                        link.contains("amhentai") -> FPlayer(true).getStreamLinks(it.text(), link)
                        link.contains("cdnview")  -> getCdnViewLink(it.text(), link)
                        else                      -> null
                    }
                    if (a != null) episode.streamLinks[it.text()] = a
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
        return episode
    }

    override suspend fun getStreams(episode: Episode): Episode {
        try {
            httpClient.get(episode.link!!).document.select("select.mirror>option").asyncEach {
                val base64 = it.attr("value")
                val link = String(Base64.decode(base64, Base64.DEFAULT)).substringAfter("src=\"").substringBefore('"')
                if (link.isNotEmpty()) {
                    val a = when {
                        link.contains("amhentai") -> FPlayer(true).getStreamLinks(it.text(), link)
                        link.contains("cdnview")  -> getCdnViewLink(it.text(), link)
                        else                      -> null
                    }
                    if (a != null) episode.streamLinks[it.text()] = a
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
        return episode
    }

    override suspend fun getEpisodes(media: Media): MutableMap<String, Episode> {
        var slug: Source? = loadData("hentaiff_${media.id}")
        if (slug == null) {
            val it = media.nameMAL ?: media.name
            setTextListener("Searching for $it")
            logger("HentaiFF : Searching for $it")
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

    override suspend fun search(string: String): ArrayList<Source> {
        val responseArray = arrayListOf<Source>()
        try {
            httpClient.get("${host}/?s=$string").document.body()
                .select(".bs>.bsx>a").forEach {
                    val link = it.attr("href").toString()
                    val title = it.attr("title")
                    val cover = it.select("img").attr("src")
                    responseArray.add(Source(link, title, cover))
                }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    override suspend fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        try {
            val pageBody = httpClient.get(slug).document.body()
            val notRaw = arrayListOf<Episode>()
            val raw = arrayListOf<Episode>()
            pageBody.select("div.eplister>ul>li>a").reversed().forEach { i ->
                i.select(".epl-num").text().split(" ").apply {
                    val num = this[0]
                    val title = this[1]
                    (if (title == "RAW") raw else notRaw).add(Episode(number = num, link = i.attr("href"), title = title))
                }
            }
            raw.map { responseArray[it.number] = it }
            notRaw.map { responseArray[it.number] = it }
            logger("Response Episodes : $responseArray")
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("hentaiff_$id", source)
    }
}