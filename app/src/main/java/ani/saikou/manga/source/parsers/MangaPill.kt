package ani.saikou.manga.source.parsers

import ani.saikou.httpClient
import ani.saikou.loadData
import ani.saikou.logger
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.logError
import ani.saikou.saveData
import java.net.URLEncoder

@Suppress("BlockingMethodInNonBlockingContext")
class MangaPill(override val name: String = "mangapill.com") : MangaParser() {
    override suspend fun getChapter(chapter: MangaChapter): MangaChapter {
        chapter.images = arrayListOf()
        try {
            httpClient.get(chapter.link!!).document.select("img.js-page").forEach {
                chapter.images!!.add(it.attr("data-src"))
            }
        } catch (e: Exception) {
            logError(e)
        }
        return chapter
    }

    override suspend fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        val responseArray = mutableMapOf<String, MangaChapter>()
        try {
            httpClient.get(link).document.select("#chapters > div > a").reversed().forEach {
                val chap = it.text().replace("Chapter ", "")
                responseArray[chap] = MangaChapter(chap, link = "https://mangapill.com"+it.attr("href"))
            }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    override suspend fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source: Source? = loadData("mangapill_${media.id}")
        if (source == null) {
            setTextListener("Searching : ${media.getMangaName()}")
            val search = search(media.getMangaName())
            if (search.isNotEmpty()) {
                logger("MangaPill : ${search[0]}")
                source = search[0]
                saveSource(source, media.id, false)
            } else {
                val a = search(media.nameRomaji)
                if (a.isNotEmpty()) {
                    logger("MangaPill : ${a[0]}")
                    source = a[0]
                    saveSource(source, media.id, false)
                }
            }
        } else {
            setTextListener("Selected : ${source.name}")
        }
        if (source != null) return getLinkChapters(source.link)
        return mutableMapOf()
    }

    override suspend fun search(string: String): ArrayList<Source> {
        val response = arrayListOf<Source>()
        try {
            httpClient.get("https://mangapill.com/quick-search?q=${URLEncoder.encode(string, "utf-8")}").document.select(".bg-card")
                .forEach {
                    response.add(
                        Source(
                            link = "https://mangapill.com"+it.attr("href"),
                            name = it.select(".font-black").text(),
                            cover = it.select("img").attr("src")
                        )
                    )
                }
        } catch (e: Exception) {
            logError(e)
        }
        return response
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("mangapill_$id", source)
    }
}