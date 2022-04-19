package ani.saikou.core.source.manga.parsers

import ani.saikou.core.model.manga.MangaChapter
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source
import ani.saikou.core.service.LOG
import ani.saikou.core.service.STORE
import ani.saikou.core.source.manga.MangaParser
import ani.saikou.core.utils.extension.findBetween
import ani.saikou.core.utils.extension.sortByTitle
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.jsoup.Jsoup

class MangaSee(override val name: String = "MangaSee") : MangaParser() {
    private val host = "https://mangasee123.com"

    override fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        val responseArray = mutableMapOf<String, MangaChapter>()
        try {
            val a = Jsoup.connect("$host/manga/$link").maxBodySize(0).get().select("script")
                .lastOrNull()
            val json = (a ?: return responseArray).toString().findBetween("vm.Chapters = ", ";")
                ?: return responseArray

            Json.decodeFromString<JsonArray>(json).reversed().forEach {
                val chap = it.jsonObject["Chapter"].toString().trim('"')
                val name =
                    if (it.jsonObject["ChapterName"] != JsonNull) it.jsonObject["ChapterName"].toString()
                        .trim('"') else null
                val num = chapChop(chap, 3)
                responseArray[num] = MangaChapter(
                    num,
                    name,
                    host + "/read-online/" + link + "-chapter-" + chapChop(chap, 1) + chapChop(
                        chap,
                        2
                    ) + chapChop(chap, 0) + ".html"
                )
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return responseArray
    }

    private fun chapChop(id: String, type: Int): String = when (type) {
        0 -> if (id.startsWith("1")) "" else ("-index-${id[0]}")
        1 -> (id.substring(1, 5).replace("[^0-9]".toRegex(), ""))
        2 -> if (id.endsWith("0")) "" else (".${id[id.length - 1]}")
        3 -> "${id.drop(1).dropLast(1).toInt()}${chapChop(id, 2)}"
        else -> ""
    }

    override fun getChapter(chapter: MangaChapter): MangaChapter {
        chapter.images = arrayListOf()
        try {
            val a =
                Jsoup.connect(chapter.link ?: return chapter).maxBodySize(0).get().select("script")
                    .lastOrNull()
            val str = (a ?: return chapter).toString()
            val server = (str.findBetween("vm.CurPathName = ", ";") ?: return chapter).trim('"')
            val slug = (str.findBetween("vm.IndexName = ", ";") ?: return chapter).trim('"')
            val chapJson = Json.decodeFromString<JsonObject>(
                str.findBetween("vm.CurChapter = ", ";") ?: return chapter
            )
            val id = chapJson["Chapter"].toString().trim('"')
            val chap = chapChop(id, 1) + chapChop(id, 2) + chapChop(id, 0)
            val pages = chapJson["Page"].toString().trim('"').toIntOrNull() ?: return chapter
            for (i in 1..pages)
                chapter.images!!.add("https://$server/manga/$slug/$chap-${"000$i".takeLast(3)}.png")
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return chapter
    }

    override fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source: Source? = STORE.loadData("mangasee_${media.id}")
        if (source == null) {
            setTextListener("Searching : ${media.mangaName}")
            val search = search(media.mangaName)
            if (search.isNotEmpty()) {
                LOG.log("MangaSee : ${search[0]}")
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

    override fun search(string: String): List<Source> {
        val response = mutableListOf<Source>()
        try {
            val a = Jsoup.connect("$host/search/")
                .maxBodySize(0)
                .get()
                .select("script")
                .lastOrNull()

            val json = (a ?: return response).toString().findBetween("vm.Directory = ", "\n")
                ?.replace(";", "") ?: return response
            Json.decodeFromString<JsonArray>(json).forEach {
                response.add(
                    Source(
                        name = it.jsonObject["s"].toString().trim('"'),
                        link = it.jsonObject["i"].toString().trim('"'),
                        cover = "https://cover.nep.li/cover/" +
                                it.jsonObject["i"].toString().trim('"') +
                                ".jpg"
                    )
                )
            }
            response.sortByTitle(string)
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return response
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        STORE.saveData("mangasee_$id", source)
    }
}