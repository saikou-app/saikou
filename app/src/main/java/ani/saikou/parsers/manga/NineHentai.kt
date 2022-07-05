package ani.saikou.parsers.manga

import ani.saikou.client
import ani.saikou.media.Media
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class NineHentai : MangaParser() {

    override val name = "9Hentai"
    override val saveName = "nine_hentai"
    override val hostUrl = "https://9hentai.to"
    override val isNSFW = true

    override suspend fun search(query: String): List<ShowResponse> {
        //val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        //val requestBody = RequestBody.create(mediaType, "{\"search\":{\"text\":\"${encode(query)}\",\"page\":0,\"sort\":0,\"pages\":{\"range\":[0,2000]},\"tag\":{\"text\":\"\",\"type\":1,\"tags\":[],\"items\":{\"included\":[],\"excluded\":[]}}}}")
        val resp = client.post(
            "https://9hentai.to/api/getBook", json = mapOf()
            //requestBody = requestBody
        ).parsed<SearchResponse>()
        if (resp.status) {
            return resp.results.map {
                // we don't need ShowResponse.link here, we already got what we need
                // and have passed it to ShowResponse.extra for loadChapters()
                ShowResponse(name = it.title, link = "", coverUrl = "${it.image_server}/${it.id}/cover.jpg", otherNames = listOf(it.alt_title),
                    extra = mapOf(
                        "imageServer" to it.image_server,
                        "totalPages" to it.total_page.toString(),
                        "title" to it.title,
                        "id" to it.id.toString(),
                    )
                )
            }
        }
        return emptyList()
    }

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val imageServer = extra!!["imageServer"]
        val totalPages = extra["totalPages"]
        val id = extra["id"]
        // There's no "chapters" here on 9hentai so we just return it as the first chapter.
        return listOf(
            MangaChapter(number = "1", link = "$imageServer$id/TOTALPAGES=$totalPages", title = extra["title"])
        )
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val url = chapterLink.substringBefore("TOTALPAGES=")
        val totalPages = chapterLink.substringAfter("TOTALPAGES=").toInt()
        return (1..totalPages).map {
            MangaImage(url = "$url$it.jpg")
        }
    }

}

private data class SearchResponse(
    val status: Boolean,
    val results: List<Result>
) {
    data class Result(
        val id: Int,
        val title: String,
        val alt_title: String,
        val total_page: Int,
        val image_server: String,
    )
}
