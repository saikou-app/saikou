package ani.saikou.parsers.manga

import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse

class MangaKatana : MangaParser() {

    override val name = "MangaKatana"
    override val saveName = "manga_katana"
    override val hostUrl = "https://mangakatana.com"

    override suspend fun search(query: String): List<ShowResponse> {
        val doc = client.get("$hostUrl/?search=${encode(query)}").document
        val mediaData = doc.select("#book_list div.media div.wrap_img a")
        val titleData = doc.select("#book_list div.text h3 a")
        val data = titleData.zip(mediaData)
        return data.map {
            val name = it.first.text()
            val link = it.second.attr("href")
            val cover = it.second.select("img").attr("src")
            ShowResponse(name= name, link = link, coverUrl = cover)
        }
    }

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val doc = client.get(mangaLink).document
        val data = doc.select("tbody tr a")
        val chapNumRe = Regex("([0-9]+(?:\\.[0-9]+)?)")
        return data.reversed().map {
            MangaChapter(number = chapNumRe.find(it.text())?.groupValues?.get(1).toString(), link = it.attr("href"))
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val html = client.get(chapterLink).text
        val re = Regex("var\\s+\\w+\\s?=\\s?(\\[['\"].+?['\"]).?\\]\\s?;")
        val match = re.find(html)?.destructured?.toList()?.get(0)?.removePrefix("[")
        return match!!.split(",").map {
            MangaImage(url = it.replace("\"", "").replace("'", ""))
        }
    }

}
