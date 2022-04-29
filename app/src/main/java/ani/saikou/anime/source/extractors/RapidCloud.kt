package ani.saikou.anime.source.extractors

import android.net.Uri
import android.util.Base64
import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.findBetween
import ani.saikou.httpClient
import ani.saikou.okHttpClient
import okhttp3.*
import java.util.concurrent.*

class RapidCloud : Extractor() {

    override suspend fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val qualities = arrayListOf<Episode.Quality>()
        val subtitle = mutableMapOf<String, String>()

        val soup = httpClient.get(url, referer = "https://zoro.to/").text.replace("\n", "")
        val key = soup.findBetween("var recaptchaSiteKey = '", "',")
        val number = soup.findBetween("recaptchaNumber = '", "';")

        val sId = wss(okHttpClient)
        if (key != null && number != null && sId != null) {
            captcha(url, key)?.apply {

                val jsonLink = "https://rapid-cloud.ru/ajax/embed-6/getSources?id=${
                    url.findBetween("/embed-6/", "?z=")!!
                }&_token=${this}&_number=$number&sId=$sId"

                val json = httpClient.get(jsonLink).parsed<SourceResponse>()

                json.sources?.forEach {
                    qualities.add(Episode.Quality(it.file?:return@forEach, "Multi Quality", null))
                }
                json.sourcesBackup?.forEach {
                    qualities.add(Episode.Quality(it.file?:return@forEach, "Multi Quality", null,"Backup"))
                }
                json.tracks?.forEach {
                    if(it.kind=="captions" && it.label!=null && it.file!=null)
                        subtitle[it.label] = it.file
                }
            }
        }

        return Episode.StreamLinks(
            name,
            qualities,
            mutableMapOf(
                "SID" to (sId ?: ""),
                "origin" to "https://rapid-cloud.ru",
                "referer" to "https://zoro.to/"
            ),
            subtitle
        )
    }

    data class SourceResponse (
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

    private suspend fun captcha(url: String, key: String): String? {
        val uri = Uri.parse(url)
        val domain = (Base64.encodeToString(
            (uri.scheme + "://" + uri.host + ":443").encodeToByteArray(),
            Base64.NO_PADDING
        ) + ".").replace("\n", "")
        val vToken =
            httpClient.get("https://www.google.com/recaptcha/api.js?render=$key", referer = (uri.scheme + "://" + uri.host))
                .text.replace("\n", "")
                .findBetween("/releases/", "/recaptcha") ?: return null
        val recapToken =
            httpClient.get("https://www.google.com/recaptcha/api2/anchor?ar=1&hl=en&size=invisible&cb=kr60249sk&k=$key&co=$domain&v=$vToken")
                .document.selectFirst("#recaptcha-token")?.attr("value") ?: return null
        return httpClient.post(
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
            }
        }
        client.newWebSocket(
            Request.Builder().url("wss://ws1.rapid-cloud.ru/socket.io/?EIO=4&transport=websocket").build(),
            listener
        )
        latch.await(30, TimeUnit.SECONDS)
        return sId
    }
}
