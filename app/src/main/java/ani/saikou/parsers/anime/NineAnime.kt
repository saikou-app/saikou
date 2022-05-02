package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.StreamTape
import ani.saikou.parsers.anime.extractors.VizCloud
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.Requests
import org.jsoup.Jsoup
import java.net.URLDecoder

class NineAnime : AnimeParser() {

    override val name = "9anime"
    override val saveName = "9anime_to"
    override val hostUrl = "https://$defaultHost"
    override val malSyncBackupName = "9anime"
    override val isDubAvailableSeparately = true

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
    }

    override suspend fun loadEpisodes(animeLink: String): List<Episode> {
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

    override suspend fun loadVideoServers(episodeLink: String): List<VideoServer> {
        val body = client.get(episodeLink).parsed<Response>().html
        val document = Jsoup.parse(body)
        val rawJson = document.select(".episodes li a").select(".active").attr("data-sources")
        val dataSources = Requests.mapper.readValue<Map<String, String>>(rawJson)

        return document.select(".tabs span").map {
            val name = it.text()
            val encodedStreamUrl = getEpisodeLinks(dataSources[it.attr("data-id")].toString()).url
            val realLink = FileUrl(getLink(encodedStreamUrl), embedHeaders)
            VideoServer(name, realLink)
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val extractor: VideoExtractor? = when (server.name) {
            "Vidstream"  -> VizCloud(server)
            "MyCloud"    -> VizCloud(server)
            "Streamtape" -> StreamTape(server)
            else         -> null
        }
        return extractor
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val vrf = getVrf(query)
        val searchLink = "${host()}/filter?language%5B%5D=${if (selectDub) "dubbed" else "subbed"}&keyword=${encode(query)}&vrf=${encode(vrf)}&page=1"
        return client.get(searchLink).document.select("ul.anime-list li").map {
            val link = it.select("a.name").attr("href")
            val title = it.select("a.name").text()
            val cover = it.select("a.poster img").attr("src")
            ShowResponse(title, link, cover)
        }
    }

    private data class Links(val url: String)
    data class Response(val html: String)

    private suspend fun getEpisodeLinks(source: String): Links {
        return client.get("${host()}/ajax/anime/episode?id=${source.replace("\"", "")}").parsed()
    }

    //The code below is fully taken from
    //https://github.com/jmir1/aniyomi-extensions/blob/master/src/en/nineanime/src/eu/kanade/tachiyomi/animeextension/en/nineanime/NineAnime.kt

    private val key = "0wMrYU+ixjJ4QdzgfN2HlyIVAt3sBOZnCT9Lm7uFDovkb/EaKpRWhqXS5168ePcG"

    private fun getVrf(id: String): String {
        val reversed = ue(encode(id) + "0000000").slice(0..5).reversed()
        return reversed + ue(je(reversed, encode(id))).replace("""=+$""".toRegex(), "")
    }

    private fun getLink(url: String): String {
        val i = url.slice(0..5)
        val n = url.slice(6..url.lastIndex)
        return decode(je(i, ze(n)))
    }


    private fun ue(input: String): String {
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

    private fun je(inputOne: String, inputTwo: String): String {
        val arr = IntArray(256) { it }
        var output = ""
        var u = 0
        var r: Int
        for (a in arr.indices) {
            u = (u + arr[a] + inputOne[a % inputOne.length].code) % 256
            r = arr[a]
            arr[a] = arr[u]
            arr[u] = r
        }
        u = 0
        var c = 0
        for (f in inputTwo.indices) {
            c = (c + f) % 256
            u = (u + arr[c]) % 256
            r = arr[c]
            arr[c] = arr[u]
            arr[u] = r
            output += (inputTwo[f].code xor arr[(arr[c] + arr[u]) % 256]).toChar()
        }
        return output
    }

    private fun ze(input: String): String {
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

    private fun decode(input: String): String = URLDecoder.decode(input, "utf-8")
}