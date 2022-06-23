package ani.saikou.parsers.manga

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.findBetween
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse

class MangaBuddy : MangaParser() {

    override val name = "MangaBuddy"
    override val saveName = "manga_buddy"
    override val hostUrl = "https://mangabuddy.com"

    val headers = mapOf("referer" to hostUrl)

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {

        val res = client.get("$hostUrl/api/manga${mangaLink}/chapters?source=detail")
            .document.select("#chapter-list>li").reversed()

        return res.mapNotNull {
            if (it.select("strong").text().contains("Chapter")) {
                val chap = Regex("(Chapter ([A-Za-z0-9.]+))( ?: ?)?( ?(.+))?").find(it.select("strong").text())?.destructured
                val number: String
                var title: String? = null
                val link = hostUrl + it.select("a").attr("href")
                if (chap != null) {
                    number = chap.component2()
                    title = chap.component5()
                } else {
                    number = it.select("strong").text()
                }
                MangaChapter(number, link, title)
            } else null
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val res = client.get(chapterLink).text
        val cdn = res.findBetween("var mainServer = \"", "\";")
        val arr = res.findBetween("var chapImages = ", "\n")?.trim('\'')?.split(",")

        return (arr ?: return listOf()).map {
            val link = FileUrl("https:$cdn$it", headers)
            MangaImage(link)
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val doc = client.get("$hostUrl/search?status=all&sort=views&q=$query")
            .document.select(".list > .book-item > .book-detailed-item > .thumb > a")

        return doc.mapNotNull {
            if (it.attr("title") != "") {
                ShowResponse(
                    it.attr("title"),
                    it.attr("href"),
                    FileUrl(it.select("img").attr("data-src"), headers)
                )
            }
            else null
        }
    }

}
