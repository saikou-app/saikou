package ani.saikou.manga.source.parsers

import ani.saikou.*
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.logError

class MangaBuddy(override val name: String = "mangabuddy.com") : MangaParser() {

    val host = "https://mangabuddy.com"
    override suspend fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        val arr = mutableMapOf<String, MangaChapter>()
        try {
            httpClient.get("$host/api/manga${link}/chapters?source=detail").document.select("#chapter-list>li")
                .reversed().forEach {
                if (it.select("strong").text().contains("Chapter")) {
                    val chap = Regex("(Chapter ([A-Za-z0-9.]+))( ?: ?)?( ?(.+))?").find(it.select("strong").text())?.destructured
                    if (chap != null) {
                        arr[chap.component2()] = MangaChapter(
                            number = chap.component2(),
                            link = host + it.select("a").attr("href"),
                            title = chap.component5()
                        )
                    } else {
                        arr[it.select("strong").text()] = MangaChapter(
                            number = it.select("strong").text(),
                            link = host + it.select("a").attr("href"),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
        return arr
    }

    override suspend fun getChapter(chapter: MangaChapter): MangaChapter {
        chapter.images = arrayListOf()
        try {
            val res = httpClient.get(chapter.link!!).text
            val cdn = res.findBetween("var mainServer = \"", "\";")
            val arr = res.findBetween("var chapImages = ", "\n")?.trim('\'')?.split(",")
            arr?.forEach {
                val link = "https:$cdn$it"
                chapter.images!!.add(link)
            }
            chapter.headers = mutableMapOf("referer" to host)
        } catch (e: Exception) {
            logError(e)
        }
        return chapter
    }

    override suspend fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source: Source? = loadData("mangabuddy_${media.id}")
        if (source == null) {
            setTextListener("Searching : ${media.getMangaName()}")
            val search = search(media.getMangaName())
            if (search.isNotEmpty()) {
                logger("MangaBuddy : ${search[0]}")
                source = search[0]
                setTextListener("Found : ${source.name}")
                saveSource(source, media.id)
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
            httpClient.get("$host/search?status=all&sort=views&q=$string").document
                .select(".list > .book-item > .book-detailed-item > .thumb > a").forEach {
                if (it.attr("title") != "") {
                    response.add(
                        Source(
                            link = it.attr("href"),
                            name = it.attr("title"),
                            cover = it.select("img").attr("data-src"),
                            headers = mutableMapOf("referer" to host)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
        return response
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("mangabuddy_$id", source)
    }
}