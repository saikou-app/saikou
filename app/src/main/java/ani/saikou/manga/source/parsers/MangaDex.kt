package ani.saikou.manga.source.parsers

import ani.saikou.httpClient
import ani.saikou.loadData
import ani.saikou.logger
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.asyncEach
import ani.saikou.others.logError
import ani.saikou.saveData

class MangaDex(override val name: String = "mangadex.org") : MangaParser() {
    private val host = "https://api.mangadex.org"
    private val limit = 15
    override suspend fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        setTextListener("Getting Chapters...")
        val arr = mutableMapOf<String, MangaChapter>()
        try {
            val totalChapters = httpClient.get("$host/manga/$link/feed?limit=0").parsed<MangaResponse>().total?:return mutableMapOf()
            setTextListener("Parsing Chapters...")
            (0..totalChapters step 200).reversed().toList().asyncEach{ index ->
                httpClient.get("$host/manga/$link/feed?limit=200&order[volume]=desc&order[chapter]=desc&offset=$index").parsed<MangaResponse>().data?.reversed()?.forEach {
                    if (it.attributes!!.translatedLanguage == "en") {
                        val chapter = (it.attributes.chapter?:return@forEach).toString()
                        val title = it.attributes.title?:return@forEach
                        val id = it.id?:return@forEach
                        arr[chapter] = MangaChapter(chapter, title, id)
                    }
                }
            }
            arr.toSortedMap()
        } catch (e: Exception) {
            logError(e)
        }
        return arr
    }

    override suspend fun getChapter(chapter: MangaChapter): MangaChapter {
        try {
            httpClient.get("$host/at-home/server/${chapter.link}").parsed<ChapterResponse>().chapter?.apply {
                val images = arrayListOf<String>()
                for (page in data?:return@apply) {
                    images.add("https://uploads.mangadex.org/data/${hash?:return@apply}/${page}")
                }
                chapter.images = images
            }
        } catch (e: Exception) {
            logError(e)
        }
        return chapter
    }

    override suspend fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source: Source? = loadData("mangadex_${media.id}")
        if (source == null) {
            setTextListener("Searching : ${media.getMangaName()}")
            val search = search(media.getMangaName())
            if (search.isNotEmpty()) {
                logger("MangaDex : ${search[0]}")
                source = search[0]
                saveSource(source, media.id, false)
            }
        } else {
            setTextListener("Selected : ${source.name}")
        }
        if (source != null) {
            val s = getLinkChapters(source.link)
            setTextListener("Loaded : ${source.name}")
            return s
        }
        return mutableMapOf()
    }

    override suspend fun search(string: String): ArrayList<Source> {
        val arr = arrayListOf<Source>()
        try {
            val json = httpClient.get("$host/manga?limit=$limit&title=$string&order[relevance]=desc&includes[]=cover_art").parsed<SearchResponse>()
            json.data?.forEach {
                val id = it.id?:return@forEach
                val title = it.attributes?.title?.en?:return@forEach
                val coverName = it.relationships?.find { i-> i.type=="cover_art" }?.attributes?.fileName
                val coverURL = if(coverName!=null) "https://uploads.mangadex.org/covers/$id/$coverName.256.jpg" else ""
                arr.add(Source(id, title, coverURL))
            }
        } catch (e: Exception) {
            logError(e)
        }
        return arr
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("mangadex_$id", source)
    }

    data class SearchResponse (
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

    private data class MangaResponse (
        val result: String? = null,
        val data: List<Datum>? = null,
        val total: Long? = null
    )
    {
        data class Datum (
            val id: String? = null,
            val attributes: Attributes? = null
        )

        data class Attributes (
            val volume: Any? = null,
            val chapter: Any? = null,
            val title: String? = null,
            val translatedLanguage: String? = null
        )
    }

    private data class ChapterResponse (
        val result: String? = null,
        val baseURL: String? = null,
        val chapter: Chapter? = null
    ) {
        data class Chapter (
            val hash : String? = null,
            val data: List<String>? = null,
            val dataSaver: List<String>? = null
        )
    }
}