package ani.saikou.parsers

import ani.saikou.FileUrl
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.saveData
import java.io.Serializable
import java.net.URLEncoder

abstract class BaseParser {

    /**
     * Name that will be shown in Source Selection
     * **/
    open val name: String = ""

    /**
     * Name used to save the ShowResponse selected by user or by autoSearch
     * **/
    open val saveName: String = ""

    /**
     * The main URL of the Site
     * **/
    open val hostUrl: String = ""

    /**
     * override as `true` if the site **only** has NSFW media
     * **/
    open val isNSFW = false

    /**
     * mostly redundant for official app, But override if you want to add different languages
     * **/
    open val language = "English"

    /**
     *  Search for Anime/Manga/Novel, returns a List of Responses
     *
     *  use `encode(query)` to encode the query for making requests
     * **/
    abstract suspend fun search(query: String): List<ShowResponse>

    /**
     * The function app uses to auto find the anime/manga using Media data provided by anilist
     *
     * Isn't necessary to override, but recommended, if you want to improve auto search results
     * **/
    open suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        var response = loadSavedShowResponse(mediaObj.id)
        if (response != null) {
            setUserText("Selected : ${response.name}")
        } else {
            setUserText("Searching : ${mediaObj.mainName}")
            response = search(mediaObj.mainName).let { if (it.isNotEmpty()) it[0] else null }

            if (response == null) {
                setUserText("Searching : ${mediaObj.nameRomaji}")
                response = search(mediaObj.nameRomaji).let { if (it.isNotEmpty()) it[0] else null }
            }
        }
        saveShowResponse(mediaObj.id, response)
        return response
    }

    /**
     * Used to get an existing Search Response which was selected by the user.
     * **/
    open suspend fun loadSavedShowResponse(mediaId: Int): ShowResponse? {
        checkIfVariablesAreEmpty()
        return loadData("${saveName}_$mediaId")
    }

    /**
     * Used to save Shows Response using `saveName`.
     * **/
    open fun saveShowResponse(mediaId: Int, response: ShowResponse?, selected: Boolean = false) {
        if (response != null) {
            checkIfVariablesAreEmpty()
            setUserText("${if (selected) "Selected" else "Found"} : ${response.name}")
            saveData("${saveName}_$mediaId", response)
        }
    }

    fun checkIfVariablesAreEmpty() {
        if (hostUrl.isEmpty()) throw UninitializedPropertyAccessException("Please provide a `hostUrl` for the Parser")
        if (name.isEmpty()) throw UninitializedPropertyAccessException("Please provide a `name` for the Parser")
        if (saveName.isEmpty()) throw UninitializedPropertyAccessException("Please provide a `saveName` for the Parser")
    }

    open var showUserText = ""
    open var showUserTextListener: ((String) -> Unit)? = null

    /**
     * Used to show messages & errors to the User, a useful way to convey what's currently happening or what was done.
     * **/
    fun setUserText(string: String) {
        showUserText = string
        showUserTextListener?.invoke(showUserText)
    }

    fun encode(input: String): String = URLEncoder.encode(input, "utf-8").replace("+", "%20")
}


/**
 * A single show which contains some episodes/chapters which is sent by the site using their search function.
 *
 * You might wanna include `otherNames` & `total` too, to further improve user experience.
 * **/
data class ShowResponse(
    val name: String,
    val link: String,
    val coverUrl: FileUrl,

    //would be Useful for custom search, ig
    val otherNames: List<String> = listOf(),
    //Total number of Episodes/Chapters in the show.
    val total: Int? = null,
) : Serializable {
    constructor(name: String, link: String, coverUrl: String, otherNames: List<String> = listOf(), total: Int? = null)
            : this(name, link, FileUrl(coverUrl), otherNames, total)

    constructor(name: String, link: String, coverUrl: String, otherNames: List<String> = listOf())
            : this(name, link, FileUrl(coverUrl), otherNames)

    constructor(name: String, link: String, coverUrl: String)
            : this(name, link, FileUrl(coverUrl))
}


