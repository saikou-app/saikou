package ani.saikou.parsers.manga

import androidx.core.text.isDigitsOnly
import ani.saikou.client
import ani.saikou.parsers.*

class NHentai : MangaParser() {

    override val name     = "NHentai"
    override val saveName = "nhentai_manga"
    override val hostUrl  = "https://nhentai.net"
    override val isNSFW   = true

    override suspend fun loadChapters(mangaLink: String): List<MangaChapter> {
        val id   = mangaLink.substringAfter("g/")
        val json = client.get("$hostUrl/api/gallery/$id").parsed<MangaResponse>()
        // There's really no "chapter(s)" in nhentai afaik. So here it's being returned as if it's the first chapter.
        return arrayListOf(
            MangaChapter(
                number = "1",
                link   = "https://nhentai.net/g/$id",
                title  = json.title.pretty
            )
        )
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val id   = chapterLink.substringAfter("g/")
        val json = client.get("$hostUrl/api/gallery/$id").parsed<MangaResponse>()
        val ext  = ext(json.images.pages[0].t)
        val imageArr = arrayListOf<MangaImage>()
        for (page in 1 until (json.images.pages.size - 1)) {
            val url = "https://i.nhentai.net/galleries/${json.media_id}/$page.$ext"
            imageArr.add(
                MangaImage(url = url)
            )
        }
        return imageArr
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val responseArr = arrayListOf<ShowResponse>()
        if(query.contains("#") || query.isDigitsOnly()) {
            val id = query.replace("#","")
            val document = client.get("https://nhentai.net/g/$id").document
            val coverUrl = document.select("body div#content div#bigcontainer.container div#cover a img.lazyload").attr("data-src")
            val tittle = document.select("div#content div#bigcontainer.container div#info-block div#info h1.title span.pretty").text()
            responseArr.add(
                ShowResponse(
                    name = tittle,
                    link = "https://nhentai.net/g/$id",
                    coverUrl = coverUrl,
                )
            )
            return responseArr
        }else{
        val json = client.get("$hostUrl/api/galleries/search?query=${encode(query)}").parsed<SearchResponse>()
        for (i in json.result) {
            responseArr.add(
                ShowResponse(
                    name = i.title.pretty,
                    link = "https://nhentai.net/g/${i.id}",
                    coverUrl = "https://t.nhentai.net/galleries/${i.media_id}/cover.jpg",
                )
            )
        }
        return responseArr
    }
    }


    // convert to proper extension from API
    private fun ext(t: String): String {
        return when (t) {
            "j" -> "jpg"
            "p" -> "png"
            "g" -> "gif"
            else -> "jpg" // unreachable anyways
        }
    }

    private data class SearchResponse(
        val result: List<Result>,
    ) {
        data class Result(
            val id: Int,
            val media_id: Int,
            val title: Title,
        ) {
             data class Title(
                val english: String,
                val japanese: String,
                val pretty: String
            )
        }
    }

    private data class MangaResponse(
        val media_id: Int,
        val title: Title,
        val images: Pages
    ) {
        data class Title(val pretty: String)
        data class Pages(val pages: List<Page>) {
            data class Page(
                val t: String, // extension
                val w: Int,    // width
                val h: Int     // height
            )
        }
    }
}
