package ani.saikou.parsers

import ani.saikou.FileUrl
import ani.saikou.media.Media
import com.bumptech.glide.load.Transformation
import java.io.File
import java.io.Serializable

abstract class MangaParser : BaseParser() {

    /**
     * Takes ShowResponse.link and ShowResponse.extra (if any) as arguments & gives a list of total chapters present on the site.
     * **/
    abstract suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter>

    /**
     * Takes MangaChapter.link as an argument & returns a list of MangaImages with their Url (with headers & transformations, if needed)
     * **/
    abstract suspend fun loadImages(chapterLink: String): List<MangaImage>

    override suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        var response = loadSavedShowResponse(mediaObj.id)
        if (response != null) {
            saveShowResponse(mediaObj.id, response, true)
        } else {
            setUserText("Searching : ${mediaObj.mangaName()}")
            response = search(mediaObj.mangaName()).let { if (it.isNotEmpty()) it[0] else null }

            if (response == null) {
                setUserText("Searching : ${mediaObj.nameRomaji}")
                response = search(mediaObj.nameRomaji).let { if (it.isNotEmpty()) it[0] else null }
            }
            saveShowResponse(mediaObj.id, response)
        }
        return response
    }

    open fun getTransformation():Transformation<File>? = null
}

data class MangaChapter(
    /**
     * Number of the Chapter in "String",
     *
     * useful in cases where chapter is not a number
     * **/
    val number: String,

    /**
     * Link that links to the chapter page containing videos
     * **/
    val link: String,

    //Self-Descriptive
    val title: String? = null,
    val description: String? = null,
)

data class MangaImage(
    /**
     * The direct url to the Image of a page in a chapter
     *
     * Supports jpeg,jpg,png & gif(non animated) afaik
     * **/
    val url: FileUrl,

    val useTransformation: Boolean = false
) : Serializable{
    constructor(url: String,useTransformation: Boolean=false)
            : this(FileUrl(url),useTransformation)
}
