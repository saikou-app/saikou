package ani.saikou.parsers

import ani.saikou.others.asyncEach
import ani.saikou.others.logError


/**
 * An abstract class for creating a new Source
 * Most of the functions & variables that need to be overridden are abstract
 * **/
abstract class AnimeParser : BaseParser() {

    data class Episode(
        val number: String,
        val link : String,

        val title : String?=null,
        val thumbnail : String?=null,
        val description : String?=null,
        val isFiller : Boolean = false,
    )

    /**
     * Usually takes a SearchResponse.url as argument & gives a list of total episode present on the server.
     * **/
    abstract suspend fun loadEpisodes(url: String) : List<Episode>

    /**
     * Most of the time takes Episode.link as parameter, but you can use anything else if needed
     * This returns a Map of "Video Server's Name" & "Link/Data" of all the Extractor VideoServers, which can be further used by loadVideoServers() & loadSingleVideoServer()
     * **/
    abstract suspend fun loadVideoServerLinks (url:String) : MutableMap<String,String>


    /**
     * Takes an url or any other data as an argument & returns VideoExtractor
     * This function will receive url of the embed & name of the Video Server used on the site.
     * **/
    abstract suspend fun getVideoExtractor(url:String, serverName:String) : VideoExtractor?

    //Example :
    //     val domain = URI(url).host
    //     val extractor : VideoExtractor? = when {
    //         "gogo" in domain -> GogoCDN()
    //         "sb" in domain ->  StreamSB()
    //         "fplayer" in domain -> FPlayer()
    //         else -> null
    //     }
    //     return null


    /**
     * This Function used when there "isn't" a default Server set by the user, or when user wants to switch the Server
     * Doesn't need to be overridden, if the parser is following the norm.
     * **/
    open suspend fun loadVideoServers(url:String, callback: (VideoExtractor) -> Unit) {
        loadVideoServerLinks(url).asyncEach{
            try {
                getVideoExtractor(it.value,it.key)?.apply{
                    this.name = it.key
                    extractFromUrl(it.value)
                    callback.invoke(this)
                }
            }
            catch (e:Exception){
                logError(e)
            }
        }
    }

    /**
     * This Function used when there "is" a default Server set by the user, only loads a Single Server for faster response.
     * Doesn't need to be overridden, if the parser is following the norm.
     * **/
    open suspend fun loadSingleVideoServer(serverName: String, url:String) : VideoExtractor? {
        try {
            loadVideoServerLinks(url).apply{
                if(containsKey(serverName))
                    return getVideoExtractor(url,serverName)?.apply {
                        this.name = serverName
                        extractFromUrl(url)
                    }
            }
        } catch (e:Exception){
            logError(e)
        }
        return null
    }

}