package ani.saikou.parsers.anime

import ani.saikou.client
import ani.saikou.getSize
import ani.saikou.parsers.Video
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer

class Haho : Tenshi() {
    override val name: String = "H Aho"
    override val saveName: String = "haho_moe"
    override val hostUrl: String = "https://haho.moe"

    override val isNSFW: Boolean = true

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor {
        return object : VideoExtractor(){

            override val server = server

            val url = server.embed.url
            val headers = server.embed.headers

            override suspend fun extract(): VideoContainer {
                val list = mutableListOf<Video>()
                client.get(url, headers).document.select("video#player>source").forEach {
                    val uri = it.attr("src")
                    if (uri.isNotEmpty())
                        list.add(Video(it.attr("title").replace("p","").toIntOrNull(),false,uri, getSize(uri)))
                }
                return VideoContainer(list)
            }

        }
    }
}