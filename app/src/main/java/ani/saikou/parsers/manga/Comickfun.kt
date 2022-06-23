package ani.saikou.parsers.manga

import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse

class Comickfun : MangaParser() {

    override val name = "Comickfun"
    override val saveName = "comick_fun"
    override val hostUrl = "https://api.comick.fun"

    override suspend fun search(query: String): List<ShowResponse> {
        val resp = client.get("https://api.comick.fun/search?q=${encode(query)}&tachiyomi=true").parsed<List<SearchData>>()
        return resp.map { manga ->
            val mangaLink = "$hostUrl/comic/${manga.id}/chapter?tachiyomi=true"
            ShowResponse(name = manga.title, link = mangaLink, coverUrl = manga.cover_url, otherNames = manga.md_titles.map { it.title },
                extra = mapOf("slug" to manga.slug)) // need this slug for loadChapters
        }
    }

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val resp = client.get(mangaLink).parsed<MangaChapterData>()
        val filtered = resp.chapters.filter { chapter -> chapter.lang == "en" }
        val weirdUrl = "https://comick.fun/_next/data/AME6vwUgUUAUopPyx9QYb/comic/${extra!!["slug"]}/${filtered[0].hid}-chapter-0-en.json"
        val secondResp = client.get(weirdUrl).parsed<WeirdUrlData>()
        return secondResp.pageProps.chapters.reversed().map {
            val chapterLink = "$hostUrl/chapter/${it.hid}?tachiyomi=true"
            MangaChapter(number = it.chap.toString(), link = chapterLink, title = null)
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val resp = client.get(chapterLink).parsed<MangaImageData>()
        return resp.chapter.images.map { MangaImage(url = it.url) }
    }
}

// --- dataclasses --- //

private data class WeirdUrlData(val pageProps: Data) {
    data class Data(val chapters: List<Chapters>) {
        data class Chapters(
            val chap: String?, // chapter number
            val hid: String,
        )
    }
}

private data class SearchData(
    val title: String,
    val id: Int,
    val slug: String,
    val md_titles: List<MdTitles>,
    val cover_url: String,
) {
    data class MdTitles(
        val title: String, // other titles
    )
}

private data class MangaChapterData(val chapters: List<Chapter>) {
    data class Chapter(
        val chap: String?,  // chapter number
        val title: String?,
        val lang: String?,  // may contain other lang too, so filter "en" using this
        val hid: String,
    )
}

private data class MangaImageData(val chapter: Chapter) {
    data class Chapter(val images: List<Image>) {
        data class Image(val url: String)
    }
}
