package ani.saikou.parsers.manga

import android.util.Log
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
        val resp = client.get("$hostUrl/search?q=${encode(query)}").document
        val data = resp.select("#mangalist div.media-left")
        return data.map { manga ->
            val link = manga.select("a").attr("href")
            val name = manga.select("img").attr("alt")
            val cover = manga.select("img").attr("src")
            Log.d("infos", "name - $name | link - $link | cover - $cover")
            ShowResponse(name = name, link = link, coverUrl = cover)
        }
    }

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        Log.d("mangalink", mangaLink)
        val doc = client.get(mangaLink).document
        val chapterLinks = doc.select("#noanim-content-tab > div a").map { it.attr("href") }
        Log.d("chapterLinks", chapterLinks.toString())
        return chapterLinks.reversed().map {
            Log.d("chapterlinkit", it.toString())
            MangaChapter(number = it.substringAfter("chapter-"), link = it)
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val doc = client.get(chapterLink).document
        val p = doc.selectFirst("p")?.text()!!
        Log.d("p", p)
        val firstPage = p.substringBefore("/").toInt()
        val totalPage = p.substringAfter("/").toInt()
        Log.d("firsttotalpage", "$firstPage / $totalPage")
        val chap = chapterLink.substringAfter("chapter-")
        val slug = doc.select("div > img:nth-child(2)").attr("src").substringAfter("imghub/").substringBefore("/")
        Log.d("chapslug", "$chap $slug")
        return (firstPage..totalPage).map {
            MangaImage(url = "https://img.mghubcdn.com/file/imghub/$slug/$chap/$it.jpg")
        }
    }

}
