package ani.saikou.manga.source.parsers

import ani.saikou.*
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import org.jsoup.Jsoup
import java.net.URLEncoder

class MangaReaderTo(override val name: String = "MangaReader") : MangaParser() {
    private val host = "https://mangareader.to"
    private val transformation = MangaReaderToTransformation()

    override fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        val responseArray = mutableMapOf<String, MangaChapter>()
        try {
            Jsoup.connect(link).get().select("#en-chapters > .chapter-item > a").reversed()
                .forEach {
                    it.attr("title").apply {
                        val chap = findBetween("Chapter ", ":")!!
                        val title = subSequence(indexOf(":") + 1, length).toString()
                        responseArray[chap] =
                            MangaChapter(chap, link = it.attr("abs:href"), title = title)
                    }
                }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray

    }

    override fun getChapter(chapter: MangaChapter): MangaChapter {
        chapter.images = arrayListOf()
        try {
            val id = Jsoup.connect(chapter.link!!).get().select("#wrapper").attr("data-reading-id")
            val res =
                Jsoup.connect("$host/ajax/image/list/chap/$id?mode=vertical&quality=high&hozPageSize=1")
                    .ignoreContentType(true).execute().body().replace("\\n", "\n")
                    .replace("\\\"", "\"")
            val element = Jsoup.parse(
                res.findBetween("""{"status":true,"html":"""", """"}""") ?: return chapter
            )
            var a = element.select(".iv-card.shuffled")
            chapter.transformation = transformation
            if (a.isEmpty()) {
                a = element.select(".iv-card")
                chapter.transformation = null
            }
            a.forEach {
                chapter.images!!.add(it.attr("data-url"))
            }

        } catch (e: Exception) {
            toastString(e.toString())
        }
        return chapter
    }

    override fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source: Source? = loadData("mangareader_${media.id}")
        if (source == null) {
            setTextListener("Searching : ${media.getMainName()}")
            val search = search(media.getMainName())
            if (search.isNotEmpty()) {
                logger("MangaReader : ${search[0]}")
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
        val responseArray = arrayListOf<Source>()
        try {
            val url = URLEncoder.encode(string, "utf-8")
            val res = Jsoup.connect("$host/ajax/manga/search/suggest?keyword=$url")
                .ignoreContentType(true).execute().body().replace("\\n", "\n").replace("\\\"", "\"")
            val element = Jsoup.parse(
                res.findBetween("""{"status":true,"html":"""", """"}""") ?: return responseArray
            )
            element.select("a:not(.nav-bottom)").forEach {
                val link = host + it.attr("href")
                val title = it.select(".manga-name").text()
                val cover = it.select(".manga-poster-img").attr("src")
                responseArray.add(Source(link, title, cover))
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        setTextListener("${if (selected) "Selected" else "Found"} : ${source.name}")
        saveData("mangareader_$id", source)
    }
}