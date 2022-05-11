package ani.saikou.anime

import ani.saikou.FileUrl
import ani.saikou.parsers.VideoExtractor
import java.io.Serializable

data class Episode(
    val number: String,
    var link: String? = null,
    var title: String? = null,
    var desc: String? = null,
    var thumb: FileUrl? = null,
    var filler: Boolean = false,
    var selectedServer: String? = null,
    var selectedVideo: Int = 0,
    var extractors: MutableList<VideoExtractor>?=null,
    @Transient var extractorCallback: ((VideoExtractor) -> Unit)?=null,
    var allStreams: Boolean = false,
    var watched: Long? = null,
    var maxLength: Long? = null,
    val extra: Any?=null,

) : Serializable


