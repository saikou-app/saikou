package ani.saikou.anime.source.parsers


import android.os.Build
import androidx.annotation.RequiresApi
import ani.saikou.anilist.httpClient
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.anime.source.extractors.VizCloud
import ani.saikou.loadData
import ani.saikou.logger
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.toastString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.*
import kotlin.collections.ArrayList

class NineAnime(private val dub: Boolean = false, override val name: String = "9Anime Scraper - 9Anime.to") : AnimeParser() {
    private val key = "0wMrYU+ixjJ4QdzgfN2HlyIVAt3sBOZnCT9Lm7uFDovkb/EaKpRWhqXS5168ePcG"
    private val host = listOf(
        "https://9anime.to",
        "https://9anime.id",
        "https://9anime.pl",
    )

//    Special thanks to the contributors of Aniyomi: https://github.com/jmir1 and https://github.com/silverwolf-waltz for most of the decoding code of 9anime!
    override fun getStream(episode: Episode, server: String): Episode {
        val streams = mutableMapOf<String, Episode.StreamLinks?>()
        val body = httpClient.newCall(
            Request.Builder().url(episode.link!!).build()
        ).execute().body!!.string()
        val bodyJson = Json.decodeFromString<JsonObject>(body)
        val html = Jsoup.parse(bodyJson["html"].toString())
        val replacedHtml = (html.toString().replace("\\n", "")).replace("\\&quot;", "")
        val dataSources =
            makeJsonJsonAgain(Jsoup.parse(replacedHtml).body().select(".episodes li a").select(".active").attr("data-sources"))
        var realLink = ""
        Jsoup.parse(replacedHtml).body().select(".tabs span").forEach {
            val name = it.text()
            if (name == server){
                val encodedStreamBody = shitCallThatFailsOften(dataSources[it.attr("data-id")].toString())
                    ?: shitCallThatFailsOften(dataSources[it.attr("data-id")].toString())!!
                val encodedStreamUrl = Json.decodeFromString<JsonObject>(encodedStreamBody)["url"]!!.jsonPrimitive.content
                realLink = getLink(encodedStreamUrl)
            }
        }
        when (server) {
            "Vidstream" -> streams[server] = (VizCloud("${host[0]}/").getStreamLinks(name, realLink))
            "MyCloud"   -> streams[server] = (VizCloud("${host[0]}/").getStreamLinks(name, realLink))
        }
        episode.streamLinks = streams
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        val streams = mutableMapOf<String, Episode.StreamLinks?>()
        try {
            val body = httpClient.newCall(
                Request.Builder().url(episode.link!!).build()
            ).execute().body!!.string()
            val bodyJson = Json.decodeFromString<JsonObject>(body)
            val html = Jsoup.parse(bodyJson["html"].toString())
            val replacedHtml = (html.toString().replace("\\n", "")).replace("\\&quot;", "")
            val dataSources = makeJsonJsonAgain(
                Jsoup.parse(replacedHtml).body().select(".episodes li a").select(".active").attr("data-sources")
            )
            Jsoup.parse(replacedHtml).body().select(".tabs span").forEach {
                val name = it.text()
                val encodedStreamBody = shitCallThatFailsOften(dataSources[it.attr("data-id")].toString())
                    ?: shitCallThatFailsOften(dataSources[it.attr("data-id")].toString())!!
                val encodedStreamUrl = Json.decodeFromString<JsonObject>(encodedStreamBody)["url"]!!.jsonPrimitive.content
                val realLink = getLink(encodedStreamUrl)
                when (name) {
                    "Vidstream" -> streams[name] = (VizCloud("${host[0]}/").getStreamLinks(name, realLink))
                    "MyCloud"   -> streams[name] = (VizCloud("${host[0]}/").getStreamLinks(name, realLink))
                }

            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        episode.streamLinks = streams
        return episode
    }

    private fun shitCallThatFailsOften(source: String): String? {
        val call = httpClient.newCall(
            Request.Builder().url("${host[0]}/ajax/anime/episode?id=${source.replace("\"", "")}").build()
        ).execute()
        return if (call.code == 200) call.body!!.string() else null
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        try {
            var slug: Source? = loadData("9anime${if (dub) "dub" else ""}_${media.id}")
            if (slug == null) {
                var it = media.nameMAL ?: media.name + if (dub) " (Dub)" else ""
                setTextListener("Searching for $it")
                logger("9anime : Searching for $it")
                var search = search(it)
                if (search.isNotEmpty()) {
                    slug = search[0]
                    saveSource(slug, media.id, false)
                } else {
                    it = media.nameRomaji + if (dub) " (Dub)" else ""
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
        try {
            Jsoup.connect("${host[0]}/search?keyword=${encode(string)}&vrf=${encode(vrf)}&page=1").get()
                .select("ul.anime-list li").forEach {
                    val link = it.select("a.name").attr("href")
                    val title = it.select("a.name").text()
                    val cover = it.select("a.poster img").attr("src")
                    responseArray.add(Source(link, title, cover))
                }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String, Episode>()
        val animeId = slug.substringAfterLast(".")
        val vrf = encode(getVrf(animeId))
        try {
            val body = httpClient.newCall(
                Request.Builder().url("${host[0]}/ajax/anime/servers?id=$animeId&vrf=$vrf").build()
            ).execute().body!!.string()
            val bodyJson = Json.decodeFromString<JsonObject>(body)
            val html = Jsoup.parse(bodyJson["html"].toString())
            val replacedHtml = shittyReplaceBecauseOfWeirdEncodingShit(html.toString().replace("\\&quot;", "\""))
            Jsoup.parse(replacedHtml).body().select("ul.episodes li a").forEach {
                val num = it.attr("data-base")
                responseArray[num] =
                    Episode(number = num, link = "${host[0]}/ajax/anime/servers?id=$animeId&vrf=$vrf&episode=$num")
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseArray
    }

    private fun getVrf(id: String): String {
        val reversed = ue(encode(id) + "0000000").slice(0..5).reversed()
        return reversed + ue(je(reversed, encode(id))).replace("""=+$""".toRegex(), "")
    }

    private fun getLink(url: String): String {
        val i = url.slice(0..5)
        val n = url.slice(6..url.lastIndex)
        return decode(je(i, ze(n)))
    }

    private fun shittyReplaceBecauseOfWeirdEncodingShit(encodedHtml: String): String {
        val html = encodedHtml
            .replace("\"\"", "\"")
            .replace("\n", "")
            .replace(" \" ", " ")
            .replace("\\n", "")
            .replace("\"\"", "\"")
            .replace("\\\"", "\"")
        return html
    }

    private fun makeJsonJsonAgain(notJson: String): JsonObject {
        val json = notJson.replace("{", "{\"")
            .replace("}", "\"}")
            .replace(",", "\",\"")
            .replace(":", "\":\"")

        return Json.decodeFromString<JsonObject>(json)
    }

    private fun ue(input: String): String {
        if (input.any { it.code >= 256 }) throw Exception("illegal characters!")
        var output = ""
        da(output)
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

    private fun da(a: String) {
        val e = "4oCU4oCU4oCU4oCU4oCU4oCU4oCU4oCU4oCU4oCU4oCUIE5vIHJld29yaz8g4oCU4oCU4oCU4oCU4oCU4oCU4oCU4oCU4oCU4oCU4oCUCuKggOKjnuKiveKiquKio+Kio+Kio+Kiq+KhuuKhteKjneKhruKjl+Kit+KiveKiveKiveKjruKht+KhveKjnOKjnOKiruKiuuKjnOKit+KiveKineKhveKjnQrioLjiobjioJzioJXioJXioIHiooHioofioo/ior3iorrio6riobPioZ3io47io4/ioq/iop7iob/io5/io7fio7Pioq/iobfio73ior3ioq/io7Pio6vioIcK4qCA4qCA4qKA4qKA4qKE4qKs4qKq4qGq4qGO4qOG4qGI4qCa4qCc4qCV4qCH4qCX4qCd4qKV4qKv4qKr4qOe4qOv4qO/4qO74qG94qOP4qKX4qOX4qCP4qCACuKggOKgquKhquKhquKjquKiquKiuuKiuOKiouKik+KihuKipOKigOKggOKggOKggOKggOKgiOKiiuKinuKhvuKjv+Khr+Kjj+KiruKgt+KggeKggOKggArioIDioIDioIDioIjioIrioIbioYPioJXiopXioofioofioofioofioofioo/ioo7ioo7ioobiooTioIDiopHio73io7/iop3ioLLioInioIDioIDioIDioIAK4qCA4qCA4qCA4qCA4qCA4qG/4qCC4qCg4qCA4qGH4qKH4qCV4qKI4qOA4qCA4qCB4qCh4qCj4qGj4qGr4qOC4qO/4qCv4qKq4qCw4qCC4qCA4qCA4qCA4qCACuKggOKggOKggOKggOKhpuKhmeKhguKigOKipOKio+Kgo+KhiOKjvuKhg+KgoOKghOKggOKhhOKiseKjjOKjtuKij+KiiuKgguKggOKggOKggOKggOKggOKggArioIDioIDioIDioIDiop3iobLio5zioa7ioY/ioo7iooziooLioJnioKLioJDiooDiopjiorXio73io7/iob/ioIHioIHioIDioIDioIDioIDioIDioIDioIAK4qCA4qCA4qCA4qCA4qCo4qO64qG64qGV4qGV4qGx4qGR4qGG4qGV4qGF4qGV4qGc4qG84qK94qG74qCP4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCACuKggOKggOKggOKggOKjvOKjs+Kjq+KjvuKjteKjl+KhteKhseKhoeKio+KikeKileKinOKileKhneKggOKggOKggOKggOKggOKggOKggOKggOKggOKggOKggArioIDioIDioIDio7Tio7/io77io7/io7/io7/iob/iob3ioZHioozioKrioaLioaPio6PioZ/ioIDioIDioIDioIDioIDioIDioIDioIDioIDioIDioIDioIAK4qCA4qCA4qCA4qGf4qG+4qO/4qK/4qK/4qK14qO94qO+4qO84qOY4qK44qK44qOe4qGf4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCA4qCACuKggOKggOKggOKggOKggeKgh+KgoeKgqeKhq+Kiv+KjneKhu+KhruKjkuKiveKgi+KggOKggOKggOKggOKggOKggOKggOKggOKggOKggOKggOKggOKggOKggArigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJTigJQ="
        val f = a+e
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            logger(String(Base64.getDecoder().decode(f)))
        }
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