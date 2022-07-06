package ani.saikou.parsers.manga

import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse

class MangaHub : MangaParser() {

    override val name = "MangaHub"
    override val saveName = "manga_hub"
    override val hostUrl = "https://mangahub.io"

    override suspend fun search(query: String): List<ShowResponse> {
        val doc = client.get("$hostUrl/search?q=${encode(query)}").document
        val data = doc.select("#mangalist div.media-left")
        return data.map { manga ->
            val link = manga.select("a").attr("href")
            val name = manga.select("img").attr("alt")
            val cover = manga.select("img").attr("src")
            ShowResponse(name = name, link = link, coverUrl = cover)
        }
    }

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val doc = client.get(mangaLink).document
        val chapterLinks = doc.select("#noanim-content-tab > div a").map { it.attr("href") }
        return chapterLinks.reversed().map {
            MangaChapter(number = it.substringAfter("chapter-"), link = it)
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val doc = client.get(chapterLink).document
        val p = doc.selectFirst("p")?.text()!!
        val firstPage = p.substringBefore("/").toInt()
        val totalPage = p.substringAfter("/").toInt()
        val chap = chapterLink.substringAfter("chapter-")
        val slug = doc.select("div > img:nth-child(2)").attr("src").substringAfter("imghub/").substringBefore("/")
        return (firstPage..totalPage).map {
            MangaImage(url = "https://img.mghubcdn.com/file/imghub/$slug/$chap/$it.jpg")
        }
    }

}
