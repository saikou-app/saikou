package ani.saikou.manga.source.parsers

import ani.saikou.*
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.logError
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.Requests

class MangaSee(override val name: String = "MangaSee") : MangaParser() {
    private val host = "https://mangasee123.com"

    private data class MangaResponse(
        @JsonProperty("Chapter") val chapter: String,
        @JsonProperty("ChapterName") val chapterName: String?
    )

    override suspend fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        val responseArray = mutableMapOf<String, MangaChapter>()
        try {
            val json = httpClient.get("$host/manga/$link").document.select("script").lastOrNull()!!.toString()
                .findBetween("vm.Chapters = ", ";")!!

            Requests.mapper.readValue<List<MangaResponse>>(json).forEach {
                val chap = it.chapter
                val num = chapChop(chap, 3)
                responseArray[num] = MangaChapter(
                    num,
                    it.chapterName,
                    host + "/read-online/" + link + "-chapter-" + chapChop(chap, 1) + chapChop(chap, 2) + chapChop(
                        chap,
                        0
                    ) + ".html"
                )
            }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    private fun chapChop(id: String, type: Int): String = when (type) {
        0    -> if (id.startsWith("1")) "" else ("-index-${id[0]}")
        1    -> (id.substring(1, 5).replace("[^0-9]".toRegex(), ""))
        2    -> if (id.endsWith("0")) "" else (".${id[id.length - 1]}")
        3    -> "${id.drop(1).dropLast(1).toInt()}${chapChop(id, 2)}"
        else -> ""
    }

    private data class ChapterResponse(
        @JsonProperty("Chapter") val chapter: String,
        @JsonProperty("Page") val page:String
    )

    override suspend fun getChapter(chapter: MangaChapter): MangaChapter {
        chapter.images = arrayListOf()
        try {
            val a =
                httpClient.get(chapter.link ?: return chapter).document.select("script")
                    .lastOrNull()
            val str = (a ?: return chapter).toString()
            val server = (str.findBetween("vm.CurPathName = ", ";") ?: return chapter).trim('"')
            val slug = (str.findBetween("vm.IndexName = ", ";") ?: return chapter).trim('"')
            val json = Requests.mapper.readValue<ChapterResponse>(
                str.findBetween("vm.CurChapter = ", ";") ?: return chapter
            )
            val id = json.chapter
            val chap = chapChop(id, 1) + chapChop(id, 2) + chapChop(id, 0)
            val pages = json.page.toInt()
            for (i in 1..pages)
                chapter.images!!.add("https://$server/manga/$slug/$chap-${"000$i".takeLast(3)}.png")
        } catch (e: Exception) {
            logError(e)
        }
        return chapter
    }

    override suspend fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source: Source? = loadData("mangasee_${media.id}")
        if (source == null) {
            setTextListener("Searching : ${media.getMainName()}")
            val search = search(media.getMainName())
            if (search.isNotEmpty()) {
                logger("MangaSee : ${search[0]}")
                source = search[0]
                setTextListener("Found : ${source.name}")
                saveSource(source, media.id)
            }
        } else {
            setTextListener("Selected : ${source.name}")
        }
        if (source != null) return getLinkChapters(source.link)
        return mutableMapOf()
    }

    private data class SearchResponse(
        val s: String,
        val i: String
    )

    override suspend fun search(string: String): ArrayList<Source> {
        val response = arrayListOf<Source>()
        try {
            response.addAll(
                getSearchData()
            )
            response.sortByTitle(string)
        } catch (e: Exception) {
            logError(e)
        }
        return response
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("mangasee_$id", source)
    }

    companion object {
        private var response: List<Source>? = null
        suspend fun getSearchData(): List<Source> {
            response =
                if (response != null) response ?: listOf()
                else {
                    val json = httpClient.get("https://mangasee123.com/search/").document.select("script").last().toString()
                        .findBetween("vm.Directory = ", "\n")!!.replace(";", "")
                    Requests.mapper.readValue<List<SearchResponse>>(json).map {
                        Source(
                            name = it.s,
                            link = it.i,
                            cover = "https://cover.nep.li/cover/${it.i}.jpg"
                        )
                    }
                }
            return response ?: listOf()
        }
    }
}