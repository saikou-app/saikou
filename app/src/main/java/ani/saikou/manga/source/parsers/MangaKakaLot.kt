package ani.saikou.manga.source.parsers

import ani.saikou.*
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import org.jsoup.Jsoup

class MangaKakaLot(override val name: String="MangaKakaLot") :MangaParser() {
    private val host = "https://mangakakalot.com"

    override fun getLinkChapters(link:String):MutableMap<String,MangaChapter>{
        val arr = mutableMapOf<String, MangaChapter>()
        try {
            Jsoup.connect(link).get().select(if (link.contains("readmanganato.com")) ".row-content-chapter > .a-h" else ".chapter-list > .row > span").reversed().forEach {
                val chap = Regex("(Chapter ([A-Za-z0-9.]+))(: ?(.+))?").find(it.select("a").text())?.destructured
                if(chap!=null) {
                    arr[chap.component2()] = MangaChapter(
                        number = chap.component2(),
                        link = it.select("a").attr("href"),
                        title = chap.component4()
                    )
                }
            }
        }catch (e:Exception){
            toastString("$e")
        }
        return arr
    }

    override fun getChapter(chapter: MangaChapter): MangaChapter {
        chapter.images = arrayListOf()
        try {
            val arr = Jsoup.connect(chapter.link!!).get().select(".container-chapter-reader > img")
            arr?.forEach {
                chapter.images!!.add(it.attr("src"))
            }
            chapter.headers = mutableMapOf("referer" to host)
        }catch (e:Exception){
            toastString(e.toString())
        }
        return chapter
    }

    override fun getChapters(media: Media): MutableMap<String, MangaChapter> {
        var source:Source? = loadData("mangakakalot_${media.id}")
        if (source==null) {
            setTextListener("Searching : ${media.getMangaName()}")
            val search = search(media.getMangaName())
            if (search.isNotEmpty()) {
                logger("MangaKakaLot : ${search[0]}")
                source = search[0]
                setTextListener("Found : ${source.name}")
                saveSource(source,media.id)
            }
        }
        else{
            setTextListener("Selected : ${source.name}")
        }
        if (source!=null) return getLinkChapters(source.link)
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        val response = arrayListOf<Source>()
        try{
            Jsoup.connect("$host/search/story/${string.replace(" ", "_").replace(Regex("\\W"),"")}").get().select(".story_item").forEach {
                if (it.select(".story_name > a").text()!=""){
                    response.add(Source(
                        link = it.select("a").attr("href"),
                        name = it.select(".story_name > a").text(),
                        cover = it.select("img").attr("src"),
                        headers = mutableMapOf("referer" to host)
                    ))
                }
            }} catch(e:Exception){ toastString(e.toString()) }
        return response
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("mangakakalot_$id", source)
    }
}