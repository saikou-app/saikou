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
        TODO("Not yet implemented")
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val responseArr = arrayListOf<ShowResponse>()
        val json = client.get("$hostUrl/api/galleries/search?query=${encode(query)}").parsed<SearchResponse>()
        for (i in json.result) {
            responseArr.add(
                ShowResponse(
                    name = i.title.pretty,
                    link = "https://nhentai.net/galleries/${i.id}",
                    coverUrl = "https://t.nhentai.net/galleries/${i.media_id}/cover.jpg",
                )
            )
        }
        return responseArr
    }


    private data class SearchResponse(
        val result: List<Result>,
        val num_pages: Int,
        val per_page: Int
    ) {
        data class Result(
            val id: Int,
            val media_id: Int,
            val title: Title,
            val images: Pages,
            val upload_date: Int
        ) {
             data class Title(
                val english: String,
                val japanese: String,
                val pretty: String
            )
            data class Pages(
                val pages: List<Page>
            ) {
                data class Page(
                    val t: String,
                    val w: Int,
                    val h: Int
                )
            }
        }
    }
}
