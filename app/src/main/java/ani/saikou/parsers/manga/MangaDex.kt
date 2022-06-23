package ani.saikou.parsers.manga

import ani.saikou.asyncMap
import ani.saikou.client
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse

class MangaDex : MangaParser() {

    override val name = "MangaDex"
    override val saveName = "manga_dex"
    override val hostUrl = "https://mangadex.org"

    val host = "https://api.mangadex.org"

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        val old = showUserText
        setUserText("Getting Chapters...")
        val list = mutableListOf<MangaChapter>()

        val totalChapters = client.get("$host/manga/$mangaLink/feed?limit=0")
            .parsed<MangaResponse>().total ?: return list

        setUserText("Parsing Chapters...")
        (0..totalChapters step 200).reversed().toList().asyncMap { index ->
            val data = client
                .get("$host/manga/$mangaLink/feed?limit=200&order[volume]=desc&order[chapter]=desc&offset=$index")
                .parsed<MangaResponse>().data?.reversed()

            data?.forEach {
                if (it.attributes.translatedLanguage == "en" && it.attributes.externalUrl == null) {
                    val chapter = (it.attributes.chapter ?: return@forEach).toString()
                    val title = it.attributes.title
                    list.add(MangaChapter(chapter, it.id, title))
                }
            }
        }
        list.sortBy { it.number.toFloatOrNull() }
        setUserText(old)
        return list
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val res = client.get("$host/at-home/server/${chapterLink}").parsed<ChapterResponse>().chapter
        return res?.data?.map {
            MangaImage("https://uploads.mangadex.org/data/${res.hash}/${it}")
        } ?: listOf()
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val json = client.get("$host/manga?limit=15&title=$query&order[followedCount]=desc&includes[]=cover_art").parsed<SearchResponse>()
        return json.data?.mapNotNull {
            val id = it.id ?: return@mapNotNull null
            val title = it.attributes?.title?.en ?: return@mapNotNull null
            val coverName = it.relationships?.find { i -> i.type == "cover_art" }?.attributes?.fileName
            val coverURL = if (coverName != null) "https://uploads.mangadex.org/covers/$id/$coverName.256.jpg" else ""
            ShowResponse(title, id, coverURL)
        } ?: listOf()
    }

    private data class SearchResponse(
        val result: String? = null,
        val data: List<Datum>? = null,
        val total: Long? = null
    ) {
        data class Datum(
            val id: String? = null,
            val attributes: DatumAttributes? = null,
            val relationships: List<Relationship>? = null
        )

        data class DatumAttributes(
            val title: Title? = null
        )

        data class Title(
            val en: String? = null
        )

        data class Relationship(
            val id: String? = null,
            val type: String? = null,
            val attributes: RelationshipAttributes? = null
        )

        data class RelationshipAttributes(
            val fileName: String? = null
        )
    }

    private data class MangaResponse(
        val result: String? = null,
        val data: List<Datum>? = null,
        val total: Long? = null
    ) {
        data class Datum(
            val id: String,
            val attributes: Attributes
        )

        data class Attributes(
            val volume: Any? = null,
            val chapter: Any? = null,
            val title: String? = null,
            val translatedLanguage: String? = null,
            val externalUrl : String? = null
        )
    }

    private data class ChapterResponse(
        val result: String? = null,
        val baseURL: String? = null,
        val chapter: Chapter? = null
    ) {
        data class Chapter(
            val hash: String? = null,
            val data: List<String>? = null,
            val dataSaver: List<String>? = null
        )
    }
}
