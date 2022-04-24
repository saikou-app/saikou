package ani.saikou.anime.source.parsers


import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.anime.source.extractors.VizCloud
import ani.saikou.media.Media
import ani.saikou.media.Source
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jsoup.Jsoup

class NineAnime(private val dub: Boolean = false, override val name: String = "9Anime.me") : AnimeParser() {

    override fun getStream(episode: Episode, server: String): Episode {
        val streams = mutableMapOf<String, Episode.StreamLinks?>()
        try {
            val body = httpClient.get(episode.link!!).parsed<Response>().html
            val document = Jsoup.parse(body)
            val rawJson = document.select(".episodes li a").select(".active").attr("data-sources")
            val dataSources = Json.decodeFromString<JsonObject>(rawJson)
            document.select(".tabs span").forEach {
                val name = it.text()
                if (name == server) {
                    val encodedStreamUrl = getEpisodeLinks(dataSources[it.attr("data-id")].toString()).url
                    val realLink = getLink(encodedStreamUrl)
                    when (server) {
                        "Vidstream" -> streams[server] = (VizCloud("${host()}/").getStreamLinks(name, realLink))
                        "MyCloud"   -> streams[server] = (VizCloud("${host()}/").getStreamLinks(name, realLink))
                    }
                    episode.streamLinks = streams
                    return episode
                }
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        episode.streamLinks = streams
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        val streams = mutableMapOf<String, Episode.StreamLinks?>()
//        try {
            val body = httpClient.get(episode.link!!).parsed<Response>().html
            val document = Jsoup.parse(body)
            val rawJson = document.select(".episodes li a").select(".active").attr("data-sources")
            val dataSources = Json.decodeFromString<JsonObject>(rawJson)
            document.select(".tabs span").forEach {
                val name = it.text()
                val encodedStreamUrl = getEpisodeLinks(dataSources[it.attr("data-id")].toString()).url
                val realLink = getLink(encodedStreamUrl)
                when (name) {
                    "Vidstream" -> streams[name] = (VizCloud("${host()}/").getStreamLinks(name, realLink))
                    "MyCloud"   -> streams[name] = (VizCloud("${host()}/").getStreamLinks(name, realLink))
                }

            }
//        } catch (e: Exception) {
//            toastString(e.toString())
//        }
        episode.streamLinks = streams
        return episode
    }


    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        try {
            var slug: Source? = loadData("9anime${if (dub) "dub" else ""}_${media.id}")
            if (slug == null) {
                var it = media.nameMAL ?: media.name
                setTextListener("Searching for $it")
                logger("9anime : Searching for $it")
                var search = search(it)
                if (search.isNotEmpty()) {
                    slug = search[0]
                    saveSource(slug, media.id, false)
                } else {
                    it = media.nameRomaji
                    search = search(it)
                    setTextListener("Searching for $it")
                    logger("9anime : Searching for $it")
                    if (search.isNotEmpty()) {
                        slug = search[0]
                        saveSource(slug, media.id, false)
                    }
                }
            } else {
                setTextListener("Selected : ${slug.name}")
            }
            if (slug != null) return getSlugEpisodes(slug.link)
        } catch (e: Exception) {
            toastString("$e")
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        val responseArray = arrayListOf<Source>()
        val vrf = getVrf(string)
        //        try {
        httpClient.get(
            "${host()}/filter?language%5B%5D=${
                if (dub) "dubbed" else "subbed"
            }&keyword=${encode(string)}&vrf=${encode(vrf)}&page=1"
        ).document.select("ul.anime-list li").forEach {

            val link = it.select("a.name").attr("href")
            val title = it.select("a.name").text()
            val cover = it.select("a.poster img").attr("src")
            responseArray.add(Source(link, title, cover))
        }
        //        } catch (e: Exception) {
        //            toastString(e.toString())
        //        }
        return responseArray
    }

    data class Response(val html: String)

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        val animeId = slug.substringAfterLast(".")
        val vrf = encode(getVrf(animeId))
        try {
            val body = httpClient.get("${host()}/ajax/anime/servers?id=$animeId&vrf=$vrf").parsed<Response>()
            val html = Jsoup.parse(body.html)
            val replacedHtml = fixEncoding(html.toString())
            Jsoup.parse(replacedHtml).body().select("ul.episodes li a").forEach {
                val num = it.attr("data-base")
                responseArray[num] =
                    Episode(number = num, link = "${host()}/ajax/anime/servers?id=$animeId&vrf=$vrf&episode=$num")
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("9anime${if (dub) "dub" else ""}_$id", source)
    }

    data class Links(val url: String)

    private fun getEpisodeLinks(source: String): Links {
        return httpClient.get("${host()}/ajax/anime/episode?id=${source.replace("\"", "")}").parsed()
    }

    companion object {
        private var host: String? = null
        fun host(): String {
            val liveHost =
                if (host != null)
                    host ?: "9anime.pl"
                else
                    httpClient.get("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/nine.txt")
                        .text.replace("\n", "")
            return "https://$liveHost"
        }
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

    private fun fixEncoding(encodedHtml: String): String {
        return encodedHtml
            .replace("\\&quot;", "\"")
            .replace("\"\"", "\"")
            .replace("\n", "")
            .replace(" \" ", " ")
            .replace("\\n", "")
            .replace("\"\"", "\"")
            .replace("\\\"", "\"")
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

    private fun encode(input: String): String = java.net.URLEncoder.encode(input, "utf-8").replace("+", "%20")

    private fun decode(input: String): String = java.net.URLDecoder.decode(input, "utf-8")
}