package ani.saikou.parsers

import ani.saikou.media.Media

abstract class BaseParser {
    abstract val name: String
    abstract val saveName : String
    abstract val hostUrl: String
    abstract val author: String

    open val nsfw = false
    open val language = "en"

    data class SearchResponse(
        val name: String,
        val url: String,
        val coverUrl: String?,

        val otherNames: List<String> = listOf(),
        val totalEpisodes: Int? = null,
    )

    /**
     *  Search for Anime/Manga/Novel, returns a List of Responses, having name, url & other metadata
     **/
    abstract suspend fun search(mediaObj: Media): List<SearchResponse>

    open var displayText = ""
    open var displayTextListener: ((String) -> Unit)? = null

    /**
     * Used to send messages & errors to the User, a useful way to convey what's happening on currently being done & what was done.
     */
    fun setText(string: String) {
        displayText = string
        displayTextListener?.invoke(displayText)
    }

}


