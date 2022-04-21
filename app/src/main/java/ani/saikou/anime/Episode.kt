package ani.saikou.anime

import java.io.Serializable

data class Episode(
    val number: String,
    var title: String? = null,
    var desc: String? = null,
    var thumb: String? = null,
    var filler: Boolean = false,
    val saveStreams: Boolean = true,
    var link: String? = null,
    var selectedStream: String? = null,
    var selectedQuality: Int = 0,
    var videoServers: Map<String, VideoServer> = mapOf(),
    var allStreams: Boolean = false,
    var watched: Long? = null,
    var maxLength: Long? = null,
) : Serializable {
    data class VideoQuality(
        val videoUrl: String,
        val quality: String,
        val size: Long?,
        val note: String? = null,
    ) : Serializable

    data class Subtitle(
        val language: String,
        val vttUrl: String,
    )

    data class VideoServer(
        val serverName: String,
        val videoQuality: List<VideoQuality>,
        val headers: MutableMap<String, String> = mutableMapOf(),
        val availableSubtitles: List<Subtitle> = emptyList()
    ) : Serializable
}


