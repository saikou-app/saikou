package ani.saikou.parsers

import ani.saikou.FileUrl
import java.io.Serializable

/**
 * Used to extract videos from a specific video host,
 *
 * A new instance is created for every embeds/iframes of that Episode
 * **/
abstract class VideoExtractor : Serializable {
    abstract val server: VideoServer
    lateinit var videos: List<Video>
    lateinit var subtitles: List<Subtitle>

    /**
     * Extracts videos & subtitles from the `embed`
     *
     * returns a container containing both videos & subtitles (optional)
     * **/
    abstract suspend fun extract(): VideoContainer

    /**
     * Loads videos & subtitles from a given Url
     *
     * & returns itself with the data loaded
     * **/
    open suspend fun load(): VideoExtractor {
        extract().also {
            videos = it.videos
            subtitles = it.subtitles
            return this
        }
    }

    /**
     * Gets called when a Video from this extractor starts playing
     *
     * Useful for Extractor that require Polling
     * **/
    open fun onVideoPlayed(video: Video?) {}

    /**
     * Called when a particular video has been stopped playing
     **/
    open fun onVideoStopped(video: Video?) {}
}

/**
 * A simple class containing name & link of the embed which shows the video present on the site
 *
 * `name` variable is used when checking if there was a Default Server Selected with the same name
 * **/
data class VideoServer(
    val name: String,
    val embed: FileUrl,
) : Serializable {
    constructor(name: String, embedUrl: String) : this(name, FileUrl(embedUrl))
}

/**
 * A Container for keeping video & subtitles, so you dont need to check backend
 * **/
data class VideoContainer(
    val videos: List<Video>,
    val subtitles: List<Subtitle> = listOf()
)

/**
 * The Class which contains all the information about a Video
 * **/
data class Video(
    /**
     * Will represent quality to user in form of `"${quality}p"` (1080p)
     *
     * If quality is null, shows "Unknown Quality"
     *
     * If isM3U8 is true, shows "Multi Quality"
     * **/
    val quality: Int?,

    /**
     * If the video is an M3U8 file, set this variable to true,
     *
     * This makes the app show it as a "Multi Quality" Link
     * **/
    val isM3U8: Boolean,

    /**
     * The direct url to the Video
     *
     * Supports mp4,mkv & m3u8 for now, afaik
     * **/
    val url: FileUrl,

    /**
     * use getSize(url) to get this size,
     *
     * no need to set it on M3U8 links
     * **/
    val size: Double? = null,

    /**
     * In case, you want to show some extra notes to the User
     *
     * Ex: "Backup" which could be used if the site provides some
     * **/
    val extraNote: String? = null,
) : Serializable {

    constructor(quality: Int? = null, isM3U8: Boolean, url: String, size: Double?, extraNote: String? = null)
            : this(quality, isM3U8, FileUrl(url), size, extraNote)

    constructor(quality: Int? = null, isM3U8: Boolean, url: String, size: Double?)
            : this(quality, isM3U8, FileUrl(url), size)

    constructor(quality: Int? = null, isM3U8: Boolean, url: String)
            : this(quality, isM3U8, FileUrl(url))
}

/**
 * The Class which contains the link to a subtitle file of a specific language
 * **/
data class Subtitle(
    /**
     * Language of the Subtitle
     *
     * for now app will directly try to select "English".
     * Probably in rework we can add more subtitles support
     * **/
    val language: String,

    /**
     * The direct url to the Subtitle
     *
     * Supports vtt, afaik
     * **/
    val url: FileUrl,
) : Serializable {
    constructor(language: String, url: String) : this(language, FileUrl(url))
}