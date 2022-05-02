package ani.saikou.parsers.anime.extractors

import android.net.Uri
import android.util.Base64
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.findBetween
import ani.saikou.okHttpClient
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.RapidCloud.SocketHandler.webSocket
import okhttp3.*
import java.util.concurrent.*

class RapidCloud(override val server: VideoServer) : VideoExtractor() {

    override suspend fun extract(): VideoContainer {
        val videos = mutableListOf<Video>()
        val subtitles = mutableListOf<Subtitle>()

        val embed = server.embed

        val soup = client.get(embed.url, embed.headers).text.replace("\n", "")

        val key = soup.findBetween("var recaptchaSiteKey = '", "',")
        val number = soup.findBetween("recaptchaNumber = '", "';")

        val sId = wss(okHttpClient)

        if (key != null && number != null && sId != null) {
            captcha(embed.url, key)?.apply {

                val jsonLink = "https://rapid-cloud.ru/ajax/embed-6/getSources?id=${
                    embed.url.findBetween("/embed-6/", "?z=")!!
                }&_token=${this}&_number=$number&sId=$sId"

                val json = client.get(jsonLink).parsed<SourceResponse>()

                json.sources?.forEach {
                    videos.add(Video(0,true, FileUrl(it.file?:return@forEach)))
                }
                json.sourcesBackup?.forEach {
                    videos.add(Video(0,true, FileUrl(it.file?:return@forEach), extraNote = "Backup"))
                }
                json.tracks?.forEach {
                    if(it.kind=="captions" && it.label!=null && it.file!=null)
                        subtitles.add(Subtitle(it.label, it.file))
                }
            }
        }

        return VideoContainer(videos,subtitles)
    }

    private suspend fun captcha(url: String, key: String): String? {
        val uri = Uri.parse(url)
        val domain = (Base64.encodeToString(
            (uri.scheme + "://" + uri.host + ":443").encodeToByteArray(),
            Base64.NO_PADDING
        ) + ".").replace("\n", "")
        val vToken =
            client.get("https://www.google.com/recaptcha/api.js?render=$key", referer = (uri.scheme + "://" + uri.host))
                .text.replace("\n", "")
                .findBetween("/releases/", "/recaptcha") ?: return null
        val recapToken =
            client.get("https://www.google.com/recaptcha/api2/anchor?ar=1&hl=en&size=invisible&cb=kr60249sk&k=$key&co=$domain&v=$vToken")
                .document.selectFirst("#recaptcha-token")?.attr("value") ?: return null
        return client.post(
            "https://www.google.com/recaptcha/api2/reload?k=$key",
            data = mutableMapOf("v" to vToken, "k" to key, "c" to recapToken, "co" to domain, "sa" to "", "reason" to "q")
        )
            .text.replace("\n", "").findBetween("rresp\",\"", "\",null")
    }

    private fun wss(client: OkHttpClient): String? {
        val latch = CountDownLatch(1)
        var sId: String? = null
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("40")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                when {
                    text.startsWith("40") -> {
                        sId = text.findBetween("40{\"sid\":\"", "\"}")
                        latch.countDown()
                    }
                    text == "2"           -> webSocket.send("3")
                }
                println("web : $text")
            }
        }
        webSocket = client.newWebSocket(
            Request.Builder().url("wss://ws1.rapid-cloud.ru/socket.io/?EIO=4&transport=websocket").build(),
            listener
        )
        latch.await(30, TimeUnit.SECONDS)
        return sId
    }

    override fun onVideoStopped(video: Video?) {
        webSocket?.close(4969,"Just got Saikou-ed")
    }

    private data class SourceResponse (
        val sources: List<Source>? = null,
        val sourcesBackup: List<Source>? = null,
        val tracks: List<Source>? = null
    ) {
        data class Source(
            val file: String?=null,
            val label: String? = null,
            val kind:  String? = null
        )
    }
    object SocketHandler {
        var webSocket: WebSocket? = null
    }
}