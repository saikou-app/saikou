package ani.saikou.parsers.anime

import ani.saikou.*
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.StreamTape
import ani.saikou.parsers.anime.extractors.VidVard
import ani.saikou.parsers.anime.extractors.VizCloud
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jsoup.Jsoup
import java.net.URLDecoder.decode
import java.net.URLEncoder.encode

class NineAnime : AnimeParser() {

    override val name = "9anime"
    override val saveName = "9anime_to"
    override val hostUrl = "https://$defaultHost"
    override val malSyncBackupName = "9anime"
    override val isDubAvailableSeparately = true

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val animeId = animeLink.substringAfterLast(".")
        val vrf = encode(getVrf(animeId))
        val body = client.get("${host()}/ajax/anime/servers?id=$animeId&vrf=$vrf").parsed<Response>()
        return Jsoup.parse(body.html).body().select("ul.episodes li a").map {
            val num = it.attr("data-base")
            val text = it.text()
            Episode(text, "${host()}/ajax/anime/servers?id=$animeId&vrf=$vrf&episode=$num")
        }
    }

    private val embedHeaders = mapOf("referer" to "$hostUrl/")

    override suspend fun loadVideoServers(episodeLink: String, extra: Any?): List<VideoServer> {
        val body = client.get(episodeLink).parsed<Response>().html
        val document = Jsoup.parse(body)
        val rawJson = document.select(".episodes li a").select(".active").attr("data-sources")
        val dataSources = mapper.readValue<Map<String, String>>(rawJson)

        return document.select(".tabs span").mapNotNull {
            val name = it.text()
            val encodedStreamUrl = getEpisodeLinks(dataSources[it.attr("data-id")].toString())?.url ?: return@mapNotNull null
            val realLink = FileUrl(getLink(encodedStreamUrl), embedHeaders)
            VideoServer(name, realLink)
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val extractor: VideoExtractor? = when (server.name) {
            "Vidstream"  -> VizCloud(server)
            "MyCloud"    -> VizCloud(server)
            "VideoVard"  -> VidVard(server)
            "Streamtape" -> StreamTape(server)
            else         -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val vrf = getVrf(query)
        val searchLink =
            "${host()}/filter?language%5B%5D=${if (selectDub) "dubbed" else "subbed"}&keyword=${encode(query)}&vrf=${encode(vrf)}&page=1"
        return client.get(searchLink).document.select("ul.anime-list li").map {
            val link = it.select("a.name").attr("href")
            val title = it.select("a.name").text()
            val cover = it.select("a.poster img").attr("src")
            ShowResponse(title, link, cover)
        }
    }

    override suspend fun loadByVideoServers(episodeUrl: String, extra: Any?, callback: (VideoExtractor) -> Unit) {
        tryWithSuspend {
            val servers = loadVideoServers(episodeUrl, extra).map { getVideoExtractor(it) }
            val mutex = Mutex()
            servers.asyncMap {
                tryWithSuspend {
                    it?.apply {
                        if (this is VizCloud) mutex.withLock {
                            load()
                            callback.invoke(this)
                        } else {
                            load()
                            callback.invoke(this)
                        }
                    }
                }
            }
        }
    }

    private data class Links(val url: String?)
    data class Response(val html: String)

    private suspend fun getEpisodeLinks(source: String): Links? {
        return tryWithSuspend { client.get("${host()}/ajax/anime/episode?id=${source.replace("\"", "")}").parsed() }
    }


    companion object {
        private const val defaultHost = "9anime.pl"
        private var host: String? = null
        suspend fun host(): String {
            host =
                if (host != null) host ?: defaultHost
                else {
                    client.get("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/nine.txt")
                        .text.replace("\n", "")
                }
            return "https://$host"
        }

        //thanks to @Modder4869 for key
        private const val nineAnimeKey = "c/aUAorINHBLxWTy3uRiPt8J+vjsOheFG1E0q2X9CYwDZlnmd4Kb5M6gSVzfk7pQ"

        //The code below is taken from
        //https://github.com/jmir1/aniyomi-extensions/blob/master/src/en/nineanime/src/eu/kanade/tachiyomi/animeextension/en/nineanime/NineAnime.kt

        private fun getVrf(id: String): String {
            val reversed = encrypt(Companion.encode(id) + "0000000", nineAnimeKey).slice(0..5).reversed()
            return reversed + encrypt(cipher(reversed, Companion.encode(id)), nineAnimeKey).replace("""=+$""".toRegex(), "")
        }

        private fun getLink(url: String): String {
            val i = url.slice(0..5)
            val n = url.slice(6..url.lastIndex)
            return decode(cipher(i, decrypt(n, nineAnimeKey)))
        }

        fun encrypt(input: String,key:String): String {
            if (input.any { it.code >= 256 }) throw Exception("illegal characters!")
            var output = ""
            for (i in input.indices step 3) {
                val a = intArrayOf(-1, -1, -1, -1)
                a[0] = input[i].code shr 2
                a[1] = (3 and input[i].code) shl 4
                if (input.length > i + 1) {
                    a[1] = a[1] or (input[i + 1].code shr 4)
                    a[2] = (15 and input[i + 1].code) shl 2
                }
                if (input.length > i + 2) {
                    a[2] = a[2] or (input[i + 2].code shr 6)
                    a[3] = 63 and input[i + 2].code
                }
                for (n in a) {
                    if (n == -1) output += "="
                    else {
                        if (n in 0..63) output += key[n]
                    }
                }
            }
            return output
        }

        fun cipher(key: String, text: String): String {
            val arr = IntArray(256) { it }
            var output = ""
            var u = 0
            var r: Int
            for (a in arr.indices) {
                u = (u + arr[a] + key[a % key.length].code) % 256
                r = arr[a]
                arr[a] = arr[u]
                arr[u] = r
            }
            u = 0
            var c = 0
            for (f in text.indices) {
                c = (c + f) % 256
                u = (u + arr[c]) % 256
                r = arr[c]
                arr[c] = arr[u]
                arr[u] = r
                output += (text[f].code xor arr[(arr[c] + arr[u]) % 256]).toChar()
            }
            return output
        }

        @Suppress("SameParameterValue")
        private fun decrypt(input: String, key:String): String {
            val t = if (input.replace("""[\t\n\f\r]""".toRegex(), "").length % 4 == 0) {
                input.replace("""==?$""".toRegex(), "")
            } else input
            if (t.length % 4 == 1 || t.contains("""[^+/0-9A-Za-z]""".toRegex())) throw Exception("bad input")
            var i: Int
            var r = ""
            var e = 0
            var u = 0
            for (o in t.indices) {
                e = e shl 6
                i = key.indexOf(t[o])
                e = e or i
                u += 6
                if (24 == u) {
                    r += ((16711680 and e) shr 16).toChar()
                    r += ((65280 and e) shr 8).toChar()
                    r += (255 and e).toChar()
                    e = 0
                    u = 0
                }
            }
            return if (12 == u) {
                e = e shr 4
                r + e.toChar()
            } else {
                if (18 == u) {
                    e = e shr 2
                    r += ((65280 and e) shr 8).toChar()
                    r += (255 and e).toChar()
                }
                r
            }
        }

        private fun encode(input: String): String = encode(input, "utf-8").replace("+", "%20")

        private fun decode(input: String): String = decode(input, "utf-8")
    }

}