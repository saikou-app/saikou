package ani.saikou.android.anime.source.extractors

import android.net.Uri
import android.util.Base64
import ani.saikou.android.anime.source.Extractor
import ani.saikou.core.model.anime.Episode
import ani.saikou.core.utils.extension.findBetween
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.*
import org.jsoup.Jsoup
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RapidCloud : Extractor() {

    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val qualities = arrayListOf<Episode.Quality>()
        val subtitle = mutableMapOf<String, String>()

        val client = OkHttpClient()

        val soup =
            Jsoup.connect(url).referrer("https://zoro.to/").get().toString().replace("\n", "")
        val key = soup.findBetween("var recaptchaSiteKey = '", "',")
        val number = soup.findBetween("recaptchaNumber = '", "';")
        val sId = sId(client)
        if (key != null && number != null && sId != null) {
            captcha(url, key)?.apply {
                val jsonLink = "https://rapid-cloud.ru/ajax/embed-6/getSources?id=${
                    url.findBetween(
                        "/embed-6/",
                        "?z="
                    )!!
                }&_token=${this}&_number=$number&sId=$sId"
                val json = Json.decodeFromString<JsonObject>(
                    client.newCall(
                        Request.Builder().url(jsonLink).build()
                    ).execute().body!!.string()
                )
                val m3u8 = json["sources"]!!.jsonArray[0].jsonObject["file"].toString().trim('"')

                json["tracks"]!!.jsonArray.forEach {
                    if (it.jsonObject["kind"].toString().trim('"') == "captions")
                        subtitle[it.jsonObject["label"].toString().trim('"')] =
                            it.jsonObject["file"].toString().trim('"')
                }
                qualities.add(Episode.Quality(m3u8, "Multi Quality", null))
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

    private fun captcha(url: String, key: String): String? {
        val uri = Uri.parse(url)
        val domain = (Base64.encodeToString(
            (uri.scheme + "://" + uri.host + ":443").encodeToByteArray(),
            Base64.NO_PADDING
        ) + ".").replace("\n", "")
        val vToken = Jsoup.connect("https://www.google.com/recaptcha/api.js?render=$key")
            .referrer(uri.scheme + "://" + uri.host).get().toString().replace("\n", "")
            .findBetween("/releases/", "/recaptcha") ?: return null
        val recapToken =
            Jsoup.connect("https://www.google.com/recaptcha/api2/anchor?ar=1&hl=en&size=invisible&cb=kr60249sk&k=$key&co=$domain&v=$vToken")
                .get().selectFirst("#recaptcha-token")?.attr("value") ?: return null
        return Jsoup.connect("https://www.google.com/recaptcha/api2/reload?k=$key")
            .ignoreContentType(true)
            .data(
                mutableMapOf(
                    "v" to vToken,
                    "k" to key,
                    "c" to recapToken,
                    "co" to domain,
                    "sa" to "",
                    "reason" to "q"
                )
            )
            .post().toString().replace("\n", "").findBetween("rresp\",\"", "\",null")
    }

    //jmir the lord & saviour
    private fun sId(client: OkHttpClient): String? {
        val latch = CountDownLatch(1)
        var sId: String? = null
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("40")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.startsWith("40")) {
                    sId = text
                    webSocket.close(1000, null)
                    latch.countDown()
                }
            }
        }
        client.newWebSocket(
            Request.Builder().url("wss://ws1.rapid-cloud.ru/socket.io/?EIO=4&transport=websocket")
                .build(), listener
        )
        latch.await(30, TimeUnit.SECONDS)
        return sId?.substringAfter("40{\"sid\":\"", "")
            ?.substringBefore("\"", "")
    }
}
