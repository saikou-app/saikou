package ani.saikou.anime.source.parsers

import android.util.Base64
import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.anime.source.extractors.*
import ani.saikou.anime.source.Extractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class Monoschinos(private val dub:Boolean=false, override val name: String = "Monoschinos"): AnimeParser() {


    private val host = "https://monoschinos2.com"

    private fun directLinkify(name: String,url: String): Episode.StreamLinks? {
        val domain = Regex("""(?<=^http[s]?://).+?(?=/)""").find(url)!!.value
        val extractor : Extractor?=when {
            "fembed" in domain -> FPlayer(getSize = true)
            "ok" in domain -> OK()
            else -> null
        }
        val a = extractor?.getStreamLinks(name,url)
        if (a!=null && a.quality.isNotEmpty()) return a
        return null
    }

    override fun getStream(episode: Episode,server: String): Episode {
        episode.streamLinks = runBlocking {
            val linkForVideos = mutableMapOf<String,Episode.StreamLinks?>()
            try{
                withContext(Dispatchers.Default) {
                Jsoup.connect(episode.link!!).ignoreHttpErrors(true).get().select("ul.dropcaps li").forEach {
                    launch {
                        val name = it.select("a").text()
                        val urlBase64 = it.select("a").attr("data-player")
                        val url1 = Base64.decode(urlBase64, Base64.DEFAULT)
                        val url = String(url1).replace("https://monoschinos2.com/reproductor?url=", "")
                        val directLinks = directLinkify(name, url)
                        logger("$server")
                        if(name==server){
                            if(directLinks != null){linkForVideos[name] = directLinks}
                        }

                    }
                }
            }}catch (e:Exception){
            toastString(e.toString())
            }
            return@runBlocking (linkForVideos)
        }
        return episode
    }




    override fun getStreams(episode: Episode): Episode {
//        try {
        episode.streamLinks = runBlocking {
            val linkForVideos = mutableMapOf<String,Episode.StreamLinks?>()
            withContext(Dispatchers.Default) {
                Jsoup.connect(episode.link!!).ignoreHttpErrors(true).get().select("ul.dropcaps li").forEach {
                    launch {
                            val server = it.select("a").text()
                            val urlBase64 = it.select("a").attr("data-player")
                            val url1 = Base64.decode(urlBase64, Base64.DEFAULT)
                            val url = String(url1).replace("https://monoschinos2.com/reproductor?url=", "")
                        val directLinks = directLinkify(server, url)
                        logger("server")
                        if(directLinks != null){linkForVideos[directLinks.server] = directLinks}
                    }
                }
            }
            return@runBlocking (linkForVideos)
        }
        return episode
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        var slug:Source? = loadData("monoschinos_${media.id}")
        if (slug==null) {
            val it = media.nameMAL?:media.nameRomaji
            setTextListener("Buscando $it")
            logger("Monoschinos : Searching for $it")
            val search = search(media.nameRomaji)
            if (search.isNotEmpty()) {
                slug = search[0]
                saveSource(slug,media.id,false)
            }
        }
        else{
            setTextListener("Buscando ${slug.name}")
        }
        if (slug!=null) return getSlugEpisodes(slug.link)
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        var url = URLEncoder.encode(string, "utf-8")
        if(string.startsWith("$!")){
            val a = string.replace("$!","").split(" | ")
            url = URLEncoder.encode(a[0], "utf-8")+a[1]
        }
        val responseArray = arrayListOf<Source>()
        val requests = Jsoup.connect("$host/buscar?q=$url").get()
        requests.select("div.heromain div.row div.col-md-4").forEach() {
            val link = it.select("a").attr("href")
            val title = it.select("a div.series div.seriesdetails h5").text()
            val cover = it.select("a div.series div.seriesimg img").attr("src")
            logger("log  $link")
            responseArray.add(Source(link,title,cover))
        }
        return responseArray
    }


    override fun getSlugEpisodes(slug:String): MutableMap<String, Episode>{
        val responseArray = mutableMapOf<String,Episode>()
        try{
        val pageBody = Jsoup.connect(slug).get()
            pageBody.select("div.heroarea2 div.heromain2 div.allanimes div.row.jpage.row-cols-md-6 div.col-item").forEach { it ->
                val epNum = it.attr("data-episode")
                 logger("Episode-$epNum")
                val url = it.select("a").attr("href")
                val thumb1 = it.select("a div.animedtlsmain div.animeimgdiv img.animeimghv").attr("data-src")
                logger("url2-$url")
                logger("thumb-$thumb1")
                responseArray[epNum] = Episode(number = epNum,link = url, thumb = thumb1)
            }

        logger("Response Episodes : $responseArray")
            logger("slug-$slug")
        }catch (e:Exception){ toastString(e.toString()) }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        saveData("monoschinos_$id", source)
    }
}