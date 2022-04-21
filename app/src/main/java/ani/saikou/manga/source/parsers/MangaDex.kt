package ani.saikou.manga.source.parsers

import ani.saikou.loadData
import ani.saikou.logger
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.saveData
import ani.saikou.toastString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import kotlin.math.roundToInt

class MangaDex(override val name: String="mangadex.org") :MangaParser() {
    private val host = "https://api.mangadex.org"
    private val limit = 100
    override fun getLinkChapters(link: String): MutableMap<String, MangaChapter> {
        setTextListener("Getting Chapters...")
        val arr = mutableMapOf<String, MangaChapter>()
        try{
        val totalChapters = Regex("(?<=\"total\":)\\d+").find(
            Jsoup.connect("$host/manga/$link/feed?limit=0").ignoreContentType(true).get().text()
        )!!.value.toInt()
        setTextListener("Parsing Chapters...")
        (0..totalChapters step 200).reversed().forEach{ index ->
            val jsonResponse = Jsoup.connect("$host/manga/$link/feed?limit=200&order[volume]=desc&order[chapter]=desc&offset=$index").ignoreContentType(true).get().text()
            Json.decodeFromString<JsonObject>(jsonResponse)["data"]!!.jsonArray.reversed().forEach{
                if(it.jsonObject["attributes"]!!.jsonObject["translatedLanguage"].toString() == "\"en\""){
                    val chapter = it.jsonObject["attributes"]!!.jsonObject["chapter"].toString().trim('"')
                    val title = it.jsonObject["attributes"]!!.jsonObject["title"].toString().trim('"')
                    val id = it.jsonObject["id"].toString().trim('"')
                    arr[chapter] = MangaChapter(chapter,title,id)
                }
            }
            var a = (index.toFloat() / totalChapters * 100)
            try { a = a.roundToInt().toFloat() }catch (e:Exception){}
            setTextListener("Chapter Parsing : ${100-a}%...")
        }}catch (e:Exception){
            toastString(e.toString())
        }
        return arr
    }

    override fun getChapter(chapter: MangaChapter): MangaChapter {
        try{
        val json = Json.decodeFromString<JsonObject>(Jsoup.connect("$host/at-home/server/${chapter.link}").ignoreContentType(true).get().text())
        val images = arrayListOf<String>()
        val hash = json.jsonObject["chapter"]!!.jsonObject["hash"].toString().trim('"')
        for(page in json.jsonObject["chapter"]!!.jsonObject["data"]!!.jsonArray){
            images.add("https://uploads.mangadex.org/data/${hash}/${page.toString().trim('"')}")
        }
        chapter.images = images
        }catch (e:Exception){
            toastString(e.toString())
        }
        return chapter
    }

    override fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source:Source? = loadData("mangadex_${media.id}")
        if (source==null) {
            setTextListener("Searching : ${media.getMangaName()}")
            val search = search(media.getMangaName())
            if (search.isNotEmpty()) {
                logger("MangaDex : ${search[0]}")
                source = search[0]
                saveSource(source,media.id,false)
            }
        }
        else{
            setTextListener("Selected : ${source.name}")
        }
        if (source!=null) {
            val s = getLinkChapters(source.id)
            setTextListener("Loaded : ${source.name}")
            return s
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        val arr = arrayListOf<Source>()
        try{
        val jsonResponse = Jsoup.connect("$host/manga?limit=$limit&title=$string&order[relevance]=desc&includes[]=cover_art").ignoreContentType(true).get().text()
        Json.decodeFromString<JsonObject>(jsonResponse)["data"]!!.jsonArray.forEach{
            val id = it.jsonObject["id"].toString().trim('"') // id
            val title = it.jsonObject["attributes"]!!.jsonObject["title"]!!.jsonObject["en"].toString().trim('"') // en title
            val coverName = Regex("(?<=\"fileName\":\").+?(?=\")").find(it.jsonObject["relationships"]!!.jsonArray.toString())?.value // cover image
            val coverURL = "https://uploads.mangadex.org/covers/$id/$coverName.256.jpg"
            arr.add(Source(id,title,coverURL))
        }}catch (e:Exception){
            toastString(e.toString())
        }
        return arr
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("mangadex_$id", source)
    }
}