package ani.saikou.parsers

/**
 * Used to extract videos from a specific video host,
 * A new instance is created for every embeds/iframes of that Episode
 * **/
abstract class VideoExtractor {
    lateinit var name: String
    lateinit var url: String
    lateinit var videos: List<Video>
    lateinit var subtitles: List<Subtitle>

    /**
     * Loads "Video/s" from a given Url
     * & returns itself with the data loaded
     * **/
    abstract suspend fun extractFromUrl(url: String): VideoExtractor

    /**
     * Gets called when a Video from this extractor starts playing.
     * Useful for Extractor that require Polling
     * **/
    open suspend fun onVideoPlayed(video: Video) {}

    /**
     * Called when a particular video has been stopped playing
     **/
    open suspend fun onVideoStopped(video: Video) {}
}

data class Video(
    /**
     * Will represent quality to user in form of "${quality}p" (1080p)
     * If quality is 0, will show "Default Quality"
     * If isM3U8 is true, shows "Multi Quality"
     * **/
    val quality: Int = 0,

    /**
     * If the video is an M3U8 file, set this variable to true,
     * This makes the app show it as a "Multi Quality" Link
     * **/
    val isM3U8: Boolean,

    /**
     * The direct url to the Video
     * Supports mp4,mkv & m3u8 for now, afaik
     * **/
    val url: String,
    val headers: MutableMap<String,String>,

    /**
     * use getSize(url) to get this size,
     * no need to set it on M3U8 links
     * **/
    val size: Long? = null,

    /**
     * In case, you want to show some extra notes to the User
     * Ex: "Backup" which could be used if the site provides some
     * **/
    val extraNote: String? = null,
)

data class Subtitle(
    /**
     * Language of the Subtitle
     * for now app will directly try to select "English".
     * Probably in rework we can add more subtitles support
     * **/
    val language: String,

    /**
     * The direct url to the Subtitle
     * Supports vtt, afaik
     * **/
    val url: String,
    val headers: MutableMap<String,String>,
)