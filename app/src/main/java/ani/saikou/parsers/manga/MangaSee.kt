package ani.saikou.parsers.manga

import ani.saikou.client
import ani.saikou.findBetween
import ani.saikou.mapper
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse
import ani.saikou.sortByTitle
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue

class MangaSee : MangaParser() {

    override val name = "MangaSee"
    override val saveName = "manga_see"
    override val hostUrl = host

    override suspend fun loadChapters(mangaLink: String): List<MangaChapter> {

        val json = client.get("$hostUrl/manga/$mangaLink").document.select("script")
            .lastOrNull()?.toString()?.findBetween("vm.Chapters = ", ";")?: return emptyList()

        return mapper.readValue<List<MangaResponse>>(json).reversed().map {
            val chap = it.chapter
            val num = chapChop(chap, 3)
            val link = hostUrl + "/read-online/$mangaLink-chapter-" + chapChop(chap, 1) + chapChop(chap, 2) + chapChop(chap, 0) + ".html"
            MangaChapter(num, link, it.chapterName)
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        val res = client.get(chapterLink).document.select("script").lastOrNull()
        val str = res?.toString() ?: return emptyList()
        val server = str.findBetween("vm.CurPathName = ", ";")?.trim('"') ?: return emptyList()
        val slug = str.findBetween("vm.IndexName = ", ";")?.trim('"') ?: return emptyList()
        val json = mapper.readValue<ChapterResponse>(
            str.findBetween("vm.CurChapter = ", ";") ?: return emptyList()
        )
        val id = json.chapter
        val chap = chapChop(id, 1) + chapChop(id, 2) + chapChop(id, 0)
        val pages = json.page.toInt()

        val a = (1..pages)

        return a.map {
            val link = "https://$server/manga/$slug/$chap-${"000$it".takeLast(3)}.png"
            MangaImage(link)
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val list = getSearchData().toMutableList()
        list.sortByTitle(query)
        return list
    }

    companion object {
        private const val host = "https://mangasee123.com"
        private var response: List<ShowResponse>? = null
        suspend fun getSearchData(): List<ShowResponse> {
            response = if (response != null) response ?: emptyList()
            else {
                val json = client.get("$host/search/").document.select("script").last().toString()
                    .findBetween("vm.Directory = ", "\n")!!.replace(";", "")
                mapper.readValue<List<SearchResponse>>(json).map {
                    ShowResponse(
                        it.s,
                        it.i,
                        "https://cover.nep.li/cover/${it.i}.jpg"
                    )
                }
            }
            return response ?: emptyList()
        }
    }

    private fun chapChop(id: String, type: Int): String = when (type) {
        0    -> if (id.startsWith("1")) "" else ("-index-${id[0]}")
        1    -> (id.substring(1, 5).replace("[^0-9]".toRegex(), ""))
        2    -> if (id.endsWith("0")) "" else (".${id[id.length - 1]}")
        3    -> "${id.drop(1).dropLast(1).toInt()}${chapChop(id, 2)}"
        else -> ""
    }

    private data class MangaResponse(
        @JsonProperty("Chapter") val chapter: String,
        @JsonProperty("ChapterName") val chapterName: String?
    )

    private data class ChapterResponse(
        @JsonProperty("Chapter") val chapter: String,
        @JsonProperty("Page") val page: String
    )

    private data class SearchResponse(
        val s: String,
        val i: String
    )
}