package ani.saikou.parsers.manga

import ani.saikou.client
import ani.saikou.parsers.*

class NHentai : MangaParser() {

    override val name     = "NHentai"
    override val saveName = "nhentai_manga"
    override val hostUrl  = "https://nhentai.net"
    override val isNSFW   = true

    override suspend fun loadChapters(mangaLink: String): List<MangaChapter> {
        TODO("Not yet implemented")
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val id   = chapterLink.substringAfter("g/")
        val json = client.get("$hostUrl/api/gallery/$id").parsed<MangaImageResponse>()
        val ext  = ext(json.images.pages[0].t)
        val imageArr = arrayListOf<MangaImage>()
        for (page in 1 until (json.images.pages.size - 1)) {
            imageArr.add(
                MangaImage(url = "https://i.nhentai.net/galleries/${json.media_id}/$page}.$ext")
            )
        }
        return imageArr
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val responseArr = arrayListOf<ShowResponse>()
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

    private data class MangaImageResponse(
        val media_id: Int,
        val images: Pages
    ) {
        data class Pages(
            val pages: List<Page>
        ) {
            data class Page(
                val t: String, // extension (.jpg, .png)
                val w: Int,    // width
                val h: Int     // height
            )
        }
    }
}
