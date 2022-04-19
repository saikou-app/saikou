package ani.saikou.core.source.manga.parsers

import ani.saikou.core.model.manga.MangaChapter
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source
import ani.saikou.core.service.LOG
import ani.saikou.core.service.STORE
import ani.saikou.core.source.manga.MangaParser
import org.jsoup.Jsoup
import java.net.URLEncoder

class MangaPill(override val name: String = "mangapill.com") : MangaParser() {
    override fun getChapter(chapter: MangaChapter): MangaChapter {
        chapter.images = arrayListOf()
        try {
            Jsoup.connect(chapter.link!!).get().select("img.js-page").forEach {
                chapter.images!!.add(it.attr("data-src"))
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return chapter
    }

    override fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        val responseArray = mutableMapOf<String, MangaChapter>()
        try {
            Jsoup.connect(link).get().select("#chapters > div > a").reversed().forEach {
                val chap = it.text().replace("Chapter ", "")
                responseArray[chap] = MangaChapter(chap, link = it.attr("abs:href"))
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return responseArray
    }

    override fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source: Source? = STORE.loadData("mangapill_${media.id}")
        if (source == null) {
            setTextListener("Searching : ${media.mangaName}")
            val search = search(media.mangaName)
            if (search.isNotEmpty()) {
                LOG.log("MangaPill : ${search[0]}")
                source = search[0]
                saveSource(source, media.id, false)
            } else {
                val a = search(media.nameRomaji)
                if (a.isNotEmpty()) {
                    LOG.log("MangaPill : ${a[0]}")
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

    override fun search(string: String): ArrayList<Source> {
        val response = arrayListOf<Source>()
        try {
            Jsoup.connect(
                "https://mangapill.com/quick-search?q=${
                    URLEncoder.encode(
                        string,
                        "utf-8"
                    )
                }"
            ).get().select(".bg-card").forEach {
                val text2 = it.select(".text-sm").text()
                response.add(
                    Source(
                        link = it.attr("abs:href"),
                        name = it.select(".flex .flex-col").text().replace(text2, "").trim(),
                        cover = it.select("img").attr("src")
                    )
                )
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return response
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        STORE.saveData("mangapill_$id", source)
    }
}