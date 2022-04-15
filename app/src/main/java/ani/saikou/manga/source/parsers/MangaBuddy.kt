package ani.saikou.manga.source.parsers

import ani.saikou.*
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import org.jsoup.Jsoup

class MangaBuddy(override val name: String = "mangabuddy.com") : MangaParser() {

    override fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        val arr = mutableMapOf<String, MangaChapter>()
        try {
            Jsoup.connect("https://mangabuddy.com/api/manga$link/chapters?source=detail").get().select("#chapter-list>li").reversed().forEach {
                if (it.select("strong").text().contains("Chapter")) {
                    val chap = Regex("(Chapter ([A-Za-z0-9.]+))( ?: ?)?( ?(.+))?").find(it.select("strong").text())?.destructured
                    if (chap != null) {
                        arr[chap.component2()] = MangaChapter(
                            number = chap.component2(),
                            link = it.select("a").attr("abs:href"),
                            title = chap.component5()
                        )
                    } else {
                        arr[it.select("strong").text()] = MangaChapter(
                            number = it.select("strong").text(),
                            link = it.select("a").attr("abs:href"),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            toastString("$e")
        }
        return arr
    }

    override fun getChapter(chapter: MangaChapter): MangaChapter {
        chapter.images = arrayListOf()
        try {
            val res = Jsoup.connect(chapter.link!!).get().toString()
            val cdn = res.findBetween("var mainServer = \"", "\";")
            val arr = res.findBetween("var chapImages = ", "\n")?.trim('\'')?.split(",")
            arr?.forEach {
                val link = "https:$cdn$it"
                chapter.images!!.add(link)
            }
            chapter.headers = mutableMapOf("referer" to "https://mangabuddy.com/")
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return chapter
    }

    override fun getChapters(media: Media): MutableMap<String, MangaChapter> {
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

    override fun search(string: String): ArrayList<Source> {
        val response = arrayListOf<Source>()
        try {
            Jsoup.connect("https://mangabuddy.com/search?status=all&sort=views&q=$string").get().select(".list > .book-item > .book-detailed-item > .thumb > a").forEach {
                if (it.attr("title") != "") {
                    response.add(
                        Source(
                            link = it.attr("href"),
                            name = it.attr("title"),
                            cover = it.select("img").attr("data-src"),
                            headers = mutableMapOf("referer" to "https://mangabuddy.com/")
                        )
                    )
                }
            }
        } catch (e: Exception) { toastString(e.toString()) }
        return response
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("mangabuddy_$id", source)
    }
}
