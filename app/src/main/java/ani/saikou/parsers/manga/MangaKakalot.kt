package ani.saikou.parsers.manga

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse

class MangaKakalot : MangaParser() {

    override val name = "MangaKakalot"
    override val saveName = "manga_kakalot"
    override val hostUrl = "https://mangakakalot.com"

    val headers = mapOf("referer" to hostUrl)

    override suspend fun loadChapters(mangaLink: String): List<MangaChapter> {

        val res = client.get(mangaLink).document
            .select(
                if (mangaLink.contains("readmanganato.com")) ".row-content-chapter > .a-h"
                else ".chapter-list > .row > span"
            ).reversed()

        return res.mapNotNull {
            val chap = Regex("((?<=Chapter )[0-9.]+)([\\s:]+)?(.+)?").find(it.select("a").text())?.destructured
            if (chap != null) {
                val link = it.select("a").attr("href")
                MangaChapter(link, chap.component1(), chap.component3())
            }
            else null
        }

    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {

        return client.get(chapterLink).document.select(".container-chapter-reader > img").map {
            val file = FileUrl(it.attr("src"), headers)
            MangaImage(file)
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {

        val res = client
            .get("$hostUrl/search/story/${query.replace(" ", "_").replace(Regex("\\W"), "")}")
            .document.select(".story_item")

        return res.mapNotNull {
            if (it.select(".story_name > a").text() != "") {
                ShowResponse(
                    it.select(".story_name > a").text(),
                    it.select("a").attr("href"),
                    FileUrl(it.select("img").attr("src"), headers)
                )
            }
            else null
        }
    }
}