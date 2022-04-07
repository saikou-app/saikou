package ani.saikou.anime.source.parsers

import android.util.Base64
import android.util.Log
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

class Animefenix(private val dub:Boolean=false, override val name: String = "Animefenix"): AnimeParser() {


    private val host = "https://www.animefenix.com"

    private fun directLinkify(name: String,url: String): Episode.StreamLinks? {
         return when (name) {
            "fembed", "Fembed" -> FPlayer(getSize = true).getStreamLinks("fembed","https://www.fembed.com/v/$url")
            "ru","RU" -> OK().getStreamLinks("OKru","https://ok.ru/videoembed/$url")
            "Amazon","AMAZON","amazon" -> amazonExtractor("Amazon","https://www.animefenix.com/stream/amz.php?v=$url")
            "AmazonEs","AmazonES","amazones" -> amazonExtractor("AmazonES","https://www.animefenix.com/stream/amz.php?v=$url&ext=es")
            else -> null
        }
    }

    private fun amazonExtractor(name: String,url: String): Episode.StreamLinks {
        val jsoup = Jsoup.connect(url).get()
        val videoUrl = jsoup.select("body script").toString()
            .substringAfter("[{\"file\":\"")
            .substringBefore("\",").replace("\\", "")
        val tempQuality = mutableListOf<Episode.Quality>()
        tempQuality.add(Episode.Quality(videoUrl,name,null))
        Log.i("bruh", videoUrl)
        return  Episode.StreamLinks(
            name,
            tempQuality,
            null
        )
    }

    override fun getStream(episode: Episode,server: String): Episode {
        episode.streamLinks = runBlocking {
            val linkForVideos = mutableMapOf<String,Episode.StreamLinks?>()
            try{
                withContext(Dispatchers.Default) {
                    val document = Jsoup.connect(episode.link!!).ignoreHttpErrors(true).get()
                    document.select("ul.is-borderless.episode-page__servers-list li").forEach { it ->
                        launch {
                            val serverName = it.select("a").attr("title")
                            val serverId = it.select("a").attr("href").replace("#vid", "").toInt()
                            val serverCode =
                                document.select("div.player-container script").toString()
                                    .substringAfter("tabsArray['$serverId'] =")
                                    .substringBefore("&amp;thumbnail")
                                    .substringAfter("code=")
                                    .substringBefore("&amp")



                            val directLinks = directLinkify(serverName, serverCode)
                            if(serverName==server){
                                if(directLinks != null){linkForVideos[directLinks.server] = directLinks}
                        }

                    }}

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
            try{
                withContext(Dispatchers.Default) {
                    val document = Jsoup.connect(episode.link!!).ignoreHttpErrors(true).get()
                    document.select("ul.is-borderless.episode-page__servers-list li").forEach { it ->
                        launch {
                            val serverName = it.select("a").attr("title")
                            val serverId = it.select("a").attr("href").replace("#vid", "").toInt()
                            val serverCode =
                                document.select("div.player-container script").toString()
                                    .substringAfter("tabsArray['$serverId'] =")
                                    .substringBefore("&amp;thumbnail")
                                    .substringAfter("code=")
                                    .substringBefore("&amp")
                            val directLinks = directLinkify(serverName, serverCode)
                            if(directLinks != null){linkForVideos[directLinks.server] = directLinks}

                        }}

                }}catch (e:Exception){
                toastString(e.toString())
            }
            return@runBlocking (linkForVideos)
        }
        return episode
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        var slug:Source? = loadData("animefenix_${media.id}")
        if (slug==null) {
            val it = media.nameMAL?:media.nameRomaji
            setTextListener("Buscando $it")
            logger("Animefenix : Searching for $it")
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
        logger("log  $url")
        val responseArray = arrayListOf<Source>()
        val requests = Jsoup.connect("$host/animes?q=$url").get()
        requests.select("div.container div.container div.list-series article.serie-card").forEach() {
            val link = it.select("figure.image a").attr("href")
            val title = it.select("div.title h3 a").text()
            val cover = it.select("figure.image a img").attr("src")
            logger("log  $link")
            responseArray.add(Source(link,title,cover))
        }
        return responseArray
    }


    override fun getSlugEpisodes(slug:String): MutableMap<String, Episode>{
        val responseArray = mutableMapOf<String,Episode>()
        try{
            val pageBody = Jsoup.connect(slug).get()
            pageBody.select("ul.anime-page__episode-list.is-size-6 li").forEach { it ->
                val epNum = it.select("a span").text().replace("Episodio", "")
                logger("Episode-$epNum")
                val url = it.select("a").attr("href")
                logger("url2-$url")
                responseArray[epNum] = Episode(number = epNum,link = url)
            }

            logger("Response Episodes : $responseArray")
            logger("slug-$slug")
        }catch (e:Exception){ toastString(e.toString()) }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        saveData("animefenix_$id", source)
    }
}